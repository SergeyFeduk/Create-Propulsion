package com.deltasf.createpropulsion.compat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import org.valkyrienskies.mod.common.assembly.ShipAssembler;

public final class VS2AssemblyCompat {

    private VS2AssemblyCompat() {
    }

    public static CompletableFuture<Void> queueAssemble(ServerLevel level, Set<BlockPos> blocks, double scale) {
        Method queueMethod = findShipAssemblerMethod("queueAssembleToShipFull", ServerLevel.class, Set.class, double.class);
        if (queueMethod != null) {
            return futureFrom(invokeShipAssembler(queueMethod, level, blocks, scale));
        }

        Method syncFullMethod = findShipAssemblerMethod("assembleToShipFull", ServerLevel.class, Set.class, double.class);
        if (syncFullMethod != null) {
            return completedFromInvocation(syncFullMethod, level, blocks, scale);
        }

        Method syncMethod = findShipAssemblerMethod("assembleToShip", ServerLevel.class, Set.class, double.class);
        if (syncMethod != null) {
            return completedFromInvocation(syncMethod, level, blocks, scale);
        }

        Method legacyMethod = findShipAssemblerMethod(
            "assembleToShip",
            net.minecraft.world.level.Level.class,
            List.class,
            boolean.class,
            double.class,
            boolean.class
        );
        if (legacyMethod != null) {
            return completedFromInvocation(legacyMethod, level, List.copyOf(blocks), true, scale, false);
        }

        return failedFuture(new NoSuchMethodException("No compatible Valkyrien Skies assembly API was found"));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Void> requestChunks(ServerLevel level, Collection<ChunkPos> chunks) {
        Method requestChunksMethod = findShipAssemblerMethod("requestChunks", ServerLevel.class, Collection.class);
        if (requestChunksMethod != null) {
            Object result = invokeShipAssembler(requestChunksMethod, level, chunks);
            return result == null ? null : (CompletableFuture<Void>) result;
        }

        List<CompletableFuture<?>> futures = new ArrayList<>(chunks.size());
        Method getChunkFutureMethod = findMethod(level.getChunkSource().getClass(), "getChunkFutureMainThread",
            int.class, int.class, ChunkStatus.class, boolean.class);
        if (getChunkFutureMethod == null) {
            return chunks.stream().allMatch(chunk -> level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null)
                ? null
                : failedFuture(new NoSuchMethodException("Unable to request unloaded chunks without VS2 requestChunks()"));
        }

        try {
            getChunkFutureMethod.setAccessible(true);
            for (ChunkPos chunk : chunks) {
                if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null) {
                    continue;
                }
                futures.add((CompletableFuture<?>) getChunkFutureMethod.invoke(
                    level.getChunkSource(),
                    chunk.x,
                    chunk.z,
                    ChunkStatus.FULL,
                    true
                ));
            }
        } catch (Throwable error) {
            return failedFuture(unwrap(error));
        }

        return futures.isEmpty() ? null : CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static CompletableFuture<Void> completedFromInvocation(Method method, Object... args) {
        try {
            invokeShipAssembler(method, args);
            return CompletableFuture.completedFuture(null);
        } catch (Throwable error) {
            return failedFuture(unwrap(error));
        }
    }

    private static CompletableFuture<Void> futureFrom(Object result) {
        if (result instanceof CompletableFuture<?> future) {
            return future.thenAccept(unused -> {
            });
        }
        return failedFuture(new IllegalStateException("Unexpected assembly future type: " + result));
    }

    private static Object invokeShipAssembler(Method method, Object... args) {
        try {
            Object target = Modifier.isStatic(method.getModifiers()) ? null : ShipAssembler.class.getField("INSTANCE").get(null);
            return method.invoke(target, args);
        } catch (Throwable error) {
            throw new RuntimeException(unwrap(error));
        }
    }

    @Nullable
    private static Method findShipAssemblerMethod(String name, Class<?>... parameterTypes) {
        return findMethod(ShipAssembler.class, name, parameterTypes);
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = type.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignoredAgain) {
                return null;
            }
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error.getCause();
        return cause != null ? cause : error;
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }
}
