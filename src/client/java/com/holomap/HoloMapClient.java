package com.holomap;

import com.holomap.input.HoloMapKeybinds;
import com.holomap.render.MinimapRenderer3D;
import net.fabricmc.api.ClientModInitializer;

public class HoloMapClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HoloMapKeybinds.register(); // if it triggers rescan, point it to BlockScanner3D
		MinimapRenderer3D.registerHudRender();
	}
}