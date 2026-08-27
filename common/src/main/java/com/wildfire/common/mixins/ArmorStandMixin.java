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

package com.wildfire.common.mixins;

//~ !entity_types
import com.llamalad7.mixinextras.sugar.Local;
import com.wildfire.common.WildfireEventHandler;
import com.wildfire.common.entitydata.BreastDataComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ArmorStand.class)
abstract class ArmorStandMixin extends LivingEntity {
    private ArmorStandMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @SuppressWarnings("LocalMayUseName")
    @ModifyArg(
        method = "swapItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 1
    )
    public ItemStack attachBreastData(ItemStack stack, @Local(argsOnly = true) EquipmentSlot slot, @Local(argsOnly = true) Player player) {
        if(level().isClientSide() || slot != EquipmentSlot.CHEST || stack.isEmpty()) {
            return stack;
        }

        WildfireEventHandler.onEquipArmorStand(player, stack);

        return stack;
    }

    @SuppressWarnings("LocalMayUseName")
    @ModifyArg(
        method = "swapItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 1
    )
    public ItemStack removeBreastDataOnReplace(ItemStack stack, @Local(argsOnly = true) Player player) {
        if(!player.level().isClientSide()) {
            // this (and the onBreak hook) don't have the same chest slot item guarantee as the above event purely because the only use case
            // we have for this event is removing our nbt from the removed items, which is already only applicable to chest slot items,
            // and safely no-ops otherwise
            BreastDataComponent.removeFromStack(stack);
        }
        return stack;
    }

    @ModifyArg(
        method = "brokenByAnything",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 2
    )
    public ItemStack removeBreastDataOnBreak(ItemStack stack) {
        if(!level().isClientSide()) {
            BreastDataComponent.removeFromStack(stack);
        }
        return stack;
    }
}
