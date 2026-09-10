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

package com.wildfire.client.cloud;

import com.mojang.authlib.services.MinecraftServicesSessionService;
import com.wildfire.client.ClientHelper;
import com.wildfire.common.WildfireGender;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class CloudUtils {
    private CloudUtils() {
        throw new UnsupportedOperationException();
    }

    private static boolean loggedSessionTamperWarning = false;
    private static final String EXPECTED_YGGDRASIL_BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";

    static boolean hasTheSessionServiceBeenTamperedWith() {
        var sessionService = Minecraft.getInstance().services().sessionService();

        // minecraft normally uses yggdrasil here; if this is not the case, either mojang has made some serious
        // changes to sessions, or someone is replacing this with something that shouldn't be here.
        if(sessionService.getClass() != MinecraftServicesSessionService.class) {
            logSessionTamperWarning("Detected likely session service tampering; got {} instead of the expected Yggdrasil session service", sessionService.getClass());
            return true;
        } else {
            var yggdrasil = (MinecraftServicesSessionService) sessionService;
            // additionally verify for potential cracked client tampering here
            if(!EXPECTED_YGGDRASIL_BASE_URL.equals(ClientHelper.INSTANCE.getSessionUrl(yggdrasil))) {
                logSessionTamperWarning("Detected likely session service tampering; Yggdrasil base URL is not the expected Mojang-provided value");
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private static void logSessionTamperWarning(String message, Object... args) {
        if(loggedSessionTamperWarning) {
            return;
        }
        WildfireGender.LOGGER.warn(message, args);
        WildfireGender.LOGGER.warn("Cloud sync will be unavailable for this session");
        loggedSessionTamperWarning = true;
    }

    public static String buildQuery(@Nullable Map<String, @Nullable Object> query) {
        //~ if >26.2 'HttpAuthenticationService' -> 'HttpDiscoveryService'
        return com.mojang.authlib.HttpDiscoveryService.buildQuery(query);
    }
}
