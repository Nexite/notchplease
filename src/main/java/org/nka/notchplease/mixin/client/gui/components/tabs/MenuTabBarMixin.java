package org.nka.notchplease.mixin.client.gui.components.tabs;

import net.minecraft.client.gui.components.tabs.MenuTabBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static org.nka.notchplease.Notchplease.getScaledNotchHeight;

// 26.3: the top-of-screen tab bar (Create World, Statistics, Realms config) is now MenuTabBar,
// which positions its layout itself instead of TabNavigationBar.arrangeElements doing it
@Mixin(MenuTabBar.class)
public class MenuTabBarMixin {
    @ModifyArg(
            method = "arrangeElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/FrameLayout;setY(I)V"
            ),
            index = 0
    )
    private int modifyElementsY(int originalY) {
        int notchHeight = getScaledNotchHeight();
        if (notchHeight == -1) return originalY;
        return originalY + notchHeight;
    }
}
