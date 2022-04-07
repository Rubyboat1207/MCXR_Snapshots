package net.sorenon.mcxr.play.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.sorenon.mcxr.play.mixin.accessor.RenderTargetAcc;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a system to change the main framebuffer that the game renders to while providing the illusion that
 * it is always the same framebuffer. This limits compat issues while still providing a simple interface for changing the
 * size and target of the main framebuffer.
 * However, this class is also the main incompatibility issue with Canvas so a different way to achieve this is needed
 */
//TODO Find a way to make this compatible with Canvas's PrimaryFrameBuffer / FabulousFrameBuffer?
public class MCXRMainTarget extends WindowFramebuffer {

    public static final Logger LOGGER = Logger.getLogger("MCXR");

    //The framebuffer used for rendering to the window
    public final WindowFramebuffer minecraftMainRenderTarget;

    //The framebuffer that is affected by draw calls
    private Framebuffer currentFramebuffer;

    //The current dimensions of all the vanilla framebuffers
    public int minecraftFramebufferWidth;
    public int minecraftFramebufferHeight;

    public MCXRMainTarget(int width, int height) {
        super(width, height);
        minecraftMainRenderTarget = new WindowFramebuffer(width, height);
        setFramebuffer(minecraftMainRenderTarget);
        minecraftFramebufferWidth = width;
        minecraftFramebufferHeight = height;
    }

    //Used to set the current framebuffer without resizing the dimensions of the other framebuffers
    //This is meant for the defaultFramebuffer and any framebuffers used in rendering gui
    public void setFramebuffer(Framebuffer framebuffer) {
        this.currentFramebuffer = framebuffer;

        this.textureWidth = framebuffer.textureWidth;
        this.textureHeight = framebuffer.textureHeight;
        this.viewportWidth = framebuffer.viewportWidth;
        this.viewportHeight = framebuffer.viewportHeight;
        this.fbo = framebuffer.fbo;
//        this.clearColor[0] = framebuffer.clearColor[0];
//        this.clearColor[1] = framebuffer.clearColor[1];
//        this.clearColor[2] = framebuffer.clearColor[2];
//        this.clearColor[3] = framebuffer.clearColor[3];
        this.texFilter = framebuffer.texFilter;

        RenderTargetAcc acc = ((RenderTargetAcc) this);
        acc.setColorTextureId(framebuffer.getColorAttachment());
        acc.setDepthBufferId(framebuffer.getDepthAttachment());
    }

    public void setXrFramebuffer(Framebuffer framebuffer) {
        setFramebuffer(framebuffer);
        if (framebuffer.textureWidth != minecraftFramebufferWidth ||
                framebuffer.textureHeight != minecraftFramebufferHeight) {
            MinecraftClient.getInstance().gameRenderer.onResized(framebuffer.textureWidth, framebuffer.textureHeight);
            LOGGER.log(Level.FINE, "Resizing GameRenderer");
        }
    }

    public void resetFramebuffer() {
        setFramebuffer(minecraftMainRenderTarget);
    }

    public Framebuffer getFramebuffer() {
        return currentFramebuffer;
    }

    public Framebuffer getMinecraftMainRenderTarget() {
        return minecraftMainRenderTarget;
    }

    public boolean isCustomFramebuffer() {
        return currentFramebuffer != minecraftMainRenderTarget;
    }

    public void resize(int width, int height, boolean getError) {
        if (minecraftMainRenderTarget != null) {
            minecraftMainRenderTarget.resize(width, height, getError);
        }
    }

    public void delete() {
        minecraftMainRenderTarget.delete();
    }

    public void copyDepthFrom(Framebuffer framebuffer) {
        currentFramebuffer.copyDepthFrom(framebuffer);
    }

    public void initFbo(int width, int height, boolean getError) {
        currentFramebuffer.initFbo(width, height, getError);
    }

    public void setTexFilter(int i) {
        currentFramebuffer.setTexFilter(i);
    }

    public void checkFramebufferStatus() {
        currentFramebuffer.checkFramebufferStatus();
    }

    public void beginRead() {
        currentFramebuffer.beginRead();
    }

    public void endRead() {
        currentFramebuffer.endRead();
    }

    public void beginWrite(boolean setViewport) {
        currentFramebuffer.beginWrite(setViewport);
    }

    public void endWrite() {
        currentFramebuffer.endWrite();
    }

    public void setClearColor(float r, float g, float b, float a) {
        currentFramebuffer.setClearColor(r, g, b, a);
    }

    public void draw(int width, int height) {
        currentFramebuffer.draw(width, height);
    }

    public void draw(int width, int height, boolean bl) {
        currentFramebuffer.draw(width, height, bl);
    }

    public void clear(boolean getError) {
        currentFramebuffer.clear(getError);
    }

    public int getColorAttachment() {
        return currentFramebuffer.getColorAttachment();
    }

    public int getDepthAttachment() {
        return currentFramebuffer.getDepthAttachment();
    }
}
