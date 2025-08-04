package com.holomap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HoloMapMod implements ModInitializer {
	public static final String MOD_ID = "holomap";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[HoloMap] Initializing common module");
	}
}