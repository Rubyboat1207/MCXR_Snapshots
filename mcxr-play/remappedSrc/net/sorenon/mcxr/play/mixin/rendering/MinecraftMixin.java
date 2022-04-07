package net.sorenon.mcxr.play.mixin.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.*;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.MetricsData;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.accessor.PlayerExt;
import net.sorenon.mcxr.play.MCXRGuiManager;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.accessor.MinecraftExt;
import net.sorenon.mcxr.play.mixin.accessor.WindowAcc;
import net.sorenon.mcxr.play.openxr.MCXRGameRenderer;
import net.sorenon.mcxr.play.openxr.OpenXRState;
import net.sorenon.mcxr.play.openxr.XrRuntimeException;
import net.sorenon.mcxr.play.rendering.MCXRMainTarget;
import net.sorenon.mcxr.play.rendering.RenderPass;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openxr.XR10;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MinecraftMixin extends ReentrantThreadExecutor<Runnable> implements MinecraftExt {

    public MinecraftMixin(String string) {
        super(string);
    }

    @Shadow
    private Profiler profiler;

    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract void stop();

    @Shadow
    @Nullable
    private CompletableFuture<Void> pendingReload;

    @Shadow
    @Nullable
    private Overlay overlay;

    @Shadow
    public abstract CompletableFuture<Void> reloadResourcePacks();

    @Shadow
    @Final
    private Queue<Runnable> progressTasks;

    @Shadow
    @Final
    private RenderTickCounter timer;

    @Shadow
    public abstract void tick();

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Mutable
    @Shadow
    @Final
    private Framebuffer mainRenderTarget;

    @Shadow
    public boolean noRender;

    @Shadow
    private boolean pause;

    @Shadow
    private float pausePartialTick;

    @Shadow
    @Final
    private ToastManager toast;

    @Shadow
    @Nullable
    private ProfileResult fpsPieResults;

    @Shadow
    protected abstract void renderFpsMeter(MatrixStack matrices, ProfileResult profileResult);

    @Shadow
    private int frames;

    @Shadow
    public abstract boolean hasSingleplayerServer();

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    @Final
    public MetricsData frameTimer;

    @Shadow
    private long lastNanoTime;

    @Shadow
    private long lastTime;

    @Shadow
    private static int fps;

    @Shadow
    public String fpsString;

    @Shadow
    @Nullable
    private IntegratedServer singleplayerServer;

    @Shadow
    @Final
    public static boolean ON_OSX;

    @Shadow
    protected abstract void runTick(boolean tick);

    @Shadow
    @Nullable
    public ClientWorld level;

    @Shadow
    @Final
    public GameOptions options;

    @Shadow
    protected abstract void openChatScreen(String string);

    @Shadow
    public abstract void resizeDisplay();

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    public abstract void prepareForMultiplayer();

    @Unique
    private static final MCXRGameRenderer XR_RENDERER = MCXRPlayClient.MCXR_GAME_RENDERER;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "com/mojang/blaze3d/pipeline/MainTarget"))
    WindowFramebuffer createFramebuffer(int width, int height) {
        return new MCXRMainTarget(width, height);
    }

    @Inject(method = "run", at = @At("HEAD"))
    void start(CallbackInfo ci) {
        MCXRPlayClient.INSTANCE.MCXRGuiManager.init();
    }

    private boolean renderedNormallyLastFrame = true;

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"), method = "run")
    void loop(MinecraftClient minecraftClient, boolean tick) {
        OpenXRState openXRState = MCXRPlayClient.OPEN_XR_STATE;
        //TODO build a more rusty error system to handle this
        try {
            if (openXRState.loop()) {
                if (!renderedNormallyLastFrame) {
                    MCXRPlayClient.LOGGER.info("Resizing framebuffers due to XR -> Pancake transition");
                    this.resizeDisplay();
                }
                if (this.player != null && MCXRCore.getCoreConfig().supportsMCXR()) {
                    PlayerExt acc = (PlayerExt) this.player;
                    if (acc.isXR()) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(false);
                        ClientPlayNetworking.send(MCXRCore.IS_XR_PLAYER, buf);
                        acc.setIsXr(false);
                        this.player.calculateDimensions();
                    }
                }
                //Just render normally
                runTick(tick);
                renderedNormallyLastFrame = true;
            } else {
                if (renderedNormallyLastFrame) {
                    if (this.screen != null) {
                        MCXRPlayClient.LOGGER.info("Resizing gui due to Pancake -> XR transition");
                        var fgm = MCXRPlayClient.INSTANCE.MCXRGuiManager;
                        this.screen.resize((MinecraftClient) (Object) this, fgm.scaledWidth, fgm.scaledHeight);
                        fgm.needsReset = true;
                    }
                }
                renderedNormallyLastFrame = false;
            }
        } catch (XrRuntimeException runtimeException) {
            openXRState.session.close();
            openXRState.session = null;
            MCXRPlayClient.MCXR_GAME_RENDERER.setSession(null);

            if (runtimeException.result != XR10.XR_ERROR_SESSION_LOST) {
                openXRState.instance.close();
                openXRState.instance = null;
            }

            runtimeException.printStackTrace();
        }
    }

    /**
     */
    @Override
    public void preRender(boolean tick, Runnable preTick) {
        this.window.setPhase("Pre render");
        if (this.window.shouldClose()) {
            this.stop();
        }

        if (this.pendingReload != null && !(this.overlay instanceof SplashOverlay)) {
            CompletableFuture<Void> completableFuture = this.pendingReload;
            this.pendingReload = null;
            this.reloadResourcePacks().thenRun(() -> {
                completableFuture.complete(null);
            });
        }

        Runnable runnable;
        while ((runnable = this.progressTasks.poll()) != null) {
            runnable.run();
        }

        if (tick) {
            int i = this.timer.beginRenderTick(Util.getMeasuringTimeMs());
            this.profiler.push("scheduledExecutables");
            this.runTasks();
            this.profiler.pop();
            this.profiler.push("tick");

            for (int j = 0; j < Math.min(10, i); ++j) {
                this.profiler.visit("clientTick");
                preTick.run();
                this.tick();
            }

            this.profiler.pop();
        }
    }

    @Override
    public void doRender(boolean tick, long frameStartTime, RenderPass renderPass) {
        XR_RENDERER.renderPass = renderPass;
        this.profiler.push("render");
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(16640, ON_OSX);
        this.mainRenderTarget.beginWrite(true);
        BackgroundRenderer.clearFog();
        this.profiler.push("display");
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        this.profiler.pop();
        if (!this.noRender) {
            this.profiler.swap("gameRenderer");
            this.gameRenderer.render(this.pause ? this.pausePartialTick : this.timer.tickDelta, frameStartTime, tick);

            if (XR_RENDERER.renderPass == RenderPass.GUI || XR_RENDERER.renderPass == RenderPass.VANILLA) {
                this.profiler.swap("toasts");
                this.toast.draw(new MatrixStack());
                this.profiler.pop();
            }
        }

        if (this.fpsPieResults != null) {
            this.profiler.push("fpsPie");
            this.renderFpsMeter(new MatrixStack(), this.fpsPieResults);
            this.profiler.pop();
        }

//        this.profiler.push("blit");
        this.mainRenderTarget.endWrite();
        matrixStack.pop();

        XR_RENDERER.renderPass = RenderPass.VANILLA;
    }

    @Override
    public void postRender() {
        GLFW.glfwPollEvents();
        RenderSystem.replayQueue();
        Tessellator.getInstance().getBuffer().clear();
//        GLFW.glfwSwapBuffers(window.getHandle());
        WindowAcc windowAcc = ((WindowAcc) (Object) window);
        if (window.isFullscreen() != windowAcc.getActuallyFullscreen()) {
            windowAcc.setActuallyFullscreen(window.isFullscreen());
            windowAcc.invokeUpdateFullscreen(windowAcc.getVsync());
        }
        GLFW.glfwPollEvents();

        this.window.setPhase("Post render");
        ++this.frames;
        boolean bl = this.hasSingleplayerServer() && (this.screen != null && this.screen.shouldPause() || this.overlay != null && this.overlay.pausesGame()) && !this.singleplayerServer.isRemote();
        if (this.pause != bl) {
            if (this.pause) {
                this.pausePartialTick = this.timer.tickDelta;
            } else {
                this.timer.tickDelta = this.pausePartialTick;
            }

            this.pause = bl;
        }

        long m = Util.getMeasuringTimeNano();
        this.frameTimer.pushSample(m - this.lastNanoTime);
        this.lastNanoTime = m;
        this.profiler.push("fpsUpdate");

        while (Util.getMeasuringTimeMs() >= this.lastTime + 1000L) {
            fps = this.frames;
            this.lastTime += 1000L;
            this.frames = 0;
        }

        this.profiler.pop();
    }
}
