package net.sorenon.mcxr.play.mixin.hands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.PlayOptions;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    //TODO figure out if this should just be the direction of the player's head

    @Shadow
    public abstract boolean isAlive();

    public LivingEntityMixin(EntityType<?> entityType, World level) {
        super(entityType, level);
    }

    @Unique
    private boolean isActive() {
        //noinspection ConstantConditions
        return MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode() && (LivingEntity) (Object) this instanceof ClientPlayerEntity;
    }

    @Redirect(method = "handleRelativeFrictionAndCalculateMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    public void moveRelativeLand(LivingEntity instance, float speed, Vec3d move) {
        if (isActive()) {
            Optional<Float> val = PlayOptions.walkDirection.getMCYaw();
            if (val.isPresent()) {
                Vec3d inputVector = movementInputToVelocity(move, speed, val.get());
                this.setVelocity(this.getVelocity().add(inputVector));
                return;
            }
        }
        this.updateVelocity(speed, move);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    public void moveRelativeLiquid(LivingEntity instance, float speed, Vec3d move) {
        if (isActive() && this.isSwimming()) {
            Optional<Float> val = PlayOptions.swimDirection.getMCYaw();
            if (val.isPresent()) {
                Vec3d inputVector = movementInputToVelocity(move, speed, val.get());
                this.setVelocity(this.getVelocity().add(inputVector));
            } else {
                this.updateVelocity(speed, move);
            }
        } else {
            this.moveRelativeLand(instance, speed, move);
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    public Vec3d getLookAngleFlying(LivingEntity instance) {
        if (isActive()) {
            Vec3d result = PlayOptions.flyDirection.getLookDirection();
            if (result != null) {
                return result;
            }
        }
        return this.getRotationVector(this.getPitch(), this.getYaw());
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    public float getXRotFlying(LivingEntity instance) {
        if (isActive()) {
            Optional<Float> val = PlayOptions.flyDirection.getMCPitch();
            if (val.isPresent()) {
                return val.get();
            }
        }
        return this.getPitch();
    }

    @Unique
    private static Vec3d movementInputToVelocity(Vec3d vec3, float f, float g) {
        double d = vec3.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply((double) f);
            float h = MathHelper.sin(g * (float) (Math.PI / 180.0));
            float i = MathHelper.cos(g * (float) (Math.PI / 180.0));
            return new Vec3d(vec32.x * (double) i - vec32.z * (double) h, vec32.y, vec32.z * (double) i + vec32.x * (double) h);
        }
    }
}
