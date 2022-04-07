package net.sorenon.mcxr.play;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Vec3d;
import net.sorenon.mcxr.core.JOMLUtil;
import net.sorenon.mcxr.core.Pose;
import net.sorenon.mcxr.play.input.XrInput;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public enum MoveDirectionPose {
    Head,
    RightHand,
    LeftHand;

    public Text toComponent() {
        switch (this) {
            case Head -> {
                return new TranslatableText("mcxr.move_direction.head");
            }
            case RightHand -> {
                return new TranslatableText("mcxr.move_direction.right_hand");
            }
            case LeftHand -> {
                return new TranslatableText("mcxr.move_direction.left_hand");
            }
            default -> throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public MoveDirectionPose iterate() {
        boolean next = !InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT);
        switch (this) {
            case Head -> {
                return next ? RightHand : LeftHand;
            }
            case RightHand -> {
                return next ? LeftHand : Head;
            }
            case LeftHand -> {
                return next ? Head : RightHand;
            }
            default -> throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    @Nullable
    public Vec3d getLookDirection() {
        Quaternionf orientation = new Quaternionf();
        switch (this) {
            case Head -> {
                return null;
            }
            case RightHand -> XrInput.handsActionSet.gripPoses[1].getMinecraftPose().getOrientation().get(orientation);
            case LeftHand -> XrInput.handsActionSet.gripPoses[0].getMinecraftPose().getOrientation().get(orientation);
        }
        orientation = orientation.rotateX(Math.toRadians(PlayOptions.handPitchAdjust));
        return JOMLUtil.convert(orientation.transform(new Vector3d(0, -1, 0)));
    }

    public Optional<Float> getMCYaw() {
        Quaternionf orientation = new Quaternionf();
        switch (this) {
            case Head -> {
                return Optional.empty();
            }
            case RightHand -> XrInput.handsActionSet.gripPoses[1].getMinecraftPose().getOrientation().get(orientation);
            case LeftHand -> XrInput.handsActionSet.gripPoses[0].getMinecraftPose().getOrientation().get(orientation);
        }
        orientation = orientation.rotateX(Math.toRadians(PlayOptions.handPitchAdjust));
        return Optional.of(Pose.getMCYaw(orientation, new Vector3f(0, -1, 0)));
    }

    public Optional<Float> getMCPitch() {
        Quaternionf orientation = new Quaternionf();
        switch (this) {
            case Head -> {
                return Optional.empty();
            }
            case RightHand -> XrInput.handsActionSet.gripPoses[1].getMinecraftPose().getOrientation().get(orientation);
            case LeftHand -> XrInput.handsActionSet.gripPoses[0].getMinecraftPose().getOrientation().get(orientation);
        }
        orientation = orientation.rotateX(Math.toRadians(PlayOptions.handPitchAdjust));
        return Optional.of(Pose.getMCPitch(orientation, new Vector3f(0, -1, 0)));
    }
}
