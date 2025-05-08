package com.deltasf.createpropulsion.particles;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionCompatibility;
import com.simibubi.create.foundation.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

//Create actually handles registration so elegantly, this is the only reason I just copied it from their repo
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = Bus.MOD, value = Dist.CLIENT)
public enum ParticleTypes {
    //Plume is a special case as we handle shimmer compat
    PLUME_DEFAULT(PlumeParticleData::new),
    PLUME_SHIMMER(PlumeParticleData::new);
    private static volatile ParticleType<?> cachedPlumeType = null;
    private static final Object cacheLock = new Object();

    private final ParticleEntry<?> entry;

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        ParticleTypes.registerFactories(event);
    }

    <D extends ParticleOptions> ParticleTypes(Supplier<? extends ICustomParticleData<D>> typeFactory) {
        String name = Lang.asId(name());
        entry = new ParticleEntry<>(name, typeFactory);
    }

    public static ParticleType<?> getPlumeType() {
        ParticleType<?> result = cachedPlumeType;
        //Thread-safe caching or whatever
        if (result == null) {
            synchronized (cacheLock) {
                result = cachedPlumeType;
                if (result == null) {
                    result = PropulsionCompatibility.SHIMMER_ACTIVE ? PLUME_SHIMMER.get() : PLUME_DEFAULT.get();
                    cachedPlumeType = result;
                }
            }
        }
        return result;
    }

    public static void register(IEventBus modEventBus){
        ParticleEntry.REGISTER.register(modEventBus);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerFactories(RegisterParticleProvidersEvent event) {
        for (ParticleTypes particle : values()) 
            particle.entry.registerFactory(event);
    }

    public ParticleType<?> get() {
        return entry.object.get();
    }

    public String parameter() {
        return entry.name;
    }

    private static class ParticleEntry<D extends ParticleOptions> {
        private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CreatePropulsion.ID);
        private final String name;
        private final Supplier<? extends ICustomParticleData<D>> typeFactory;
        private final RegistryObject<ParticleType<D>> object;

        public ParticleEntry(String name, Supplier<? extends ICustomParticleData<D>> typeFactory) {
            this.name = name; this.typeFactory = typeFactory;
            object = REGISTER.register(name, () -> this.typeFactory.get().createType());
        }

        @OnlyIn(Dist.CLIENT)
        public void registerFactory(RegisterParticleProvidersEvent event){
            typeFactory.get().register(object.get(), event);
        }
    }
}
