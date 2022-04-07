package net.sorenon.mcxr.play.openxr;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import net.sorenon.mcxr.core.mixin.LivingEntityAcc;
import net.sorenon.mcxr.play.MCXRGuiManager;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.PlayOptions;
import net.sorenon.mcxr.play.accessor.MinecraftExt;
import net.sorenon.mcxr.play.input.ControllerPoses;
import net.sorenon.mcxr.play.input.XrInput;
import net.sorenon.mcxr.play.rendering.MCXRCamera;
import net.sorenon.mcxr.play.rendering.MCXRMainTarget;
import net.sorenon.mcxr.play.rendering.RenderPass;
import net.sorenon.mcxr.play.rendering.XrRenderTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openxr.*;
import org.lwjgl.openxr.XrCompositionLayerProjectionView.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackCallocInt;
import static org.lwjgl.system.MemoryStack.stackPush;

public class MCXRGameRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private MinecraftClient client;
    private MinecraftExt clientExt;
    private MCXRMainTarget mainRenderTarget;
    private MCXRCamera camera;

    private OpenXRInstance instance;
    private OpenXRSession session;

    public RenderPass renderPass = RenderPass.VANILLA;
    public Shader blitShader;
    public Shader guiBlitShader;

    private boolean xrDisabled = false;
    private boolean xrReady = true;

    public void initialize(MinecraftClient client) {
        this.client = client;
        this.clientExt = (MinecraftExt) client;
        mainRenderTarget = (MCXRMainTarget) client.getFramebuffer();
        camera = (MCXRCamera) client.gameRenderer.getCamera();
    }

    public void setSession(OpenXRSession session) {
        this.session = session;
        if (session != null) {
            this.instance = session.instance;
        } else {
            this.instance = null;
        }
    }

    public boolean isXrMode() {
        return MinecraftClient.getInstance().world != null && session != null && session.running && xrReady && !xrDisabled;
    }

    public void renderFrame(boolean xrDisabled) {
        if (this.xrDisabled != xrDisabled) {
            MCXRPlayClient.resetView();
        }
        this.xrDisabled = xrDisabled;

        try (MemoryStack stack = stackPush()) {
            var frameState = XrFrameState.calloc(stack).type(XR10.XR_TYPE_FRAME_STATE);

            if (isXrMode()) {
                GLFW.glfwSwapBuffers(MinecraftClient.getInstance().getWindow().getHandle());
            }
            //TODO tick game and poll input during xrWaitFrame (this might not work due to the gl context belonging to the xrWaitFrame thread)
            instance.checkPanic(XR10.xrWaitFrame(
                    session.handle,
                    XrFrameWaitInfo.calloc(stack).type(XR10.XR_TYPE_FRAME_WAIT_INFO),
                    frameState
            ), "xrWaitFrame");

            xrReady = frameState.shouldRender();

            instance.checkPanic(XR10.xrBeginFrame(
                    session.handle,
                    XrFrameBeginInfo.calloc(stack).type(XR10.XR_TYPE_FRAME_BEGIN_INFO)
            ), "xrBeginFrame");

            PointerBuffer layers = stack.callocPointer(1);

            if (frameState.shouldRender()) {
                if (this.isXrMode() && !xrDisabled) {
                    var layer = renderXrGame(frameState.predictedDisplayTime(), stack);
                    if (layer != null) {
                        layers.put(layer.address());
                    }
                } else {
                    var layer = renderBlankLayer(frameState.predictedDisplayTime(), stack);
                    layers.put(layer.address());
                }
            }
            layers.flip();

            int result = XR10.xrEndFrame(
                    session.handle,
                    XrFrameEndInfo.calloc(stack)
                            .type(XR10.XR_TYPE_FRAME_END_INFO)
                            .displayTime(frameState.predictedDisplayTime())
                            .environmentBlendMode(XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
                            .layers(layers)
            );
            if (result != XR10.XR_ERROR_TIME_INVALID) {
                instance.checkPanic(result, "xrEndFrame");
            } else {
                LOGGER.warn("Rendering frame took too long! (probably)");
            }
        }
    }

    private Struct renderXrGame(long predictedDisplayTime, MemoryStack stack) {
//        try (MemoryStack stack = stackPush()) {

        XrViewState viewState = XrViewState.calloc(stack).type(XR10.XR_TYPE_VIEW_STATE);
        IntBuffer intBuf = stackCallocInt(1);

        XrViewLocateInfo viewLocateInfo = XrViewLocateInfo.calloc(stack);
        viewLocateInfo.set(XR10.XR_TYPE_VIEW_LOCATE_INFO,
                0,
                session.viewConfigurationType,
                predictedDisplayTime,
                session.xrAppSpace
        );

        instance.checkPanic(XR10.xrLocateViews(session.handle, viewLocateInfo, viewState, intBuf, session.viewBuffer), "xrLocateViews");

        if ((viewState.viewStateFlags() & XR10.XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                (viewState.viewStateFlags() & XR10.XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
            LOGGER.error("Invalid headset position, try restarting your device");
            return null;
        }

        var projectionLayerViews = XrCompositionLayerProjectionView.calloc(2, stack);

        MCXRGuiManager FGM = MCXRPlayClient.INSTANCE.MCXRGuiManager;

        if (FGM.needsReset) {
            FGM.resetTransform();
        }

        long frameStartTime = Util.getMeasuringTimeNano();

        //Ticks the game
        clientExt.preRender(true, () -> {
            //Pre-tick
            //Update poses for tick
            updatePoses(camera.getFocusedEntity(), false, predictedDisplayTime, 1.0f, MCXRPlayClient.getCameraScale());

            //Update the server-side player poses
            if (MinecraftClient.getInstance().player != null && MCXRCore.getCoreConfig().supportsMCXR()) {
                PlayerExt acc = (PlayerExt) MinecraftClient.getInstance().player;
                if (!acc.isXR()) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(true);
                    ClientPlayNetworking.send(MCXRCore.IS_XR_PLAYER, buf);
                    acc.setIsXr(true);
                }
                MCXRCore.INSTANCE.setPlayerPoses(
                        MinecraftClient.getInstance().player,
                        MCXRPlayClient.viewSpacePoses.getMinecraftPose(),
                        XrInput.handsActionSet.gripPoses[0].getMinecraftPose(),
                        XrInput.handsActionSet.gripPoses[1].getMinecraftPose(),
                        (float) Math.toRadians(PlayOptions.handPitchAdjust)
                );
            }
        });

        Entity cameraEntity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
        boolean calculate = false;
        if (XrInput.vanillaGameplayActionSet.stand.changedSinceLastSync && XrInput.vanillaGameplayActionSet.stand.currentState) {
            MCXRPlayClient.heightAdjustStand = !MCXRPlayClient.heightAdjustStand;
            if (MCXRPlayClient.heightAdjustStand) {
                calculate = true;
            }
        }

        float frameUserScale = MCXRPlayClient.getCameraScale(client.getTickDelta());
        updatePoses(cameraEntity, calculate, predictedDisplayTime, client.getTickDelta(), frameUserScale);
        camera.updateXR(this.client.world, cameraEntity, MCXRPlayClient.viewSpacePoses.getMinecraftPose());

        client.getWindow().setPhase("Render");
        client.getProfiler().push("sound");
        client.getSoundManager().updateListenerPosition(client.gameRenderer.getCamera());
        client.getProfiler().pop();

        //Render GUI
        mainRenderTarget.setFramebuffer(FGM.guiRenderTarget);
        //Need to do this once framebuffer is gui
        XrInput.postTick(predictedDisplayTime);

        mainRenderTarget.clear(MinecraftClient.IS_SYSTEM_MAC);
        clientExt.doRender(true, frameStartTime, RenderPass.GUI);
        mainRenderTarget.resetFramebuffer();

        FGM.guiPostProcessRenderTarget.beginWrite(true);
        this.guiBlitShader.addSampler("DiffuseSampler", FGM.guiRenderTarget.getColorAttachment());
        this.guiBlitShader.addSampler("DepthSampler", FGM.guiRenderTarget.getDepthAttachment());
        this.blit(FGM.guiPostProcessRenderTarget, guiBlitShader);
        FGM.guiPostProcessRenderTarget.endWrite();

        OpenXRSwapchain swapchain = session.swapchain;
        int swapchainImageIndex = swapchain.acquireImage();

        // Render view to the appropriate part of the swapchain image.
        for (int viewIndex = 0; viewIndex < 2; viewIndex++) {
            // Each view has a separate swapchain which is acquired, rendered to, and released.

            var subImage = projectionLayerViews.get(viewIndex)
                    .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW)
                    .pose(session.viewBuffer.get(viewIndex).pose())
                    .fov(session.viewBuffer.get(viewIndex).fov())
                    .subImage();
            subImage.swapchain(swapchain.handle);
            subImage.imageRect().offset().set(0, 0);
            subImage.imageRect().extent().set(swapchain.width, swapchain.height);
            subImage.imageArrayIndex(viewIndex);

            XrRenderTarget swapchainFramebuffer;
            if (viewIndex == 0) {
                swapchainFramebuffer = swapchain.leftFramebuffers[swapchainImageIndex];
            } else {
                swapchainFramebuffer = swapchain.rightFramebuffers[swapchainImageIndex];
            }
            mainRenderTarget.setXrFramebuffer(swapchain.renderTarget);
            RenderPass.XrWorld worldRenderPass = RenderPass.XrWorld.create();
            worldRenderPass.fov = session.viewBuffer.get(viewIndex).fov();
            worldRenderPass.eyePoses.updatePhysicalPose(session.viewBuffer.get(viewIndex).pose(), MCXRPlayClient.stageTurn, frameUserScale);
            worldRenderPass.eyePoses.updateGamePose(MCXRPlayClient.xrOrigin);
            worldRenderPass.viewIndex = viewIndex;
            camera.setPose(worldRenderPass.eyePoses.getMinecraftPose());
            clientExt.doRender(true, frameStartTime, worldRenderPass);

            swapchainFramebuffer.beginWrite(true);
            this.blitShader.addSampler("DiffuseSampler", swapchain.renderTarget.getColorAttachment());
            GlUniform inverseScreenSize = this.blitShader.getUniform("InverseScreenSize");
            if (inverseScreenSize != null) {
                inverseScreenSize.set(1f / swapchainFramebuffer.textureWidth, 1f / swapchainFramebuffer.textureHeight);
            }
            swapchain.renderTarget.setTexFilter(GlConst.GL_LINEAR);
            this.blit(swapchainFramebuffer, blitShader);
            swapchainFramebuffer.endWrite();
        }

        blitToBackbuffer(swapchain.renderTarget);

        instance.checkPanic(XR10.xrReleaseSwapchainImage(
                swapchain.handle,
                XrSwapchainImageReleaseInfo.calloc(stack)
                        .type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        ), "xrReleaseSwapchainImage");

        mainRenderTarget.resetFramebuffer();
        camera.setPose(MCXRPlayClient.viewSpacePoses.getMinecraftPose());
        clientExt.postRender();

        return XrCompositionLayerProjection.calloc(stack)
                .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION)
                .space(session.xrAppSpace)
                .views(projectionLayerViews);
//        }
    }

    private void updatePoses(Entity camEntity,
                             boolean calculateHeightAdjust,
                             long predictedDisplayTime,
                             float delta,
                             float scale) {
        if (session.state == XR10.XR_SESSION_STATE_FOCUSED) {
            for (int i = 0; i < 2; i++) {
                if (!XrInput.handsActionSet.grip.isActive[i]) {
                    continue;
                }
                session.setPosesFromSpace(XrInput.handsActionSet.grip.spaces[i], predictedDisplayTime, XrInput.handsActionSet.gripPoses[i], scale);
                session.setPosesFromSpace(XrInput.handsActionSet.aim.spaces[i], predictedDisplayTime, XrInput.handsActionSet.aimPoses[i], scale);
            }
            session.setPosesFromSpace(session.xrViewSpace, predictedDisplayTime, MCXRPlayClient.viewSpacePoses, scale);
        }

        if (camEntity != null) { //TODO seriously need to tidy up poses
            if (client.isPaused()) {
                delta = 1.0f;
            }

            if (calculateHeightAdjust && MCXRPlayClient.heightAdjustStand && camEntity instanceof LivingEntity livingEntity) {
                float userHeight = MCXRPlayClient.viewSpacePoses.getPhysicalPose().getPos().y();
                float playerEyeHeight = ((LivingEntityAcc) livingEntity).callGetStandingEyeHeight(livingEntity.getPose(), livingEntity.getDimensions(livingEntity.getPose()));

                MCXRPlayClient.heightAdjust = playerEyeHeight - userHeight;
            }

            Entity vehicle = camEntity.getVehicle();
            if (MCXRCore.getCoreConfig().roomscaleMovement() && vehicle == null) {
                MCXRPlayClient.xrOrigin.set(MathHelper.lerp(delta, camEntity.prevX, camEntity.getX()) - MCXRPlayClient.playerPhysicalPosition.x,
                        MathHelper.lerp(delta, camEntity.prevY, camEntity.getY()),
                        MathHelper.lerp(delta, camEntity.prevZ, camEntity.getZ()) - MCXRPlayClient.playerPhysicalPosition.z);
            } else {
                MCXRPlayClient.xrOrigin.set(MathHelper.lerp(delta, camEntity.prevX, camEntity.getX()),
                        MathHelper.lerp(delta, camEntity.prevY, camEntity.getY()),
                        MathHelper.lerp(delta, camEntity.prevZ, camEntity.getZ()));
            }
            if (vehicle != null) {
                if (vehicle instanceof LivingEntity) {
                    MCXRPlayClient.xrOrigin.y += 0.60;
                } else {
                    MCXRPlayClient.xrOrigin.y += 0.54 - vehicle.getMountedHeightOffset();
                }
            }
            if (MCXRPlayClient.heightAdjustStand) {
                MCXRPlayClient.xrOrigin.y += MCXRPlayClient.heightAdjust;
            }

            MCXRPlayClient.viewSpacePoses.updateGamePose(MCXRPlayClient.xrOrigin);
            for (var poses : XrInput.handsActionSet.gripPoses) {
                poses.updateGamePose(MCXRPlayClient.xrOrigin);
            }
            for (var poses : XrInput.handsActionSet.aimPoses) {
                poses.updateGamePose(MCXRPlayClient.xrOrigin);
            }
        }
    }

    private Struct renderBlankLayer(long predictedDisplayTime, MemoryStack stack) {
        IntBuffer intBuf = stackCallocInt(1);

        instance.checkPanic(XR10.xrLocateViews(
                session.handle,
                XrViewLocateInfo.calloc(stack).set(XR10.XR_TYPE_VIEW_LOCATE_INFO,
                        0,
                        session.viewConfigurationType,
                        predictedDisplayTime,
                        session.xrAppSpace
                ),
                XrViewState.calloc(stack).type(XR10.XR_TYPE_VIEW_STATE),
                intBuf,
                session.viewBuffer
        ), "xrLocateViews");

        int viewCountOutput = intBuf.get(0);

        var projectionLayerViews = XrCompositionLayerProjectionView.calloc(viewCountOutput);

        int swapchainImageIndex = session.swapchain.acquireImage();

        for (int viewIndex = 0; viewIndex < viewCountOutput; viewIndex++) {
            XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex);
            projectionLayerView.type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW);
            projectionLayerView.pose(session.viewBuffer.get(viewIndex).pose());
            projectionLayerView.fov(session.viewBuffer.get(viewIndex).fov());
            projectionLayerView.subImage().swapchain(session.swapchain.handle);
            projectionLayerView.subImage().imageRect().offset().set(0, 0);
            projectionLayerView.subImage().imageRect().extent().set(session.swapchain.width, session.swapchain.height);
            projectionLayerView.subImage().imageArrayIndex(0);
        }

        session.swapchain.leftFramebuffers[swapchainImageIndex].clear(MinecraftClient.IS_SYSTEM_MAC);

        instance.checkPanic(XR10.xrReleaseSwapchainImage(
                session.swapchain.handle,
                XrSwapchainImageReleaseInfo.calloc(stack).type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        ), "xrReleaseSwapchainImage");

        XrCompositionLayerProjection layer = XrCompositionLayerProjection.calloc(stack).type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION);
        layer.space(session.xrAppSpace);
        layer.views(projectionLayerViews);
        return layer;
    }

    public void blit(Framebuffer framebuffer, Shader shader) {
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.loadIdentity();
        RenderSystem.applyModelViewMatrix();

        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._disableBlend();

        Matrix4f matrix4f = Matrix4f.projectionMatrix((float) width, (float) -height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f);
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(Matrix4f.translate(0.0F, 0.0F, -2000.0F));
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(matrix4f);
        }

        shader.bind();
        float u = (float) framebuffer.viewportWidth / (float) framebuffer.textureWidth;
        float v = (float) framebuffer.viewportHeight / (float) framebuffer.textureHeight;
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0, height * 2, 0.0).texture(0.0F, 1 - v * 2).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width * 2, 0.0, 0.0).texture(u * 2, 1).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0F, 1).color(255, 255, 255, 255).next();
        bufferBuilder.end();
        BufferRenderer.postDraw(bufferBuilder);
        shader.unbind();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.pop();
    }

    public void blitToBackbuffer(Framebuffer framebuffer) {
        //TODO render alyx-like gui over this
        Shader shader = MinecraftClient.getInstance().gameRenderer.blitScreenShader;
        shader.addSampler("DiffuseSampler", framebuffer.getColorAttachment());

        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.loadIdentity();
        RenderSystem.applyModelViewMatrix();

        int width = mainRenderTarget.minecraftMainRenderTarget.textureWidth;
        int height = mainRenderTarget.minecraftMainRenderTarget.textureHeight;

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._disableBlend();

        Matrix4f matrix4f = Matrix4f.projectionMatrix((float) width, (float) (-height), 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f);
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(Matrix4f.translate(0, 0, -2000.0F));
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(matrix4f);
        }

        shader.bind();
        float widthNormalized = (float) framebuffer.textureWidth / (float) width;
        float heightNormalized = (float) framebuffer.textureHeight / (float) height;
        float v = (widthNormalized / heightNormalized) / 2;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0, height, 0.0).texture(0.0F, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture(1, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, 0.0, 0.0).texture(1, 1.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0F, 1.0F).color(255, 255, 255, 255).next();
        bufferBuilder.end();
        BufferRenderer.postDraw(bufferBuilder);
        shader.unbind();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.pop();
    }
}
