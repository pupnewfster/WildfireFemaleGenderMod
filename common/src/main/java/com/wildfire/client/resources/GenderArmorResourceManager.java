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

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.wildfire.api.IGenderArmor;
import com.wildfire.api.WildfireAPI;
import com.wildfire.api.data.GenderArmorProvider;
import com.wildfire.common.WildfireGender;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// @apiNote Only use this on the client side
public final class GenderArmorResourceManager extends SimpleJsonResourceReloadListener<IGenderArmor> {

    private static final FileToIdConverter LEGACY_PATH_CONVERTER = FileToIdConverter.json("wildfire_gender_data");
    public static final Identifier ID = WildfireGender.id("armor_data");
    public static final GenderArmorResourceManager INSTANCE = new GenderArmorResourceManager();
    private @Unmodifiable Map<Identifier, IGenderArmor> configs = Map.of();

    private GenderArmorResourceManager() {
        super(IGenderArmor.CODEC, FileToIdConverter.json(GenderArmorProvider.PREFIX));
    }

    @Override
    protected Map<Identifier, IGenderArmor> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, IGenderArmor> result = super.prepare(manager, profiler);
        handleLegacyFiles(manager, result);
        return result;
    }

    @Deprecated(forRemoval = true)
    private void handleLegacyFiles(ResourceManager manager, Map<Identifier, IGenderArmor> result) {
        int newSize = result.size();
        Set<IGenderArmor> legacyElements = new ReferenceOpenHashSet<>();
        // TODO is there an alternative for this?
        //? if <=26.2 {
        /*scanDirectory(manager, LEGACY_PATH_CONVERTER, JsonOps.INSTANCE, IGenderArmor.CODEC.validate(armor -> {
            legacyElements.add(armor);
            return DataResult.success(armor);
        }), result);
        *///?}
        if (newSize != result.size()) {
            for (final Map.Entry<Identifier, IGenderArmor> entry : result.entrySet()) {
                if (legacyElements.contains(entry.getValue())) {
                    WildfireGender.LOGGER.warn("Gender Armor config: '{}' should be moved to the new folder path: '{}'", entry.getKey(), GenderArmorProvider.PREFIX);
                }
            }
        }
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
