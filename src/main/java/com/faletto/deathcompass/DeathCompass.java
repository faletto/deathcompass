package com.faletto.deathcompass;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class DeathCompass implements ModInitializer {
	public static final String MOD_ID = "death-compass";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.error("LOADED DEATH COMPASS");
		CommandRegistrationCallback.EVENT.register((dispatcher, b, c) -> dispatcher.register(
			LiteralArgumentBuilder.<CommandSourceStack>literal("dismiss")
			.executes((context) -> {
				DeathCompassClient.dismiss();
				return 0;
			})));

		CommandRegistrationCallback.EVENT.register((dispatcher, b, c) -> dispatcher.register(
			LiteralArgumentBuilder.<CommandSourceStack>literal("lastdeath")
			.executes((context) -> {
				DeathCompassClient.getLastDeath();
				return 0;
			})));
		    
	}
}