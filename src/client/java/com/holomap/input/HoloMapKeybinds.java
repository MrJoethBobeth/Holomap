package com.holomap.input;

import com.holomap.map.MinimapRenderer;
import com.holomap.scan.BlockScanner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HoloMapKeybinds {
    private static KeyBinding toggleMinimap;
    private static KeyBinding rescanMinimap;

    private HoloMapKeybinds() {}

    public static void register() {
        toggleMinimap =
                KeyBindingHelper.registerKeyBinding(
                        new KeyBinding(
                                "key.holomap.toggle",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_M,
                                "key.categories.holomap"));

        rescanMinimap =
                KeyBindingHelper.registerKeyBinding(
                        new KeyBinding(
                                "key.holomap.rescan",
                                InputUtil.Type.KEYSYM,
                                GLFW.GLFW_KEY_N,
                                "key.categories.holomap"));

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    while (toggleMinimap.wasPressed()) {
                        MinimapRenderer.toggleEnabled();
                    }
                    while (rescanMinimap.wasPressed()) {
                        var player = MinecraftClient.getInstance().player;
                        if (player != null) {
                            BlockScanner.scanAroundPlayerAsync(player, 48, 64);
                        }
                    }
                });
    }
}