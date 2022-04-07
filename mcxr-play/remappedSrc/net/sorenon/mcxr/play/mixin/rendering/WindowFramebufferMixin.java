package net.sorenon.mcxr.play.mixin.rendering;

import net.minecraft.client.gl.WindowFramebuffer;
import net.sorenon.mcxr.play.rendering.MCXRMainTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WindowFramebuffer.class)
public class WindowFramebufferMixin {

    @Inject(method = "createFrameBuffer", at = @At("HEAD"), cancellable = true)
    void cancel(int width, int height, CallbackInfo ci){
        //noinspection ConstantConditions
        if ((Object)this instanceof MCXRMainTarget) {
            ci.cancel();
        }
    }
}
