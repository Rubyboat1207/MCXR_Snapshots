package net.sorenon.mcxr.core.mixin.hands;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow public abstract Hand getUsedItemHand();

    public LivingEntityMixin(EntityType<?> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "releaseUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    void preReleaseUsing(CallbackInfo ci) {
        if (this instanceof PlayerExt playerExt && playerExt.isXR()) {
            playerExt.getOverrideTransform().set(MCXRCore.handToArm((LivingEntity)(Object)this, this.getUsedItemHand()));
        }
    }

    @Inject(method = "releaseUsingItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    void postReleaseUsing(CallbackInfo ci) {
        if (this instanceof PlayerExt playerExt && playerExt.isXR()) {
            playerExt.getOverrideTransform().set(null);
        }
    }
}
