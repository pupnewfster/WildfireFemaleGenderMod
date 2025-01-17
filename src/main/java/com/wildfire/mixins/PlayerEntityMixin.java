package com.wildfire.mixins;
/*
Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
Copyright (C) 2022  WildfireRomeo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
import com.mojang.authlib.GameProfile;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.http.NameValuePair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;


@Environment(EnvType.CLIENT)
@Mixin(value = PlayerEntity.class, priority = 900)
public abstract class PlayerEntityMixin extends LivingEntity {
	
	public float wfg_femaleBreast;
	public float wfg_preBounce;
	
	float bounceVel = 0;
	float targetBounce = 0;
	float preY = 0;
	
	boolean justSneaking = false;
	boolean alreadySleeping = false;
	
	public PlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
		super(EntityType.PLAYER, world);
	}

	@Inject(at = @At("TAIL"), method = "tick")
	public void onTick(CallbackInfo info) {
		tickWildfire();
	}

	public void tickWildfire() {
		if(!this.world.isClient()) return;
		GenderPlayer aPlr = WildfireGender.getPlayerByName(this.getUuid().toString());
		if(aPlr == null) return;
		PlayerEntity plr = (PlayerEntity) (Object) this;
		aPlr.getLeftBreastPhysics().update(plr);
		aPlr.getRightBreastPhysics().update(plr);


	}

}