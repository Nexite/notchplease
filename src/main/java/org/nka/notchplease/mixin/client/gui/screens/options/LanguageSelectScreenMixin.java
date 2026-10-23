package org.nka.notchplease.mixin.client.gui.screens.options;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nka.notchplease.Notchplease.getScaledNotchHeight;

// OptionsSubScreenMixin also runs here (via super.repositionElements), but it only shifts direct StringWidget
// children + OptionsList, and this screen's header is a LinearLayout and its list is an ObjectSelectionList,
// so nothing gets shifted twice.
// The header (title + search box) is taller than the regular one, so vanilla centers it higher (title at y=4
// instead of 12). Instead of moving each element, the header is made taller so the list follows it.
@Mixin(LanguageSelectScreen.class)
public class LanguageSelectScreenMixin extends OptionsSubScreenMixin {
    @Shadow @Nullable
    private EditBox search;

    @Unique
    private int vanillaHeaderHeight = -1;

    // start every reposition from vanilla's header height, so toggling fullscreen doesn't stack the offset
    @Inject(method = "repositionElements", at = @At("HEAD"))
    private void resetHeaderHeight(CallbackInfo ci) {
        if (this.vanillaHeaderHeight == -1) this.vanillaHeaderHeight = this.layout.getHeaderHeight();
        else this.layout.setHeaderHeight(this.vanillaHeaderHeight);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onRepositionElements(CallbackInfo ci) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return;
        LinearLayout header = null;
        ObjectSelectionList<?> langList = null;
        for (var element : this.getHeaderAndList()) {
            if (element instanceof LinearLayout layout) header = layout;
            if (element instanceof ObjectSelectionList<?> list) langList = list;
        }
        if (header == null || langList == null) return;

        // where the title sits on the regular settings screens: centered in the default header, then shifted
        // down by OptionsSubScreenMixin
        int regularTitleY = Math.round((HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT - Minecraft.getInstance().font.lineHeight) / 2.0F);
        int titleY = regularTitleY + notchHeight;

        // grow the header by however far the title moves, keeping vanilla's gap between the search box and the list.
        // the list takes its top + height from the header height, so its bottom and the footer don't move
        int delta = titleY - header.getY();
        this.layout.setHeaderHeight(this.vanillaHeaderHeight + delta);
        this.layout.arrangeElements();
        // the header frame centers its child, so pin the title + search box to the title's y
        header.setY(titleY);
        langList.updateSize(((Screen) (Object) this).width, this.layout);
    }

    // the header LinearLayout that holds the search box, and the language list
    @Unique
    private java.util.List<Object> getHeaderAndList() {
        java.util.List<Object> found = new java.util.ArrayList<>();
        this.layout.visitChildren((element) -> {
            if (element instanceof LinearLayout layout) {
                boolean[] isHeader = {false};
                layout.visitChildren((child) -> {
                    if (child == this.search) isHeader[0] = true;
                });
                if (isHeader[0]) found.add(layout);
            }
            if (element instanceof ObjectSelectionList<?>) found.add(element);
        });
        return found;
    }
}
