package net.sorenon.mcxr.play.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.sorenon.mcxr.play.MCXROptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Text component) {
        super(component);
    }

    @Inject(method = "init", at = @At("HEAD"))
    void init(CallbackInfo ci) {
        int y = this.height / 4 + 48 + -16;
        this.addDrawableChild(new ButtonWidget(
                this.width / 2 + 104,
                y,
                90,
                20,
                new TranslatableText("mcxr.options.title"),
                button -> this.client.setScreen(new MCXROptionsScreen(this))));
    }

    @Inject(method = "render", at = @At("RETURN"))
    void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int y = this.height / 4 + 48 + -16 + 12;
        int x = this.width / 2 + 104;

        MCXROptionsScreen.renderStatus(this, this.textRenderer, matrices, mouseX, mouseY, x, y, 0, 20);
    }
}
