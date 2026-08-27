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

//~ !entity_types
import com.wildfire.client.WildfireClientEventHandler;
import com.wildfire.client.WildfireGenderClient;
import com.wildfire.client.WildfireKeyBindings;
import com.wildfire.client.command.WildfireCommand;
import com.wildfire.client.gui.SyncedPlayerList;
import com.wildfire.common.LoaderAgnostics;
import com.wildfire.common.WildfireGender;
import com.wildfire.client.config.ClientConfig;
import com.wildfire.fabric.common.networking.FabricSync;
import com.wildfire.client.render.debug.GenderDebugHudEntry;
import com.wildfire.client.render.debug.PhysicsDebugHudEntry;
import com.wildfire.client.resources.GenderArmorResourceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class WildfireGenderClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WildfireGenderClient.tryMigrate();

        ClientConfig.INSTANCE.load(null);
        FabricClientHelper.registerSounds();
        FabricSync.registerClient();
        registerKeybindings();
        registerClientEvents();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GenderArmorResourceManager.ID, GenderArmorResourceManager.INSTANCE);
        DebugScreenEntries.register(GenderDebugHudEntry.SELF, new GenderDebugHudEntry(true));
        DebugScreenEntries.register(GenderDebugHudEntry.OTHER, new GenderDebugHudEntry(false));
        // only register this in dev env, as this likely isn't going to be very useful anywhere else.
        if (LoaderAgnostics.INSTANCE.isDevelopmentEnv()) {
            DebugScreenEntries.register(PhysicsDebugHudEntry.ID, new PhysicsDebugHudEntry());
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> WildfireCommand.register(dispatcher, new FabricCommandHelper()));
    }

    private void registerKeybindings() {
        KeyMappingHelper.registerKeyMapping(WildfireKeyBindings.INSTANCE.configKey());
        KeyMappingHelper.registerKeyMapping(WildfireKeyBindings.INSTANCE.toggleKey());
    }

    /// Register all client-side events
    private void registerClientEvents() {
        ClientEntityEvents.ENTITY_UNLOAD.register(WildfireClientEventHandler::onEntityUnload);
        ClientTickEvents.END_CLIENT_TICK.register(WildfireClientEventHandler::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> WildfireClientEventHandler.clientDisconnect());
        ClientPlayConnectionEvents.JOIN.register((_, _, client) -> WildfireClientEventHandler.clientJoin(client));
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(this::registerRenderLayers);
        HudElementRegistry.attachElementAfter(
            //Note: The reason this can't use PLAYER_LIST as the target is because fabric inherits the visibility requirements of the element
            // so then it wouldn't render properly when set to ALL
            VanillaHudElements.MISC_OVERLAYS,
            WildfireGender.id("player_list"),
            WildfireClientEventHandler::renderHud
        );
        ClientTickEvents.END_CLIENT_TICK.register(SyncedPlayerList::onTick);
    }

    /// Attach breast render layers to players and armor stands
    private void registerRenderLayers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?, ?> entityRenderer,
        LivingEntityRenderLayerRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererProvider.Context context) {
        if (entityRenderer instanceof AvatarRenderer<?> playerRenderer) {
            WildfireClientEventHandler.addAvatarRenderLayers(playerRenderer, context.getEquipmentRenderer(), (_, layer) -> registrationHelper.register(layer));
        } else if (entityRenderer instanceof ArmorStandRenderer armorStandRenderer) {
            WildfireClientEventHandler.addArmorStandRenderLayers(armorStandRenderer, context.getEquipmentRenderer(), (_, layer) -> registrationHelper.register(layer));
        }
    }
}
