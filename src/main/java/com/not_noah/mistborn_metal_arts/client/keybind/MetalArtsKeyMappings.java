package com.not_noah.mistborn_metal_arts.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class MetalArtsKeyMappings {
    public static final String CATEGORY = "key.categories.mistborn_metal_arts";

    public static final KeyMapping OPEN_MENU = key("open_menu", GLFW.GLFW_KEY_GRAVE_ACCENT); // Tilde menu
    public static final KeyMapping CYCLE_SELECTED = key("cycle_selected", GLFW.GLFW_KEY_Z); // Cycle metals
    public static final KeyMapping BURN_SELECTED = key("burn_selected", GLFW.GLFW_KEY_X);   // Toggle burning
    public static final KeyMapping FLARE_SELECTED = key("flare_selected", GLFW.GLFW_KEY_C); // Toggle flaring
    public static final KeyMapping STOP_BURNING = key("stop_burning", GLFW.GLFW_KEY_V);     // Emergency stop all
    public static final KeyMapping ALLOMANCY_PUSH = key("push", GLFW.GLFW_KEY_R);           // Steel Pushing
    public static final KeyMapping ALLOMANCY_PULL = key("pull", GLFW.GLFW_KEY_F);           // Iron Pulling
    public static final KeyMapping TIME_BUBBLE = key("time_bubble", GLFW.GLFW_KEY_G);       // Drop temporal bubble
    public static final KeyMapping TOGGLE_FERUCHEMY = key("toggle_feruchemy", GLFW.GLFW_KEY_K); // Store/Tap Feruchemy
    public static final KeyMapping ALUMINUM_PURGE = key("aluminum_purge", GLFW.GLFW_KEY_P); // Safe, non-fat-finger purge


    private MetalArtsKeyMappings() {
    }

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.mistborn_metal_arts." + name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }
}
