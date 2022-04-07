package net.sorenon.mcxr.play.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.sorenon.mcxr.play.MCXROptionsScreen;
import net.sorenon.mcxr.play.MCXRPlayClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    protected abstract void init();

    @Shadow
    @Final
    private boolean fading;
    @Shadow
    private long fadeInStart;
    @Unique
    private static boolean initialized = false;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    void init(CallbackInfo ci) {
        int y = this.height / 4 + 48;
        this.addDrawableChild(new ButtonWidget(
                this.width / 2 + 104,
                y,
                90,
                20,
                new TranslatableText("mcxr.options.title"),
                button -> this.client.setScreen(new MCXROptionsScreen(this))));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!initialized) {
            if (MCXRPlayClient.OPEN_XR_STATE.session == null) {
                MCXRPlayClient.OPEN_XR_STATE.tryInitialize();
            }
            initialized = true;
        }
        float f = this.fading ? (float) (Util.getMeasuringTimeMs() - this.fadeInStart) / 1000.0F : 1.0F;
        float g = this.fading ? MathHelper.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
        int l = MathHelper.ceil(g * 255.0F) << 24;
        int y = this.height / 4 + 48;
        int x = this.width / 2 + 104;

        if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
            y += 12;
        }

        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            DrawableHelper.drawStringWithShadow(matrices, textRenderer, "Sodium Missing!", x + 1, y + 12, 16733525 | l);
            y += 12;
        }

        MCXROptionsScreen.renderStatus(this, this.textRenderer, matrices, mouseX, mouseY, x, y, l, 20);
    }
}
