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

package com.wildfire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wildfire.client.ClientHelper;
import com.wildfire.common.WildfireGender;
import com.wildfire.common.WildfireHelper;
import com.wildfire.client.config.ClientConfig;
import com.wildfire.common.entitydata.BreastState;
import com.wildfire.api.uvs.UVLayout;
import com.wildfire.client.render.WildfireModelRenderer.BreastModelBox;
import com.wildfire.client.render.WildfireModelRenderer.OverlayModelBox;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

// TODO split this into an AbstractGenderLayer?
/// @apiNote Only use this on the client side
public class GenderLayer<STATE extends HumanoidRenderState, MODEL extends HumanoidModel<STATE>> extends RenderLayer<STATE, MODEL> {

    /// @apiNote  **Do not modify this array**
    private static final BreastSide[] BREAST_SIDES = BreastSide.values();

    @UnknownNullability("null until #resizeBox() is first called")
    private BreastModelBox lBreast, rBreast;
    @UnknownNullability("null until #resizeBox() is first called")
    private OverlayModelBox lBreastWear, rBreastWear;

    private @Nullable UVLayout prevLeftBreastUVLayout, prevRightBreastUVLayout,
        prevLeftBreastOverlayUVLayout, prevRightBreastOverlayUVLayout;

    private final RenderLayerParent<STATE, MODEL> context;

    private boolean isUniboob;
    protected boolean isChestplateOccupied, bounceEnabled, breathingAnimation;
    protected float breastOffsetX, breastOffsetY, breastOffsetZ, lPhysPositionY, lPhysPositionX, rPhysPositionY, rPhysPositionX,
            lPhysBounceRotation, rPhysBounceRotation, breastSize, zOffset, outwardAngle;

    public GenderLayer(RenderLayerParent<STATE, MODEL> render) {
        super(render);
        this.context = render;
    }

