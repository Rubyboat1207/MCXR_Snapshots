package net.sorenon.mcxr.core.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.MCXRScale;
import net.sorenon.mcxr.core.Pose;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

import static net.minecraft.entity.player.PlayerEntity.STANDING_DIMENSIONS;

@Mixin(value = PlayerEntity.class, priority = 10_000 /*Pehuki*/)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerExt {

    @Shadow @Final private static Map<net.minecraft.entity.EntityPose, EntityDimensions> POSES;
    @Unique
    public boolean isXr = false;

    @Unique
    public Pose headPose = new Pose();

    @Unique
    public Pose leftHandPose = new Pose();

    @Unique
    public Pose rightHandPose = new Pose();

    @Unique
    public ThreadLocal<Arm> overrideTransform = ThreadLocal.withInitial(() -> null);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void preTick(CallbackInfo ci) {
        if (this.isXR()) {
            this.calculateDimensions();
        }
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    void overrideDims(net.minecraft.entity.EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        boolean dynamicHeight = MCXRCore.getCoreConfig().dynamicPlayerHeight();
        boolean thinnerBB = MCXRCore.getCoreConfig().thinnerPlayerBoundingBox();
        if (!dynamicHeight && !thinnerBB) {
            return;
        }

        EntityDimensions vanilla = POSES.getOrDefault(pose, STANDING_DIMENSIONS);

        if (this.isXR()) {
            final float scale = MCXRScale.getScale(this);

            float width = vanilla.width;
            if (thinnerBB) {
                width = 0.5f;
            }

            if (dynamicHeight) {
                final float minHeight = 0.5f * scale;
                final float currentHeight = this.getHeight();
                final float wantedHeight = (headPose.pos.y - (float) this.getPos().y + 0.125f * scale);
                final float deltaHeight = wantedHeight - currentHeight;

                if (deltaHeight <= 0) {
                    cir.setReturnValue(
                            EntityDimensions.changing(width * scale, Math.max(wantedHeight, minHeight))
                    );
                    return;
                }

                Box currentSize = this.getBoundingBox();
                List<VoxelShape> list = this.world.getEntityCollisions(this, currentSize.stretch(0, deltaHeight, 0));
                final double maxDeltaHeight = adjustMovementForCollisions(this, new Vec3d(0, deltaHeight, 0), currentSize, this.world, list).y;

                cir.setReturnValue(
                        EntityDimensions.changing(width * scale, Math.max(currentHeight + (float) maxDeltaHeight, minHeight))
                );
            } else {
                cir.setReturnValue(
                        EntityDimensions.changing(width * scale, vanilla.height)
                );
            }
        }
    }

    @Override
    public Pose getHeadPose() {
        return headPose;
    }

    @Override
    public Pose getLeftHandPose() {
        return leftHandPose;
    }

    @Override
    public Pose getRightHandPose() {
        return rightHandPose;
    }

    @Override
    public void setIsXr(boolean isXr) {
        this.isXr = isXr;
    }

    @Override
    public boolean isXR() {
        return isXr;
    }

    @Override
    public ThreadLocal<Arm> getOverrideTransform() {
        return this.overrideTransform;
    };
}
