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

package com.wildfire.fabric.client;

import com.wildfire.client.ClientHelper;
import com.wildfire.common.WildfireGender;
import com.wildfire.client.render.GenderRenderState;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

public class FabricClientHelper implements ClientHelper {

    public static final RenderStateDataKey<GenderRenderState> STATE = RenderStateDataKey.create(() -> "GenderRenderState");
    @Nullable
    private static Holder<SoundEvent> FEMALE_HURT;

    public static void registerSounds() {
        SoundEvent femaleHurt = SoundEvent.createVariableRangeEvent(WildfireGender.id("female_hurt"));
        FEMALE_HURT = Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, femaleHurt.location(), femaleHurt);
    }

    @Override
    public Holder<SoundEvent> femaleHurt() {
        return Objects.requireNonNull(FEMALE_HURT, "Hurt sound not registered yet");
    }

    @Override
    public @Nullable GenderRenderState getRenderState(HumanoidRenderState state) {
        return state.getData(STATE);
    }

    //? if <=26.2 {
    @Override
    public boolean validateSessionUrl(final com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService service, String expected) {
        var accessor = (com.wildfire.fabric.client.mixins.accessors.YggdrasilMinecraftSessionServiceAccessor) service;
        return Objects.equals(accessor.getBaseUrl(), expected);
    }
    //?}
}
