package org.nka.notchplease.mixin.client.gui.screens.friends;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.nka.notchplease.Notchplease.getScaledNotchHeight;

// 26.3: the friends overlay is a panel centered on screen with its tab bar 27px above it,
// and the panel is (height - 80) tall, so the tab bar ends up at y=13 (under the notch).
// It uses a plain TabNavigationBar (not MenuTabBar), so MenuTabBarMixin doesn't touch it.
@Mixin(FriendsOverlayScreen.class)
public class FriendsOverlayScreenMixin {
    // shrink both tabs by the notch height so the panel still fits once it's pushed down
    @ModifyArg(
            method = "repositionElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/friends/FriendsTab;setHeight(I)V"
            ),
            index = 0
    )
    private int shrinkFriendsTab(int height) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return height;
        return height - notchHeight;
    }

    @ModifyArg(
            method = "repositionElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/friends/PendingTab;setHeight(I)V"
            ),
            index = 0
    )
    private int shrinkPendingTab(int height) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return height;
        return height - notchHeight;
    }

    // center the panel in the area below the notch instead of the whole screen.
    // refreshLists + populateLists re-center the panel too, so they need the same rectangle.
    // the tab bar, background sprite and click bounds are all derived from the panel's position, so they follow it.
    @ModifyArg(
            method = {"repositionElements", "refreshLists", "populateLists"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/FrameLayout;alignInRectangle(Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/client/gui/navigation/ScreenRectangle;FF)V"
            ),
            index = 1
    )
    private ScreenRectangle modifyAlignRectangle(ScreenRectangle rectangle) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return rectangle;
        return new ScreenRectangle(rectangle.left(), rectangle.top() + notchHeight, rectangle.width(), rectangle.height() - notchHeight);
    }
}
