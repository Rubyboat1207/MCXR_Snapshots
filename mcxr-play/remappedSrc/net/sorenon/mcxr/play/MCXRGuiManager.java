package net.sorenon.mcxr.play;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sorenon.mcxr.core.JOMLUtil;
import net.sorenon.mcxr.play.rendering.UnownedTexture;
import net.sorenon.mcxr.play.rendering.MCXRCamera;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

public class MCXRGuiManager {

    public final int guiFramebufferWidth = 1980;
    public final int guiFramebufferHeight = 1080;

    public final Identifier guiRenderTexture = new Identifier("mcxr", "gui");

    public Framebuffer guiRenderTarget;
    public Framebuffer guiPostProcessRenderTarget;

    public double guiScale;

    public int scaledWidth;
    public int scaledHeight;

    public boolean needsReset = true;

    public float size = 1.5f;

    /**
     * The transform of the GUI in physical space
     */
    public Vec3d position = null;
    public Quaterniond orientation = new Quaterniond(0, 0, 0, 1);

    public void init() {
        guiScale = calcGuiScale();

        int widthFloor = (int) (guiFramebufferWidth / guiScale);
        scaledWidth = guiFramebufferWidth / guiScale > widthFloor ? widthFloor + 1 : widthFloor;

        int heightFloor = (int) (guiFramebufferHeight / guiScale);
        scaledHeight = guiFramebufferHeight / guiScale > heightFloor ? heightFloor + 1 : heightFloor;

        guiRenderTarget = new SimpleFramebuffer(guiFramebufferWidth, guiFramebufferHeight, true, MinecraftClient.IS_SYSTEM_MAC);
        guiRenderTarget.setClearColor(0, 0, 0, 0);
        guiPostProcessRenderTarget = new SimpleFramebuffer(guiFramebufferWidth, guiFramebufferHeight, false, MinecraftClient.IS_SYSTEM_MAC);
        MinecraftClient.getInstance().getTextureManager().registerTexture(guiRenderTexture, new UnownedTexture(guiPostProcessRenderTarget.getColorAttachment()));
    }

    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public double calcGuiScale() {
        int guiScale = 4;
        boolean forceUnicodeFont = MinecraftClient.getInstance().forcesUnicodeFont();

        int scale;
        scale = 1;
        while (scale != guiScale && scale < guiFramebufferWidth && scale < guiFramebufferHeight && guiFramebufferWidth / (scale + 1) >= 320 && guiFramebufferHeight / (scale + 1) >= 240) {
            ++scale;
        }

        if (forceUnicodeFont && scale % 2 != 0) {
            ++scale;
        }
        return scale;
    }

    public boolean isScreenOpen() {
        return MinecraftClient.getInstance().currentScreen != null;
    }

    public void handleOpenScreen(@Nullable Screen screen) {
        if (screen == null) {
            position = null;
            orientation.set(0, 0, 0, 1);
            needsReset = false;
        } else if (position == null) {
            resetTransform();
        }
    }

    public void resetTransform() {
        MCXRCamera camera = (MCXRCamera) MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera.isReady()) {
            orientation = JOMLUtil.convertd(camera.getRotation());
            position = JOMLUtil.convert(MCXRPlayClient.viewSpacePoses.getUnscaledPhysicalPose().getPos().add(orientation.transform(new Vector3f(0, -0.5f, 1))));
            needsReset = false;
        } else {
            needsReset = true;
        }
    }

    @Nullable
    public Vector3d guiRaycast(Vector3d rayPos, Vector3d rayDir) {
        if (position == null) {
            return null;
        }
        double distance = Intersectiond.intersectRayPlane(
                rayPos,
                rayDir,
                JOMLUtil.convert(position),
                orientation.transform(new Vector3d(0, 0, -1)),
                0.1f
        );
        if (distance >= 0) {
            return rayDir.mul(distance, new Vector3d()).add(rayPos);
        }
        return null;
    }
}
