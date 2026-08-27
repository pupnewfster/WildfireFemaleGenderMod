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

import com.wildfire.common.WildfireGender;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import java.util.Map;

public final class CloudUtils {
    private CloudUtils() {
        throw new UnsupportedOperationException();
    }

    private static boolean loggedSessionTamperWarning = false;
    //? if <=26.2
    private static final String EXPECTED_YGGDRASIL_BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";

    static boolean hasTheSessionServiceBeenTamperedWith() {
        var sessionService = Minecraft.getInstance().services().sessionService();

        // minecraft normally uses yggdrasil here; if this is not the case, either mojang has made some serious
        // changes to sessions, or someone is replacing this with something that shouldn't be here.
        //~ if >26.2 'yggdrasil.YggdrasilMinecraftSessionService' -> 'services.MinecraftServicesSessionService'
        if(sessionService.getClass() != com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService.class) {
            logSessionTamperWarning("Detected likely session service tampering; got {} instead of the expected Yggdrasil session service", sessionService.getClass());
            return true;
        } else {
            // TODO is it possible to fix this for 26.3?
            //? if <=26.2 {
            var yggdrasil = (com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService) sessionService;
            // additionally verify for potential cracked client tampering here
            if(!com.wildfire.client.ClientHelper.INSTANCE.validateSessionUrl(yggdrasil, EXPECTED_YGGDRASIL_BASE_URL)) {
                logSessionTamperWarning("Detected likely session service tampering; Yggdrasil base URL is not the expected Mojang-provided value");
                return true;
            }
            //?}
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
        return com.mojang.authlib.HttpAuthenticationService.buildQuery(query);
    }
}
