package com.holomap;

import com.holomap.HoloMapMod;
import com.holomap.input.HoloMapKeybinds;
import com.holomap.map.MinimapRenderer;
import net.fabricmc.api.ClientModInitializer;

public class HoloMapClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HoloMapMod.LOGGER.info("[HoloMap] Initializing client module");
		HoloMapKeybinds.register();
		MinimapRenderer.registerHudRender();
	}
}