package org.nka.notchplease;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.Layout;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;

public class Notchplease implements ClientModInitializer {
    static String gpuName;
    static boolean isAppleSilicon = false;
    static boolean isMacOS = false;

    // Initializing isMacOS - checking if system is on macOS or not
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            System.out.println("Notch, Please! macOS detected.");
            isMacOS = true;
        } else {
            System.out.println("Notch, Please! Non-macOS system detected. No changes will be made.");
        }
    }

    public static ArrayList<Integer> resolutionHeights = new ArrayList<>();

    // Shifts StringWidgets (titles) that sit inside NESTED layouts. Direct children are skipped
    // here because callers already handle those.
    public static void shiftNestedStringWidgets(Layout layout, int dy, boolean skipDirect) {
        layout.visitChildren(element -> {
            if (element instanceof Layout nested) {
                shiftNestedStringWidgets(nested, dy, false);
            } else if (!skipDirect && element instanceof StringWidget) {
                element.setY(element.getY() + dy);
            }
        });
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (isMacOS) {
                // 26.3: the renderer can be OpenGL or Vulkan, so ask the active GPU device instead of GL
                gpuName = RenderSystem.getDevice().getDeviceInfo().name();
                isAppleSilicon = gpuName != null && gpuName.contains("Apple");
            }
        });
    }

    public static boolean listAvailableResolutions() {
        if (!isMacOS || !isAppleSilicon) return false;
        resolutionHeights = new ArrayList<>();
        // 26.3: Minecraft moved from GLFW to SDL3, so the monitor + video modes come from Minecraft's own Monitor wrapper
        Monitor monitor = Minecraft.getInstance().getWindow().findBestMonitor();

        if (monitor == null) {
//            System.out.println("Notch, Please! No monitor found.");
            return false;
        }

        if (!isBuiltinDisplay(monitor)) {
//            System.out.println("Notch, Please! This is not the built-in monitor.");
            return false;
        }

        if (monitor.videoModes().isEmpty()) {
//            System.out.println("Notch, Please! No video modes found.");
            return false;
        }

        for (VideoMode mode : monitor.videoModes()) {
            resolutionHeights.add(mode.getHeight());
        }
        return !resolutionHeights.isEmpty();
    }

    @Unique
    public static int getScaledNotchHeight() {
        if (!isMacOS || !isAppleSilicon || !listAvailableResolutions()) return -1;
        Window window = Minecraft.getInstance().getWindow();
        // 26.3: SDL3 video modes are in points while the framebuffer is in pixels (HiDPI), so compare in points
        int currentHeight = window.getScreenHeight();
        // exclusive fullscreen is the only mode that switches the display to a resolution that includes the notch
        boolean isFullScreen = window.isExclusiveFullscreen();
        double currentScale = window.getGuiScale() * window.getScreenHeight() / (double) window.getHeight();

        // if resolution is not set to show notch
        if (currentHeight % 10 == 0 || resolutionHeights == null || !isFullScreen || !isAppleSilicon) return -1;

        int rawNotchHeight = getRawNotchHeight(currentHeight);

        // check to see if height diff = typical notch height in px, not something crazy
        if (rawNotchHeight > 200 || rawNotchHeight < 0) {
//            System.out.println("Notch, Please! There was an error calculating the notch height. Does your current display have a notch?");
            return -1;
        }
        return (int) (rawNotchHeight / currentScale);
    }

    private static int getRawNotchHeight(int currentHeight) {
        int closestNonNotchedHeight = Integer.MIN_VALUE;
        int closestHeightDiff = Integer.MAX_VALUE;

        int currentHeightDiff;
        for (Integer height : resolutionHeights) {
            currentHeightDiff = Math.abs(currentHeight - height);
            if (height < currentHeight && currentHeightDiff < closestHeightDiff) {
                closestHeightDiff = currentHeightDiff;
                closestNonNotchedHeight = height;
            }
        }

        return currentHeight - closestNonNotchedHeight;
    }

    // SDL3 display IDs are not CGDirectDisplayIDs, so find the CoreGraphics display with the same bounds
    // (SDL's Cocoa backend gets its display bounds from CGDisplayBounds, so these match exactly)
    private static boolean isBuiltinDisplay(Monitor monitor) {
        int[] displays = new int[16];
        IntByReference displayCount = new IntByReference();
        if (CoreGraphics.INSTANCE.CGGetActiveDisplayList(displays.length, displays, displayCount) != 0) return false;

        for (int i = 0; i < displayCount.getValue(); i++) {
            CoreGraphics.CGRect bounds = CoreGraphics.INSTANCE.CGDisplayBounds(displays[i]);
            if ((int) bounds.x == monitor.x() && (int) bounds.y == monitor.y()
                    && (int) bounds.width == monitor.w() && (int) bounds.height == monitor.h()) {
                return CoreGraphics.INSTANCE.CGDisplayIsBuiltin(displays[i]) != 0;
            }
        }
        return false;
    }

    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        // for future ref, this is linking to a pre-existing C function in CoreGraphics:
        // int CGDisplayIsBuiltin(CGDirectDisplayID display);
        // this returns a 1 if displayID = built-in displayID, 0 otherwise
        // this presumably needs to be an interface bcz you can't override/overload native code
        int CGDisplayIsBuiltin(int displayID);

        // CGError CGGetActiveDisplayList(uint32_t maxDisplays, CGDirectDisplayID *activeDisplays, uint32_t *displayCount);
        int CGGetActiveDisplayList(int maxDisplays, int[] activeDisplays, IntByReference displayCount);

        // CGRect CGDisplayBounds(CGDirectDisplayID display);
        CGRect.ByValue CGDisplayBounds(int displayID);

        // CGRect is { CGPoint origin; CGSize size; }, flattened here since both are just two doubles
        @Structure.FieldOrder({"x", "y", "width", "height"})
        class CGRect extends Structure {
            public double x;
            public double y;
            public double width;
            public double height;

            public static class ByValue extends CGRect implements Structure.ByValue {
            }
        }
    }

