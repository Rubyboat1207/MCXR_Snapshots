package net.sorenon.mcxr.play.mixin.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.sorenon.mcxr.play.rendering.MCXRMainTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class LevelRendererMixin {

    @Shadow @Final private MinecraftClient minecraft;

    @Inject(method = "graphicsChanged()V", at = @At("RETURN"))
    void onGraphicsChanged(CallbackInfo ci) {
        MCXRMainTarget MCXRMainTarget = (MCXRMainTarget) minecraft.getFramebuffer();
        MCXRMainTarget.minecraftFramebufferWidth = minecraft.getFramebuffer().textureWidth;
        MCXRMainTarget.minecraftFramebufferHeight = minecraft.getFramebuffer().textureHeight;
    }

//    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
//    void cancelSetupRender(CallbackInfo ci) {
//        if (MCXRPlayClient.RENDERER.renderPass != RenderPass.VANILLA && MCXRPlayClient.RENDERER.eye != 0) {
//            ci.cancel();
//        }
//    }
}
