package net.sorenon.mcxr.play.mixin.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.openxr.MCXRGameRenderer;
import net.sorenon.mcxr.play.rendering.MCXRMainTarget;
import net.sorenon.mcxr.play.rendering.MCXRCamera;
import net.sorenon.mcxr.play.rendering.RenderPass;
import net.sorenon.mcxr.play.accessor.Matrix4fExt;
import org.joml.Quaternionf;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 10_000)
public abstract class GameRendererMixin {

    @Unique
    private static final MCXRGameRenderer XR_RENDERER = MCXRPlayClient.MCXR_GAME_RENDERER;

    @Shadow
    public abstract float getRenderDistance();

    @Shadow
    @Final
    private MinecraftClient minecraft;

    @Shadow
    private boolean renderHand;

    /**
     * Replace the default camera with an MCXRCamera
     */
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/Camera"))
    Camera replaceCamera() {
        return new MCXRCamera();
    }

    /**
     * Update the framebuffer dimensions in MCXRMainTarget so we know if framebuffers need resizing
     */
    @Inject(method = "resize", at = @At("HEAD"))
    void onResized(int i, int j, CallbackInfo ci) {
        MCXRMainTarget MCXRMainTarget = (MCXRMainTarget) minecraft.getFramebuffer();
        MCXRMainTarget.minecraftFramebufferWidth = i;
        MCXRMainTarget.minecraftFramebufferHeight = j;
    }

    /**
     * Cancels both vanilla and Iris hand rendering
     */
    @Inject(method = "renderLevel", at = @At("HEAD"))
    void cancelRenderHand(CallbackInfo ci) {
        this.renderHand = XR_RENDERER.renderPass == RenderPass.VANILLA;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    void cancelBobView(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            ci.cancel();
        }
    }

    /**
     * Replace the vanilla projection matrix
     */
    @Inject(method = "getProjectionMatrix", at = @At("HEAD"), cancellable = true)
    void getXrProjectionMatrix(double d, CallbackInfoReturnable<Matrix4f> cir) {
        if (XR_RENDERER.renderPass instanceof RenderPass.XrWorld renderPass) {
            Matrix4f proj = new Matrix4f();
            ((Matrix4fExt) (Object) proj).setXrProjection(renderPass.fov, 0.05F, this.getRenderDistance() * 4);
            cir.setReturnValue(proj);
        }
    }

    /**
     * Rotate the matrix stack using a quaternion rather than pitch and yaw
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lcom/mojang/math/Quaternion;)V", ordinal = 2), method = "renderLevel")
    void multiplyPitch(MatrixStack matrixStack, Quaternion pitchQuat) {
        if (XR_RENDERER.renderPass == RenderPass.VANILLA) {
            matrixStack.multiply(pitchQuat);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lcom/mojang/math/Quaternion;)V", ordinal = 3), method = "renderLevel")
    void multiplyYaw(MatrixStack matrixStack, Quaternion yawQuat) {
        if (XR_RENDERER.renderPass instanceof RenderPass.XrWorld xrWorldPass) {
            var inv = xrWorldPass.eyePoses.getMinecraftPose().getOrientation().invert(new Quaternionf());
            matrixStack.multiply(new Quaternion(inv.x, inv.y, inv.z, inv.w));
        } else {
            matrixStack.multiply(yawQuat);
        }
    }

    /**
     * If we are doing a gui render pass => return null to skip rendering the world
     */
    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;", opcode = Opcodes.GETFIELD, ordinal = 0), method = "render")
    public ClientWorld getWorld(MinecraftClient client) {
        if (XR_RENDERER.renderPass == RenderPass.GUI) {
            return null;
        } else {
            return client.world;
        }
    }

    /**
     * If we are doing a world render pass => return early to skip rendering the gui
     */
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", ordinal = 0, shift = At.Shift.BEFORE), method = "render", cancellable = true)
    public void guiRenderStart(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (XR_RENDERER.renderPass instanceof RenderPass.XrWorld) {
            ci.cancel();
        }
    }
}