    @Override
    public void submit(PoseStack matrixStack, SubmitNodeCollector nodeCollector, int lightCoords, STATE state, float limbAngle, float limbDistance) {
        var genderRenderState = ClientHelper.INSTANCE.getRenderState(state);
        if (genderRenderState == null) return;

        try {
            if(!setupRender(state, genderRenderState)) return;
            //Note: We ignore LivingEntityRenderer#getWhiteOverlayProgress as it is always 0 for entities we attach the layer to
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0);
            renderSides(state, getParentModel(), genderRenderState, matrixStack, nodeCollector, lightCoords, overlayCoords, this::renderBreast);
        } catch(Exception e) {
            WildfireGender.LOGGER.error("Failed to render breast layer", e);
        }
    }

    /// Common logic for setting up breast rendering
    ///
    /// @return `true` if rendering should continue
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean setupRender(STATE entityState, GenderRenderState genderState) {
        if (ClientConfig.config().overrides().disableRendering().get()) return false;

        boolean armorPhysicsOverride = ClientConfig.config().overrides().armorPhysics().get();
        isChestplateOccupied = genderState.armor.coversBreasts() && !armorPhysicsOverride;
        if (genderState.armor.alwaysHidesBreasts() || !genderState.showBreastsInArmor && isChestplateOccupied) {
            //If the armor always hides breasts or there is armor and the player configured breasts
            // to be hidden when wearing armor, we can just exit early rather than doing any calculations
            return false;
        }

        if(!isLayerVisible(entityState, genderState)) {
            return false;
        }

        BreastState breasts = genderState.breasts;
        breastOffsetX = WildfireHelper.round(breasts.offsets().x(), 1);
        breastOffsetY = -WildfireHelper.round(breasts.offsets().y(), 1);
        breastOffsetZ = -WildfireHelper.round(breasts.offsets().z(), 1);

        isUniboob = genderState.uniboob;

        GenderRenderState.BreastPhysicsState leftPhysicsState = genderState.leftBreastPhysics;
        final float bSize = leftPhysicsState.getBreastSize();
        outwardAngle = Math.round(breasts.cleavage() * 100f);
        outwardAngle = Math.min(outwardAngle, 10);

        resizeBox(genderState, bSize);

        lPhysPositionY = leftPhysicsState.getPositionY();
        lPhysPositionX = leftPhysicsState.getPositionX();
        lPhysBounceRotation = leftPhysicsState.getBounceRotation();
        if(isUniboob) {
            rPhysPositionY = lPhysPositionY;
            rPhysPositionX = lPhysPositionX;
            rPhysBounceRotation = lPhysBounceRotation;
        } else {
            GenderRenderState.BreastPhysicsState rightPhysicsState = genderState.rightBreastPhysics;
            rPhysPositionY = rightPhysicsState.getPositionY();
            rPhysPositionX = rightPhysicsState.getPositionX();
            rPhysBounceRotation = rightPhysicsState.getBounceRotation();
        }

        breastSize = Math.min(bSize * 1.5f, 0.7f); // Limit the max size to 0.7f

        if (bSize > 0.7f) {
            breastSize = bSize; // If bSize exceeds 0.7f, use bSize
        }

        if (breastSize < 0.02f) {
            return false; // Return false if breastSize is too small
        }

        zOffset = 0.0625f - (bSize * 0.0625f); // Calculate zOffset
        breastSize += 0.5f * Math.abs(bSize - 0.7f) * 2f; // Adjust breastSize based on bSize

        float resistance = Math.clamp(genderState.armor.physicsResistance(), 0, 1);
        breathingAnimation = (armorPhysicsOverride || resistance <= 0.5F) && genderState.isBreathing;
        bounceEnabled = genderState.hasBreastPhysics && (!isChestplateOccupied || resistance < 1); //oh, you found this?
        return true;
    }

    protected boolean isLayerVisible(STATE state, GenderRenderState genderState) {
        return !state.isInvisibleToPlayer || state.appearsGlowing();
    }

    protected void resizeBox(GenderRenderState state, float breastSize) {
        //TODO: Better way for this?
        if(!Objects.equals(this.prevLeftBreastUVLayout, state.leftBreastUVLayout)
                || !Objects.equals(this.prevRightBreastUVLayout, state.rightBreastUVLayout)
                || !Objects.equals(this.prevLeftBreastOverlayUVLayout, state.leftBreastOverlayUVLayout)
                || !Objects.equals(this.prevRightBreastOverlayUVLayout, state.rightBreastOverlayUVLayout)) {

            this.prevLeftBreastUVLayout = state.leftBreastUVLayout;
            this.prevRightBreastUVLayout = state.rightBreastUVLayout;
            this.prevLeftBreastOverlayUVLayout = state.leftBreastOverlayUVLayout;
            this.prevRightBreastOverlayUVLayout = state.rightBreastOverlayUVLayout;

            this.lBreast = new BreastModelBox(64, 64, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, state.leftBreastUVLayout);
            this.rBreast = new BreastModelBox(64, 64, 0F, 0.0F, 0F, 4, 5, 3, 0.0F, state.rightBreastUVLayout);
            this.lBreastWear = new OverlayModelBox(64, 64, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, state.leftBreastOverlayUVLayout);
            this.rBreastWear = new OverlayModelBox(64, 64, 0, 0.0F, 0F, 4, 5, 3, 0.0F, state.rightBreastOverlayUVLayout);
        }
    }

    protected void setupTransformations(STATE state, MODEL model, GenderRenderState genderState, PoseStack matrixStack, BreastSide side) {
        if(state.isBaby) {
            matrixStack.scale(state.ageScale, state.ageScale, state.ageScale);
            matrixStack.translate(0f, 0.75f, 0f);
        }

        model.root().translateAndRotate(matrixStack);
        ModelPart body = model.body;
        body.translateAndRotate(matrixStack);

        if(bounceEnabled) {
            matrixStack.translate(side.forSide(lPhysPositionX, rPhysPositionX) / 32f, side.forSide(lPhysPositionY, rPhysPositionY) / 32f, 0);
        }

        matrixStack.translate(side.leftOrNegate(breastOffsetX) * 0.0625f, 0.05625f + (breastOffsetY * 0.0625f), zOffset - 0.0625f * 2f + (breastOffsetZ * 0.0425f)); //shift down to correct position

        if(!isUniboob) {
            matrixStack.translate(side.leftOrNegate(-0.0625f * 2), 0, 0);
        }
        if(bounceEnabled) {
            //? if <=26.2 {
            /*matrixStack.mulPose(Axis.YP.rotationDegrees(side.forSide(lPhysBounceRotation, rPhysBounceRotation)));
            *///?} else {
            matrixStack.rotateDegrees(Axis.YP, side.forSide(lPhysBounceRotation, rPhysBounceRotation));
            //?}
        }
        if(!isUniboob) {
            matrixStack.translate(side.leftOrNegate(0.0625f * 2), 0, 0);
        }

        float rotation = breastSize;
        if(bounceEnabled) {
            matrixStack.translate(0, -0.035f * breastSize, 0); //shift down to correct position
            rotation -= side.forSide(lPhysPositionY, rPhysPositionY) / 12f;
        }

        rotation = Math.min(rotation, breastSize + 0.2f);
        rotation = Math.min(rotation, 1); //hard limit for MAX

        if(isChestplateOccupied) {
            matrixStack.translate(0, 0, 0.01f);
        }

        var rotationTransform = side.forSide(Axis.YP, Axis.YN)
            //~ if >=26.3-pre-2 'rotationDegrees(' -> 'rotateDegrees(new org.joml.Matrix3f(), '
            .rotateDegrees(new org.joml.Matrix3f(), outwardAngle)
            .rotateX(-35f * rotation * Mth.DEG_TO_RAD);

        if(breathingAnimation) {
            float f5 = -Mth.cos(state.ageInTicks * 0.09F) * 0.45F + 0.45F;
            rotationTransform.rotateX(f5 * Mth.DEG_TO_RAD);
        }

        //~ if >=26.3-pre-2 'rotationTransform' -> 'new org.joml.Matrix4f(rotationTransform)'
        matrixStack.mulPose(new org.joml.Matrix4f(rotationTransform));
        matrixStack.scale(0.9995f, 1f, 1f); //z-fighting FIXXX
    }

    private void renderBreast(STATE state, GenderRenderState genderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords, int overlayCoords, BreastSide side) {
        var renderer = (LivingEntityRenderer<?, STATE, ?>) context;

        boolean bodyVisible = renderer.isBodyVisible(state);
        boolean forceTransparent = !bodyVisible && !state.isInvisibleToPlayer;
        boolean glowing = state.appearsGlowing();

        RenderType type = renderer.getRenderType(state, bodyVisible, forceTransparent, glowing);
        if(type == null) {
            // only render if the player is visible in some capacity
            return;
        }

        int baseColor = forceTransparent ? 0x26FFFFFF : CommonColors.WHITE;
        //Note: We ignore LivingEntityRenderer#getModelTint as it is always WHITE for entities we attach the layer to

        //Note: While the living entity renderer uses the light coords, we mirror Deadmau5EarsLayer and use the passed in light coords
        // (which the renderer passes the state's light coords in, but we might as well be consistent with how vanilla does extra body parts as a layer)
        var model = side.forSide(lBreast, rBreast);
        //~ if >=26.3-pre-2 'outlineColor, null' -> 'outlineColor' {
        nodeCollector.submitModel(new BreastModel(model), state, poseStack, type, lightCoords, overlayCoords, baseColor, null, state.outlineColor);

        if (genderState.hasJacketLayer) {
            poseStack.translate(0, 0, -0.015f);
            poseStack.scale(1.05f, 1.05f, 1.05f);
            var jacketModel = side.forSide(lBreastWear, rBreastWear);
            nodeCollector.order(1).submitModel(new BreastModel(jacketModel), state, poseStack, type, lightCoords, overlayCoords, baseColor, null, state.outlineColor);
        }
        //~}
    }

    protected void renderSides(STATE state, MODEL model, GenderRenderState genderState, PoseStack matrixStack, SubmitNodeCollector nodeCollector, int lightCoords,
        int overlayCoords, BreastSideRenderer<STATE> renderer) {
        for (final BreastSide breastSide : BREAST_SIDES) {
            matrixStack.pushPose();
            try {
                setupTransformations(state, model, genderState, matrixStack, breastSide);
                renderer.render(state, genderState, matrixStack, nodeCollector, lightCoords, overlayCoords, breastSide);
            } finally {
                matrixStack.popPose();
            }
        }
    }

    @FunctionalInterface
    protected interface BreastSideRenderer<STATE extends HumanoidRenderState> {
        void render(STATE state, GenderRenderState genderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords, int overlayCoords, BreastSide side);
    }
}
