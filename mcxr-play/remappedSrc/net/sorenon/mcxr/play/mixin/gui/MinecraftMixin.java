package net.sorenon.mcxr.play.mixin.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.sorenon.mcxr.play.MCXRGuiManager;
import net.sorenon.mcxr.play.MCXRPlayClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {

    @Shadow @Nullable public ClientWorld level;

    @Shadow @Nullable public Screen screen;

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;reset()V"))
    void openScreen(Screen screen, CallbackInfo ci) {
        MCXRPlayClient.INSTANCE.MCXRGuiManager.handleOpenScreen(this.screen);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"), method = "setScreen")
    void alterScreenSize(Screen screen, MinecraftClient client, int widthIn, int heightIn) {
        if (MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode()) {
            MCXRGuiManager FGM = MCXRPlayClient.INSTANCE.MCXRGuiManager;
            screen.init(client, FGM.scaledWidth, FGM.scaledHeight);
        } else {
            screen.init(client, widthIn, heightIn);
        }
    }
}
