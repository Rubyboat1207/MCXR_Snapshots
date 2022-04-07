package net.sorenon.mcxr.play;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.sorenon.mcxr.play.openxr.OpenXRInstance;
import net.sorenon.mcxr.play.openxr.OpenXRState;
import net.sorenon.mcxr.play.openxr.OpenXRSystem;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openxr.XR10;

import java.util.List;

public class MCXROptionsScreen extends Screen {

    @Nullable
    private final Screen previous;

    private ButtonWidget reloadButton;

    public MCXROptionsScreen(@Nullable Screen previous) {
        super(new TranslatableText("mcxr.options.title"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        PlayOptions.load();
        this.reloadButton = this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 155,
                this.height / 6 - 12 - 4 + 24,
                150,
                20,
                new TranslatableText("mcxr.menu.reload"),
                button -> MCXRPlayClient.OPEN_XR_STATE.tryInitialize()));
        if (PlayOptions.xrUninitialized) {
            reloadButton.active = false;
        }

        this.addDrawableChild(new ButtonWidget(
                this.width / 2 + 5,
                this.height / 6 - 12 - 4 + 24,
                150,
                20,
                PlayOptions.xrUninitialized ? new TranslatableText("mcxr.options.initialize") : new TranslatableText("mcxr.options.uninitialize"),
                button -> {
                    PlayOptions.xrUninitialized = !PlayOptions.xrUninitialized;
                    PlayOptions.save();
                    reloadButton.active = !PlayOptions.xrUninitialized;
                    if (!PlayOptions.xrUninitialized) {
                        MCXRPlayClient.OPEN_XR_STATE.tryInitialize();
                    }
                    button.setMessage(PlayOptions.xrUninitialized ? new TranslatableText("mcxr.options.initialize") : new TranslatableText("mcxr.options.uninitialize"));
                }));

        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 100,
                this.height / 6 - 12 - 4,
                200,
                20,
                PlayOptions.xrPaused ? new TranslatableText("mcxr.options.unpause") : new TranslatableText("mcxr.options.pause"),
                button -> {
                    PlayOptions.xrPaused = !PlayOptions.xrPaused;
                    PlayOptions.save();
                    button.setMessage(PlayOptions.xrPaused ? new TranslatableText("mcxr.options.unpause") : new TranslatableText("mcxr.options.pause"));
                }));

        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 155,
                this.height / 6 + 54 + 12,
                150,
                20,
                new TranslatableText("mcxr.options.walk_direction", PlayOptions.walkDirection.toComponent()),
                button -> {
                    PlayOptions.walkDirection = PlayOptions.walkDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(new TranslatableText("mcxr.options.walk_direction", PlayOptions.walkDirection.toComponent()));
                }));
        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 155,
                this.height / 6 + 54 + 24 + 12,
                150,
                20,
                new TranslatableText("mcxr.options.swim_direction", PlayOptions.swimDirection.toComponent()),
                button -> {
                    PlayOptions.swimDirection = PlayOptions.swimDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(new TranslatableText("mcxr.options.swim_direction", PlayOptions.swimDirection.toComponent()));
                }));
        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 155,
                this.height / 6 + 54 + 24 * 2 + 12,
                150,
                20,
                new TranslatableText("mcxr.options.fly_direction", PlayOptions.flyDirection.toComponent()),
                button -> {
                    PlayOptions.flyDirection = PlayOptions.flyDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(new TranslatableText("mcxr.options.fly_direction", PlayOptions.flyDirection.toComponent()));
                }));

        assert this.client != null;

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height / 6 + 168, 200, 20, ScreenTexts.DONE, button -> this.client.setScreen(this.previous)));
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);

        drawCenteredText(poseStack, this.textRenderer, this.title, this.width / 2, 13, 16777215);

        int y = this.height / 6 - 4 + 24;
        int x = this.width / 2 - 155;

        MCXROptionsScreen.renderStatus(this, this.textRenderer, poseStack, mouseX, mouseY, x, y, 0, 60);
    }

    public static void renderStatus(Screen screen,
                                    TextRenderer font,
                                    MatrixStack poseStack,
                                    int mouseX,
                                    int mouseY,
                                    int x,
                                    int y,
                                    int fade,
                                    int wrapLength) {
        if (PlayOptions.xrUninitialized) {
            DrawableHelper.drawStringWithShadow(poseStack, font, "MCXR Disabled", x + 1, y + 12, 16777215 | fade);
            return;
        }

        OpenXRState OPEN_XR = MCXRPlayClient.OPEN_XR_STATE;

        if (OPEN_XR.instance != null) {
            OpenXRInstance instance = OPEN_XR.instance;
            DrawableHelper.drawStringWithShadow(poseStack, font, instance.runtimeName + " " + instance.runtimeVersionString, x + 1, y + 12, 16777215 | fade);
            y += 12;
        }

        if (OPEN_XR.session != null) {
            OpenXRSystem system = OPEN_XR.session.system;
            for (String line : wordWrap(system.systemName, wrapLength)) {
                DrawableHelper.drawStringWithShadow(poseStack, font, line, x + 1, y + 12, 16777215 | fade);
                y += 12;
            }
            DrawableHelper.drawStringWithShadow(poseStack, font, I18n.translate("openxr.form_factor." + system.formFactor), x + 1, y + 12, 16777215 | fade);
        } else {
            DrawableHelper.drawStringWithShadow(poseStack, font, I18n.translate("mcxr.menu.session_not_created"), x + 1, y + 12, 16777215 | fade);
            y += 12;
            if (OPEN_XR.createException != null) {
                String message = OPEN_XR.createException.getMessage();
                if (OPEN_XR.createException.result == XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE) {
                    message = I18n.translate("mcxr.error.form_factor_unavailable");
                }
                for (String line : wordWrap(message, wrapLength)) {
                    DrawableHelper.drawStringWithShadow(poseStack, MinecraftClient.getInstance().textRenderer, line, x + 1, y + 12, 16733525 | fade);
                    y += 12;
                }
                if (mouseX > x && mouseY < y + 10 && mouseY > screen.height / 4 + 48 + 12 + 10) {
                    screen.renderTooltip(poseStack, wordWrapText(message, 40), mouseX + 14, mouseY);
                }
            }
        }
    }

    private static List<String> wordWrap(String string, int wrapLength) {
        return WordUtils.wrap(string, wrapLength, null, true).lines().toList();
    }

    private static List<Text> wordWrapText(String string, int wrapLength) {
        return WordUtils.wrap(string, wrapLength, null, true).lines().map(s -> (Text) (new LiteralText(s))).toList();
    }
}
