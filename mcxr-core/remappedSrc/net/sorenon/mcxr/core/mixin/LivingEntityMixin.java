package net.sorenon.mcxr.core.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    void overrideEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (!MCXRCore.getCoreConfig().dynamicPlayerEyeHeight()) {
            return;
        }

        if (this instanceof PlayerExt acc && acc.isXR()) {
            cir.setReturnValue(acc.getHeadPose().pos.y - (float) this.getPos().y);
        }
    }
}
