package org.nka.notchplease.mixin.client.gui.components.toasts;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.toasts.FriendToast;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.nka.notchplease.Notchplease.getScaledNotchHeight;

// 26.3: FriendToast implements Toast directly (it doesn't extend SystemToast), so SystemToastMixin doesn't cover it
@Mixin(value = FriendToast.class)
public class FriendToastMixin {
    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void moveBackground(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) {
            instance.blitSprite(renderPipeline, location, x, y, width, height);
            return;
        }
        instance.blitSprite(renderPipeline, location, x, y + notchHeight, width, height);
    }

    // player head shown for incoming/accepted requests
    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/item/component/ResolvableProfile;III)V"
            )
    )
    private void moveFace(GuiGraphicsExtractor graphics, ResolvableProfile profile, int x, int y, int size) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) {
            PlayerFaceExtractor.extractRenderState(graphics, profile, x, y, size);
            return;
        }
        PlayerFaceExtractor.extractRenderState(graphics, profile, x, y + notchHeight, size);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"
            )
    )
    private void moveTextLines(GuiGraphicsExtractor instance, Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) {
            instance.text(font, str, x, y, color, dropShadow);
            return;
        }
        instance.text(font, str, x, y + notchHeight, color, dropShadow);
    }
}
