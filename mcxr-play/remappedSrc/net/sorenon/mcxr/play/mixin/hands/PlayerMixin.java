package net.sorenon.mcxr.play.mixin.hands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.PlayOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends Entity {

    @Unique
    private boolean isActive() {
        //noinspection ConstantConditions
        return MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode() && (LivingEntity) (Object) this instanceof ClientPlayerEntity;
    }

    public PlayerMixin(EntityType<?> entityType, World level) {
        super(entityType, level);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    Vec3d changeSwimDirection(PlayerEntity instance) {
        if (isActive()) {
            Vec3d result = PlayOptions.swimDirection.getLookDirection();
            if (result != null) {
                return result;
            }
        }
        return this.getRotationVector();
    }
}
