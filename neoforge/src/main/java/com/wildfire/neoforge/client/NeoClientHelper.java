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

package com.wildfire.neoforge.client;

import com.mojang.authlib.services.MinecraftServicesSessionService;
import com.wildfire.api.WildfireAPI;
import com.wildfire.client.ClientHelper;
import com.wildfire.common.WildfireGender;
import com.wildfire.client.render.GenderRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.context.ContextKey;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public class NeoClientHelper implements ClientHelper {

    public static final ContextKey<GenderRenderState> STATE = new ContextKey<>(WildfireGender.id("gender_state"));
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, WildfireAPI.MODID);

    private static final DeferredHolder<SoundEvent, SoundEvent> FEMALE_HURT = SOUND_EVENTS.register("female_hurt", SoundEvent::createVariableRangeEvent);

    @Override
    public Holder<SoundEvent> femaleHurt() {
        return FEMALE_HURT;
    }

    @Nullable
    @Override
    public GenderRenderState getRenderState(HumanoidRenderState state) {
        return state.getRenderData(STATE);
    }

    @Override
    public String getSessionUrl(final MinecraftServicesSessionService service) {
        String baseUrl;
        //Note: We need to use reflection here as Neo protects certain packages from coremods
        //? if <=26.2 {
        /*baseUrl = ObfuscationReflectionHelper.getPrivateValue(
            MinecraftServicesSessionService.class,
            service,
            "baseUrl"
        );
        *///? } else {
        baseUrl = ((com.mojang.authlib.services.MinecraftServicesDiscoveryService) ObfuscationReflectionHelper.getPrivateValue(
            MinecraftServicesSessionService.class,
            service,
            "discoveryService"
        )).getUrl(com.mojang.authlib.services.response.discovery.Service.SESSION, "join");
        if (baseUrl.endsWith("join")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
        }
        //? }
        return baseUrl;
    }
}
