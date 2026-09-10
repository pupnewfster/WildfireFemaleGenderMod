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

package com.wildfire.client.resources;

import com.wildfire.api.IGenderArmor;
import com.wildfire.api.data.GenderArmorProvider;
import com.wildfire.common.WildfireGender;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/// @apiNote Only use this on the client side
public final class GenderArmorResourceManager extends SimpleJsonResourceReloadListener<IGenderArmor> {

    public static final Identifier ID = WildfireGender.id("armor_data");
    public static final GenderArmorResourceManager INSTANCE = new GenderArmorResourceManager();
    @Deprecated(forRemoval = true)
    private static final GenderArmorResourceManager LEGACY = new GenderArmorResourceManager("wildfire_gender_data", true);

    @Deprecated(forRemoval = true)
    private final boolean isLegacy;
    private @Unmodifiable Map<Identifier, IGenderArmor> configs = Map.of();

    private GenderArmorResourceManager() {
        this(GenderArmorProvider.PREFIX, false);
    }

    private GenderArmorResourceManager(String path, boolean isLegacy) {
        this.isLegacy = isLegacy;
        super(IGenderArmor.CODEC, FileToIdConverter.json(path));
    }

    @Override
    protected Map<Identifier, IGenderArmor> prepare(ResourceManager manager, ProfilerFiller profiler) {
        if (isLegacy) {
            return super.prepare(manager, profiler);
        }
        Map<Identifier, IGenderArmor> legacyResult = LEGACY.prepare(manager, profiler);
        for (final Map.Entry<Identifier, IGenderArmor> entry : legacyResult.entrySet()) {
            WildfireGender.LOGGER.warn("Gender Armor config: '{}' should be moved to the new folder path: '{}'", entry.getKey(), GenderArmorProvider.PREFIX);
        }
        legacyResult.putAll(super.prepare(manager, profiler));
        return legacyResult;
    }

    @Override
    protected void apply(Map<Identifier, IGenderArmor> prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.configs = Collections.unmodifiableMap(prepared);
    }

    public static @Nullable IGenderArmor get(Identifier model) {
        return INSTANCE.configs.get(model);
    }

    public static Optional<IGenderArmor> get(ItemStack item) {
        return Optional.ofNullable(item.get(DataComponents.EQUIPPABLE))
            .flatMap(Equippable::assetId)
            .map(ResourceKey::identifier)
            .map(GenderArmorResourceManager::get);
    }
}
