package net.sorenon.mcxr.play.mixin.accessor;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Framebuffer.class)
public interface RenderTargetAcc {

    @Accessor
    void setColorTextureId(int colorAttachment);

    @Accessor
    void setDepthBufferId(int depthAttachment);
}
