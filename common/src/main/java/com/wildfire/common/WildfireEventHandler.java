/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wildfire.common;

import com.wildfire.common.entitydata.PlayerConfigHolder;
import com.wildfire.common.networking.WildfireSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class WildfireEventHandler {
    private WildfireEventHandler() {
        throw new UnsupportedOperationException();
    }

    /// Removes a disconnecting player from the cache on a server
    public static void playerDisconnected(Player player) {
        WildfireGender.CACHE.invalidate(player.getUUID());
    }

    /// Send a sync packet when a player enters the render distance of another player
    public static void onBeginTracking(Entity tracked, ServerPlayer syncTo) {
        if(tracked instanceof Player toSync) {
            PlayerConfigHolder genderToSync = WildfireGender.getPlayerById(toSync.getUUID());
            if(genderToSync == null) return;
            // Note that we intentionally don't check if we've previously synced a player with this code path;
            // because we use entity tracking to sync, it's entirely possible that one player would leave the
            // tracking distance of another, change their settings, and then re-enter their tracking distance;
            // we wouldn't sync while they're out of tracking distance, and as such, their settings would be out
            // of sync until they relog.
            WildfireSync.sendToClient(syncTo, genderToSync);
        }
    }
}
