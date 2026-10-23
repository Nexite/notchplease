package org.nka.notchplease.mixin.client.gui.screens;

import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static org.nka.notchplease.Notchplease.getScaledNotchHeight;

@Mixin(WorldOptionsScreen.class)
public class WorldOptionsScreenMixin {
    @Shadow
    @Final
    private HeaderAndFooterLayout layout;

    @Shadow
    private @Nullable ScrollableLayout scrollArea;

    // 26.3: the World Options screen is now a scrollable list with an Apply Changes/Cancel footer,
    // so only the title and the scroll area are moved (the footer stays at the bottom)
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onRepositionElements(CallbackInfo ci) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return;
        this.layout.visitChildren((element) -> {
            if (element instanceof StringWidget)
                element.setY(element.getY() + notchHeight);
        });
        if (this.scrollArea != null) {
            this.scrollArea.setMaxHeight(this.layout.getContentHeight() - notchHeight);
            this.scrollArea.setMinHeight(this.layout.getContentHeight() - notchHeight);
            this.scrollArea.arrangeElements();
            this.scrollArea.setY(this.layout.getHeaderHeight() + notchHeight);
        }
    }
}
