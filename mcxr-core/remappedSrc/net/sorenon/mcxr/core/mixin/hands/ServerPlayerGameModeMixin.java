package net.sorenon.mcxr.core.mixin.hands;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"))
    void preUse(ServerPlayerEntity serverPlayer,
                World level,
                ItemStack itemStack,
                Hand interactionHand,
                CallbackInfoReturnable<ActionResult> cir) {
        PlayerExt playerExt = ((PlayerExt) serverPlayer);
        if (playerExt.isXR()) {
            playerExt.getOverrideTransform().set(MCXRCore.handToArm(serverPlayer, interactionHand));
        }
    }

    @Inject(method = "useItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"))
    void postUse(ServerPlayerEntity serverPlayer,
                 World level,
                 ItemStack itemStack,
                 Hand interactionHand,
                 CallbackInfoReturnable<ActionResult> cir) {
        PlayerExt playerExt = ((PlayerExt) serverPlayer);
        if (playerExt.isXR()) {
            playerExt.getOverrideTransform().set(null);
        }
    }
}