/*
    interface ObjCRuntime extends Library {
        ObjCRuntime INSTANCE = Native.load("objc", ObjCRuntime.class

        // This is also the same, linking native C functions to Java here
        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);
        long objc_msgSend(Pointer receiver, Pointer selector);
    }

    public static boolean isNativeFullscreen() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return false;

        long windowPtr = Minecraft.getInstance().getWindow().getHandle();
        long nsWindowPtr = GLFWNativeCocoa.glfwGetCocoaWindow(windowPtr);
        if (nsWindowPtr == 0) return false;

        Pointer nsWindow = new Pointer(nsWindowPtr);
        Pointer selStyleMask = ObjCRuntime.INSTANCE.sel_registerName("styleMask");
        long styleMask = ObjCRuntime.INSTANCE.objc_msgSend(nsWindow, selStyleMask);

        final long NSWindowStyleMaskFullScreen = 1L << 14;
        return (styleMask & NSWindowStyleMaskFullScreen) != 0;
    }

    public static void toggleNativeFullscreen() {
        long windowPtr = Minecraft.getInstance().getWindow().getHandle();
        long nsWindowPtr = GLFWNativeCocoa.glfwGetCocoaWindow(windowPtr);
        Pointer nsWindow = new Pointer(nsWindowPtr);
        Pointer selToggleFullscreen = ObjCRuntime.INSTANCE.sel_registerName("toggleFullScreen:");
        ObjCRuntime.INSTANCE.objc_msgSend(nsWindow, selToggleFullscreen);
    }

    // since minecraft by default on macOS does not enable Minecraft's built-in fullscreen functionality
    public static void triggerActualFullscreen() {
        boolean isFullScreen = Minecraft.getInstance().getWindow().isFullscreen();
        if (!isFullScreen) {
            if (isNativeFullscreen()) toggleNativeFullscreen();
            Minecraft.getInstance().getWindow().toggleFullscreen();
        } else {
            Minecraft.getInstance().getWindow().toggleFullscreen();
            if (!isNativeFullscreen()) toggleNativeFullscreen();
        }
    }
*/
}
