package net.sorenon.mcxr.core.accessor;

import net.minecraft.util.Arm;
import net.sorenon.mcxr.core.Pose;

public interface PlayerExt {

    Pose getHeadPose();

    Pose getLeftHandPose();

    Pose getRightHandPose();

    default Pose getPoseForArm(Arm arm) {
        if (arm == Arm.LEFT) {
            return getLeftHandPose();
        } else {
            return getRightHandPose();
        }
    }

    void setIsXr(boolean isXr);

    boolean isXR();

    ThreadLocal<Arm> getOverrideTransform();
}
