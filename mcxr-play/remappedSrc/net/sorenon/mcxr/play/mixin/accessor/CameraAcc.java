package net.sorenon.mcxr.play.mixin.accessor;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAcc {

    @Accessor("initialized")
    void ready(boolean ready);

    @Accessor("level")
    void area(BlockView area);

    @Accessor("entity")
    void focusedEntity(Entity entity);

    @Accessor("detached")
    void thirdPerson(boolean thirdPerson);

    @Accessor("xRot")
    void pitch(float pitch);

    @Accessor("yRot")
    void yaw(float yaw);

    @Accessor("left")
    Vec3f diagonalPlane();
}
