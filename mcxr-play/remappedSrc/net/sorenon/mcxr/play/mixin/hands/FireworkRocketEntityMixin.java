package net.sorenon.mcxr.play.mixin.hands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.PlayOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    Vec3d changeFlyDirection(LivingEntity instance) {
        if (instance instanceof ClientPlayerEntity && MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode()) {
            Vec3d result = PlayOptions.flyDirection.getLookDirection();
            if (result != null) {
                return result;
            }
        }
        return instance.getRotationVector();
    }
}
