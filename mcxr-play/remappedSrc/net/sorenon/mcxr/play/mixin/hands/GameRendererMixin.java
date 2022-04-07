package net.sorenon.mcxr.play.mixin.hands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sorenon.mcxr.core.JOMLUtil;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.core.Pose;
import net.sorenon.mcxr.play.PlayOptions;
import net.sorenon.mcxr.play.input.XrInput;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient minecraft;

    @Unique
    private boolean enabled() {
        return MCXRCore.getCoreConfig().controllerRaytracing() && MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode();
    }

    @Inject(method = "pick", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private void overrideEntity$raycast(float tickDelta, CallbackInfo ci) {
        if (enabled()) {
            Entity entity = this.minecraft.getCameraEntity();
            Pose pose = XrInput.handsActionSet.gripPoses[MCXRPlayClient.getMainHand()].getMinecraftPose();
            Vec3d pos = JOMLUtil.convert(pose.getPos());
            Vector3f dir1 = pose.getOrientation().rotateX((float) Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf()).transform(new Vector3f(0, -1, 0));
            Vec3d dir = new Vec3d(dir1.x, dir1.y, dir1.z);
            Vec3d endPos = pos.add(dir.multiply(this.minecraft.interactionManager.getReachDistance()));
            this.minecraft.crosshairTarget = entity.world.raycast(new RaycastContext(pos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));
        }
    }

    @ModifyVariable(method = "pick", ordinal = 0,
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3d alterStartPosVec(Vec3d value) {
        if (enabled()) {
            Pose pose = XrInput.handsActionSet.gripPoses[MCXRPlayClient.getMainHand()].getMinecraftPose();
            return JOMLUtil.convert(pose.getPos());
        } else {
            return value;
        }
    }

    @ModifyVariable(method = "pick", ordinal = 1,
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3d alterDirVec(Vec3d value) {
        if (enabled()) {
            Pose pose = XrInput.handsActionSet.gripPoses[MCXRPlayClient.getMainHand()].getMinecraftPose();
            return JOMLUtil.convert(
                    pose.getOrientation()
                            .rotateX((float) Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf())
                            .transform(new Vector3f(0, -1, 0))
            );
        } else {
            return value;
        }
    }
}
