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

package com.wildfire.fabric.client.mixins.accessors;

import org.spongepowered.asm.mixin.Mixin;

/// @apiNote Only applied on the client side
//~ if >=26.3-pre-2 'com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService' -> 'com.wildfire.common.util.NoopMixinTarget'
@Mixin(com.wildfire.common.util.NoopMixinTarget.class)
public interface YggdrasilMinecraftSessionServiceAccessor {
    //? if <=26.2
    //@org.spongepowered.asm.mixin.gen.Accessor String getBaseUrl();
}
