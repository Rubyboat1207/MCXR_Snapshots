package net.sorenon.mcxr.play.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.sorenon.mcxr.play.mixin.accessor.RenderTargetAcc;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

public class XrRenderTarget extends SimpleFramebuffer {

    public XrRenderTarget(int width, int height, int color, int index) {
        super(width, height, true, MinecraftClient.IS_SYSTEM_MAC);

        ((RenderTargetAcc) this).setColorTextureId(color);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, color, 0, index);

        this.setClearColor(sRGBToLinear(239 / 255f), sRGBToLinear(50 / 255f), sRGBToLinear(61 / 255f), 255 / 255f);
    }

    private float sRGBToLinear(float f) {
        if (f < 0.04045f) {
            return f / 12.92f;
        } else {
            return (float) Math.pow((f + 0.055f) / 1.055f, 2.4f);
        }
    }

    @Override
    public void initFbo(int width, int height, boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            this.textureWidth = width;
            this.textureHeight = height;
            this.fbo = GlStateManager.glGenFramebuffers();
//            this.colorAttachment = TextureUtil.generateTextureId();
            if (this.useDepthAttachment) {
                this.depthAttachment = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(this.depthAttachment);
                GlStateManager._texParameter(3553, 10241, 9728);
                GlStateManager._texParameter(3553, 10240, 9728);
                GlStateManager._texParameter(3553, 34892, 0);
                GlStateManager._texParameter(3553, 10242, 33071);
                GlStateManager._texParameter(3553, 10243, 33071);
                GlStateManager._texImage2D(3553, 0, 6402, this.textureWidth, this.textureHeight, 0, 6402, 5126, (IntBuffer) null);
            }

            this.setTexFilter(9728);
//            GlStateManager._bindTexture(this.colorAttachment);
//            GlStateManager._texParameter(3553, 10242, 33071);
//            GlStateManager._texParameter(3553, 10243, 33071);
//            GlStateManager._texImage2D(3553, 0, 32856, this.textureWidth, this.textureHeight, 0, 6408, 5121, (IntBuffer)null);
            GlStateManager._glBindFramebuffer(36160, this.fbo);
//            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorAttachment, 0);
            if (this.useDepthAttachment) {
                GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthAttachment, 0);
            }

            this.checkFramebufferStatus();
            this.clear(getError);
            this.endRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }
}
