package net.sorenon.mcxr.play.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.entity.vehicle.BoatEntity;
import net.sorenon.mcxr.play.MCXRPlayClient;
import net.sorenon.mcxr.play.input.XrInput;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO have a look at https://github.com/LambdAurora/LambdaControls compat
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("RETURN"))
    void overwriteMovement(boolean slowDown, float f, CallbackInfo ci) {
        if (!MCXRPlayClient.MCXR_GAME_RENDERER.isXrMode()) return;
        if (MCXRPlayClient.INSTANCE.MCXRGuiManager.isScreenOpen()) return;

        var move = XrInput.vanillaGameplayActionSet.move.currentState;
        this.movementForward = move.y();
        this.movementSideways = -move.x();

        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getVehicle() instanceof BoatEntity) {
            this.pressingForward |= move.y() > 0.5;
            this.pressingBack |= move.y() < -0.5;
            this.pressingRight |= move.x() > 0.5;
            this.pressingLeft |= move.x() < -0.5;
        } else {
            this.pressingForward |= move.y() > 0;
            this.pressingBack |= move.y() < 0;
            this.pressingRight |= move.x() > 0;
            this.pressingLeft |= move.x() < 0;
        }

        this.jumping |= XrInput.vanillaGameplayActionSet.jump.currentState;
        
        if(slowDown) {
            this.movementForward = move.y() * 0.3f;
            this.movementSideways = -move.x() * 0.3f;
        }
    }
}
