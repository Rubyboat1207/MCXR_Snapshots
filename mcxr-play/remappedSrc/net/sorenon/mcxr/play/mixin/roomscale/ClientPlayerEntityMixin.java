package net.sorenon.mcxr.play.mixin.roomscale;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sorenon.mcxr.core.MCXRCore;
import net.sorenon.mcxr.core.MCXRScale;
import net.sorenon.mcxr.play.MCXRPlayClient;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    public Input input;

    @Shadow public abstract void move(MovementType movementType, Vec3d movement);

    @Unique
    private static final Input sneakingInput = new Input();

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    /**
     * Try to move the player to the position of the headset
     * We can do this entirely on the client because minecraft's anti cheat is non-existent
     * TODO handle teleportation here for vanilla servers
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V"))
    void applyRoomscaleMovement(CallbackInfo ci) {
        if (!MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode()) {
            return;
        }

        Vector3d playerPhysicalPosition = MCXRPlayClient.playerPhysicalPosition;
        if (MCXRCore.getCoreConfig().roomscaleMovement() && !this.hasVehicle()) {
            //Get the user's head's position in physical space
            Vector3f viewPos = MCXRPlayClient.viewSpacePoses.getPhysicalPose().getPos();

            //Store the player entity's position
            double oldX = this.getX();
            double oldZ = this.getZ();

            //Force the player to sneak to they don't accidentally fall
            //This is because the player entity is under the user's head, not their body, so they will full if they just look over
            //TODO improve this so that if the player entity is a certain distance over a gap they fall anyway
            boolean onGround = this.onGround;
            Input input = this.input;
            this.input = sneakingInput;
            sneakingInput.sneaking = true;

            //We want to move the player entity to the user's position in physical space
            Vec3d wantedMovement = new Vec3d(viewPos.x - playerPhysicalPosition.x, 0, viewPos.z - playerPhysicalPosition.z);

            //Counter out the mixin pehuki uses for scaling movement
            float invScale = 1.0f / MCXRScale.getMotionScale(this);

            this.move(MovementType.SELF, wantedMovement.multiply(invScale));
            this.input = input;
            this.onGround = onGround;

            double deltaX = this.getX() - oldX;
            double deltaZ = this.getZ() - oldZ;

            //Store the position of the player in physical space
            playerPhysicalPosition.x += deltaX;
            playerPhysicalPosition.z += deltaZ;

            this.prevX += deltaX;
            this.prevZ += deltaZ;
        } else {
            playerPhysicalPosition.zero();
        }
    }

    @Inject(method = "startRiding", at = @At("RETURN"))
    void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            MCXRPlayClient.resetView();
        }
    }
}
