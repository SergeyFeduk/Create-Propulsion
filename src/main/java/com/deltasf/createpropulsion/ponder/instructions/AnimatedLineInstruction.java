package com.deltasf.createpropulsion.ponder.instructions;

import net.createmod.catnip.outliner.LineOutline;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.world.phys.Vec3;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public class AnimatedLineInstruction extends TickingInstruction {
    private final PonderPalette color;
    private final Vec3 start;
    private final Vec3 end;
    private final boolean big;
    private final int animationTicks;

    public AnimatedLineInstruction(PonderPalette color, Vec3 start, Vec3 end, int animationTicks, int ticksToPersist, boolean big) {
        super(false, animationTicks + ticksToPersist);
        this.color = color;
        this.start = start;
        this.end = end;
        this.big = big;
        this.animationTicks = animationTicks;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);

        float elapsed = (float) this.totalTicks - this.remainingTicks;
        float linearProgress = Math.min(1.0f, elapsed / (float) Math.max(1, this.animationTicks));
        float easedProgress = 1 - (1 - linearProgress) * (1 - linearProgress);
        float internalProgress = 1.0f - easedProgress;

        //Createslop fix
        if (!scene.getOutliner().getOutlines().containsKey(this)) {
            LineOutline.EndChasingLineOutline outline = new LineOutline.EndChasingLineOutline(false);
            outline.set(this.end, this.start); 
            outline.setProgress(1.0f);
            outline.setProgress(1.0f);
            
            scene.getOutliner().showOutline(this, outline);
        }

        scene.getOutliner().endChasingLine(this, this.end, this.start, internalProgress, false).lineWidth(this.big ? 0.125F : 0.0625F).colored(this.color.getColor());
    }
}
