package com.faletto.deathcompass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.Provider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;

public class DeathCompassClient implements ClientModInitializer {
    public static final String MOD_ID = "death-compass";
    static double TIMER = 0;
    // Pessimist timer is used in multiplayer servers
    // It assumes the worst case scenario since we can't know for sure if a chunk is
    // unloaded
    static double PESSIMIST_TIMER = 0;
    static long startTime;
    static String serverID;
    static double x = Double.NaN;
    static double y = Double.NaN;
    static double z = Double.NaN;
    static int color = ARGB.color(255, 255, 255);
    public static boolean wasAlive = true;
    static Minecraft mc = Minecraft.getInstance();


    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Jarvis, activate goon mode");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            boolean isAlive = client.player.isAlive();
            // Ensures compass only starts one time
            if (wasAlive && !isAlive) {
                DeathCompassClient.startTimer();
            }

            wasAlive = isAlive;

        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, b) -> dispatcher.register(
			ClientCommandManager.literal("dismiss")
			.executes((context) -> {
				DeathCompassClient.dismiss();
				return 0;
			})));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, b) -> dispatcher.register(
			ClientCommandManager.literal("lastdeath")
			.executes((context) -> {
				DeathCompassClient.getLastDeath();
				return 0;
			})));
		    

        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
                ResourceLocation.fromNamespaceAndPath(DeathCompassClient.MOD_ID, "deathcompass"),
                DeathCompassClient::timerLoop);

    }

    public static void startTimer() {
        TIMER = 300;
        PESSIMIST_TIMER = TIMER;
        serverID = getServerID();
        x = mc.player.getX();
        y = mc.player.getY();
        z = mc.player.getZ();
    }

    public static void dismiss() {
        TIMER = 0;
    }

    public static void timerLoop(GuiGraphics context, DeltaTracker tickCount) {
        
        // Checks if player has joined a different server
        if (!getServerID().equals(serverID)) {
            serverID = getServerID();
            TIMER = 0;
            x = Double.NaN;
            y = Double.NaN;
            z = Double.NaN;
            return;
        }

        if (TIMER <= 0 || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return;
        }


        double distance = Math.sqrt(
                Math.pow(mc.player.getX() - x, 2) +
                        Math.pow(mc.player.getY() - y, 2) +
                        Math.pow(mc.player.getZ() - z, 2));

        // Checks if player has reached their death location while alive
        if ((distance < 2 && mc.player.isAlive())) {
            // chat("MISMATCH");
            // chat("Saved server id: " + serverID);
            // chat("Current server id: " + getServerID());
            TIMER = 0;
        }

        if (chunkLoaded()) {
            TIMER -= (tickCount.getRealtimeDeltaTicks()) / 20;
        }

        PESSIMIST_TIMER -= (tickCount.getRealtimeDeltaTicks()) / 20;
        if (PESSIMIST_TIMER < 0) {
            PESSIMIST_TIMER = 0;
        }

        
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        int width = screenWidth - 100;

        context.drawString(mc.font, getClockString(TIMER), width, screenHeight - 110, color, true);
        if (!mc.isSingleplayer()) {
            context.drawString(mc.font, "Worst Case: " + getClockString(PESSIMIST_TIMER), width, screenHeight - 100, color, true);
        }
        context.drawString(mc.font, "You died at:", width, screenHeight - 90, color, true);
        context.drawString(mc.font, (int) x + " " + (int) y + " " + (int) z, width, screenHeight - 80, color, true);
        context.drawString(mc.font, "Distance: " + (int) distance + "m", width, screenHeight - 70, color, true);
        context.drawString(mc.font, "[x] /dismiss ", width, screenHeight - 60, color, true);

    }

    public static boolean chunkLoaded() {
        if (mc.isPaused()) {
            return false;
        }
        LevelChunk chunk = mc.level.getChunkAt(new BlockPos((int) x, (int) y, (int) z));
        return chunk != null && !chunk.isEmpty();
    }

    public static String getClockString(double time) {
        String minute = "" + (int) time % 60;
        minute = time % 60 < 10 ? "0" + minute : minute;

        return ((int) time / 60) + ":" + minute;
    }

    // Gets a unique identifier for the server
    // If singleplayer, uses world folder path
    // If multiplayer, uses server IP
    public static String getServerID() {
        if (mc.isSingleplayer()) {
            return mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toString();
        } else {
            return mc.getCurrentServer().ip;
        }
    }

    public static void chat(String s) {
        mc.gui.getChat().addMessage(Component.literal(s));
    }

    public static void getLastDeath() {
        if (!(Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z))) {
            chat("You died at: " + (int) x + " " + (int) y + " " + (int) z);
        } else {
            chat("No death yet");
        }
    }
}
