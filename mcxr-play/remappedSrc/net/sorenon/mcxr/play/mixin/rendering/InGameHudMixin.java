package net.sorenon.mcxr.play.mixin.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.openxr.MCXRGameRenderer;
import net.sorenon.mcxr.play.rendering.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Unique
    private static final MCXRGameRenderer XR_RENDERER = MCXRPlayClient.MCXR_GAME_RENDERER;

    /**
     * Skip rendering the vignette then setup the GUI rendering state
     */
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    void cancelRenderVignette(Entity entity, CallbackInfo ci) {
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    void cancelRenderCrosshair(MatrixStack matrices, CallbackInfo ci){
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            ci.cancel();
        }
    }
}
