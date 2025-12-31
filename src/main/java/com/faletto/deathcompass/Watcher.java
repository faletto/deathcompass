package com.faletto.deathcompass;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class Watcher {
    public static boolean wasAlive = true;

    public static void register() {

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
    }
}
