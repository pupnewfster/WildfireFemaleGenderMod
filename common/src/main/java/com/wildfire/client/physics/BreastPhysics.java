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

package com.wildfire.client.physics;

import com.wildfire.api.IGenderArmor;
import com.wildfire.client.config.ClientConfig;
import com.wildfire.common.entitydata.EntityConfigHolder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

// TODO break this up into multiple classes/stages?
// maybe also expose an API to add/suppress various physics tick stages?
public class BreastPhysics {

    public static final float TIGHTNESS_REDUCTION_FACTOR = 0.15F;

    //X-Axis
    private float bounceVelX = 0, targetBounceX = 0, velocityX = 0, positionX, prePositionX;
    //Y-Axis
    private float bounceVel = 0, targetBounceY = 0, velocity = 0, positionY, prePositionY;
    //Rotation
    private float bounceRotVel = 0, targetRotVel = 0, rotVelocity = 0, bounceRotation, preBounceRotation;

    private float breastSize = 0, preBreastSize = 0;

    private @Nullable Pose lastPose;
    private int lastSwingDuration = 6, lastSwingTick = 0;
    private @Nullable Vec3 prePos;

    private final EntityConfigHolder<?> entityConfig;
    private int randomB = 1;
    private double lastVerticalMoveVelocity;

    public BreastPhysics(EntityConfigHolder<?> entityConfig) {
        this.entityConfig = entityConfig;
    }

    private static boolean vehicleSuppressesRotation(Entity vehicle) {
        return switch(vehicle) {
            // while you aren't able to normally ride chickens in vanilla, it is still possible through
            // means like /ride, and as chickens attempt to force the rider's body yaw to the same yaw
            // as the chicken (which is likely intended only for baby zombies), this results in unintended
            // behavior with what we're doing
            case Chicken _ -> true;
            // unsaddled horses (and llamas) also break rotation physics, despite acting similarly to other
            // entities where the rider's body yaw is allowed to (somewhat) freely move around
            case AbstractHorse horse when !horse.isSaddled() -> true;
            // camels also suffer from largely the same issue as unsaddled horses when sitting or standing up
            case Camel camel when camel.refuseToMove() -> true;
            default -> false;
        };
    }

    private static boolean shouldUseVehicleYaw(LivingEntity rider, Entity vehicle) {
        return vehicle.hasControllingPassenger()
               // boats will typically be caught by the above #hasControllingPassenger() check, but still
               // special case these to catch any weird modded cases that might arise
               || vehicle instanceof Boat
               // general catch-all for other entities that force the rider's body yaw to match theirs,
               // such as horses
               || vehicle.getVisualRotationYInDegrees() == rider.getVisualRotationYInDegrees();
    }

    private float calcRotation(LivingEntity entity, float bounceIntensity) {
        Entity vehicle = entity.getVehicle();
        if(vehicle != null) {
            if(vehicleSuppressesRotation(vehicle)) {
                return 0f;
            } else if(shouldUseVehicleYaw(entity, vehicle)) {
                float previous = vehicle instanceof LivingEntity living ? living.yBodyRotO : vehicle.yRotO;
                return -((vehicle.getVisualRotationYInDegrees() - previous) / 15f) * bounceIntensity;
            }
        }

        return -((entity.yBodyRot - entity.yBodyRotO) / 15f) * bounceIntensity;
    }

    // this class cannot be blanket marked as client-side only, as this is referenced in the constructor for EntityConfig;
    // as such, the best we can get here is marking this method as such.
    /// @apiNote Only call this on the client side, or the implementation will crash
    public void update(LivingEntity entity, IGenderArmor armor) {
        boolean armorPhysicsOverride = ClientConfig.config().overrides().armorPhysics().get();
        // always suppress the full physics calculations on armor stands
        if(entity instanceof ArmorStand || entityConfig.forceSimplifiedPhysics) {
            simplifiedTick(armor, armorPhysicsOverride);
            return;
        }
        //TODO: This is how when not uniboob it used to generate the random number. Do we care about this, should we just get the random source from the entity?
        RandomSource random = RandomSource.createThreadLocalInstance();

        this.prePositionY = this.positionY;
        this.prePositionX = this.positionX;
        this.preBounceRotation = this.bounceRotation;
        this.preBreastSize = this.breastSize;

        if(this.prePos == null) {
            this.prePos = entity.position();
            return;
        }

        float targetBreastSize = entityConfig.breasts().bustSize().get();
        float breastWeight = targetBreastSize * 1.25f;

        if (!entityConfig.gender().get().canHaveBreasts()) {
            targetBreastSize = 0;
        } else {
            float tightness = Math.clamp(armor.tightness(), 0, 1);
            if (armorPhysicsOverride) tightness = 0; //override resistance
            //Scale breast size by how tight the armor is, clamping at a max adjustment of shrinking by 0.15
            targetBreastSize *= 1 - TIGHTNESS_REDUCTION_FACTOR * tightness;
        }

        breastSize += (breastSize < targetBreastSize) ? Math.abs(breastSize - targetBreastSize) / 2f : -Math.abs(breastSize - targetBreastSize) / 2f;

        Vec3 motion = entity.position().subtract(this.prePos);
        this.prePos = entity.position();

        float bounceIntensity = targetBreastSize * 3f * Math.round(entityConfig.breasts().physics().bounceMultiplier().get() * 3 * 100) / 100f;
        float resistance = Math.clamp(armor.physicsResistance(), 0, 1);
        if (armorPhysicsOverride) resistance = 0; //override resistance

        //Adjust bounce intensity by physics resistance of the worn armor
        bounceIntensity *= 1 - resistance;

        if(!entityConfig.breasts().physics().uniboob().get()) {
            bounceIntensity *= Mth.randomBetween(random, 0.5F, 2.5F);
        }

        tickMovement(entity, random, motion, bounceIntensity, breastWeight);
        tickPose(entity, bounceIntensity);
        tickVehicle(entity, random, bounceIntensity, breastWeight);
        tickArmSwing(entity, random, bounceIntensity);
        finishTick();
    }

    private void simplifiedTick(IGenderArmor armor, boolean armorPhysicsOverride) {
        if(entityConfig.gender().get().canHaveBreasts()) {
            this.breastSize = entityConfig.breasts().bustSize().get();
            if (!armorPhysicsOverride) {
                float tightness = Math.clamp(armor.tightness(), 0, 1);
                this.breastSize *= 1 - TIGHTNESS_REDUCTION_FACTOR * tightness;
            }
            this.preBreastSize = this.breastSize;
        } else {
            this.preBreastSize = this.breastSize = 0f;
        }
    }

    private void tickMovement(final LivingEntity entity, final RandomSource random, final Vec3 motion, final float bounceIntensity, final float breastWeight) {
        double vertVelocity = entity.getDeltaMovement().y;
        // Randomize which side the breast will angle toward when the player jumps/has upward velocity applied to them,
        // or stops falling
        if((lastVerticalMoveVelocity <= 0 && vertVelocity > 0) || (lastVerticalMoveVelocity < 0 && vertVelocity == 0)) {
            randomB = random.nextBoolean() ? -1 : 1;
        }
        lastVerticalMoveVelocity = vertVelocity;

        this.targetBounceY = (float) motion.y * bounceIntensity;
        this.targetBounceY += breastWeight;

        this.targetRotVel = calcRotation(entity, bounceIntensity);
        this.targetRotVel += (float) motion.y * bounceIntensity * randomB;

        this.targetBounceX = -calcRotation(entity, bounceIntensity) / 10f;

        float speedValue = (float) entity.getDeltaMovement().lengthSqr() / 0.2F;
        speedValue = Mth.cube(speedValue);
        if (speedValue < 1.0F) speedValue = 1.0F;
        this.targetBounceY += Mth.cos(entity.walkAnimation.position() * 0.6662F + Mth.PI) * 0.5F * entity.walkAnimation.speed() * 0.5F / speedValue;
    }

    private void tickPose(final LivingEntity entity, final float bounceIntensity) {
        Pose pose = entity.getPose();
        if(pose != lastPose) {
            if(pose == Pose.CROUCHING || lastPose == Pose.CROUCHING) {
                this.targetBounceY += bounceIntensity;
            } else if(pose == Pose.SLEEPING || lastPose == Pose.SLEEPING) {
                this.targetBounceY = bounceIntensity;
            }
            lastPose = pose;
        }
    }

    private void tickVehicle(LivingEntity entity, RandomSource random, final float bounceIntensity, final float breastWeight) {
        switch (entity.getVehicle()) {
            case Boat boat -> {
                //TODO: Why is this casting to int?
                int leftTime = (int) boat.getRowingTime(AbstractBoat.PADDLE_LEFT, entity.walkAnimation.position());
                int rightTime = (int) boat.getRowingTime(AbstractBoat.PADDLE_RIGHT, entity.walkAnimation.position());

                float rotationL = Mth.clampedLerp(-Mth.PI / 3F, -Mth.PI / 12F, (Mth.sin(-rightTime) + 1.0F) / 2.0F);
                float rotationR = Mth.clampedLerp(-Mth.PI / 4F, Mth.PI / 4F, (Mth.sin(-leftTime + 1.0F) + 1.0F) / 2.0F);
                if(rotationL < -1 || rotationR < -0.6f) {
                    this.targetBounceY = bounceIntensity / 3.25f;
                }
            }
            case Minecart cart -> {
                float speed = (float) cart.getDeltaMovement().lengthSqr();
                if (speed > 0.2F && random.nextDouble() * speed < 0.5F) {
                    this.targetBounceY = (random.nextBoolean() ? -bounceIntensity : bounceIntensity) / 6f;
                    this.targetBounceY += breastWeight;
                }
            }
            case AbstractHorse horse -> {
                float movement = (float) horse.getDeltaMovement().lengthSqr();
                if(horse.getAge() % clampMovement(movement) == 5 && movement > 0.05f) {
                    this.targetBounceY = bounceIntensity / 4f;
                    this.targetBounceY += breastWeight;
                }
            }
            case Pig pig -> {
                float movement = (float) pig.getDeltaMovement().lengthSqr();
                if(pig.getAge() % clampMovement(movement) == 5 && movement > 0.002f) {
                    this.targetBounceY = (bounceIntensity * Math.clamp(movement * 75, 0.1f, 1f)) / 4f;
                    this.targetBounceY += breastWeight;
                }
            }
            case Strider strider -> {
                //From Strider#getPassengerAttachmentPoint (client side handling)
                float animSpeed = Math.min(0.25F, strider.walkAnimation.speed());
                float animPos = strider.walkAnimation.position();
                float offset = 0.12F * Mth.cos(animPos * 1.5F) * 2F * animSpeed;
                //TODO: How do we factor the height offset into it, can we just use the passenger attachment point?
                double heightOffset = strider.getBbHeight() - 0.19 + offset;
                this.targetBounceY += ((float) (heightOffset * 3f) - 4.5F) * bounceIntensity;
            }
            case null, default -> {}
        }
    }

    private void tickArmSwing(LivingEntity entity, RandomSource random, final float bounceIntensity) {
        final var state = getSwingState(entity);
        // Require that either the current swing duration is 2 ticks, or the swing duration from the previous tick is,
        // as any faster and the arm effectively doesn't swing at all; we check the previous tick's swing duration for
        // reasons explained later on in this block
        if((state.duration > 1 || lastSwingDuration > 1) && entity.getPose() != Pose.SLEEPING) {
            float amplifier = Math.clamp(1 + state.amplifier, 0.6f, 1.3f);

            int swingTickDelta = state.tick - lastSwingTick;
            float swingProgress = distanceFromMedian(0, lastSwingDuration, Math.clamp(lastSwingTick, 0, lastSwingDuration));
            HumanoidArm swingingToward = swingProgress > -0.2f ? state.arm.getOpposite() : state.arm;

            // consistently apply even with short swing durations, such as with haste
            int everyNthTick = Math.clamp(state.duration - 1, 1, 5);
            if(state.isSwinging && entity.tickCount % everyNthTick == 0) {
                this.targetBounceY += (random.nextBoolean() ? -0.25f : 0.25f) * amplifier * bounceIntensity;
                this.targetBounceX = 0.325f * state.xAmplifier * bounceIntensity * (state.arm == HumanoidArm.RIGHT ? -1f : 1f);
            }

            if(swingTickDelta < 0 && lastSwingTick != lastSwingDuration - 1) {
                // Add a bit of counter-rotation back toward the currently swinging arm if the previous arm swing
                // animation is interrupted
                // Note that we don't check if the player's arm is currently swinging here to account for cases like
                // haste being used to reset a player's swing; one notable example of this is Wynncraft's spell casting,
                // which applies haste to the player when a spell is successfully cast.
                this.targetRotVel += (state.arm == HumanoidArm.RIGHT ? -4f : 4f) * Math.abs(swingProgress) * bounceIntensity;
            } else if(state.isSwinging && state.duration > 1) {
                // Otherwise if the swing animation isn't interrupted, attempt to rotate slightly counter to the
                // direction that the body is currently moving
                this.targetRotVel += (swingingToward == HumanoidArm.RIGHT ? -0.2f : 0.2f) * amplifier * bounceIntensity;
            }
            lastSwingTick = state.tick;
        }
        if(!state.isSwinging) {
            lastSwingTick = 0;
        }
        lastSwingDuration = Math.max(state.duration, 1);
    }

    private void finishTick() {
        float percent = entityConfig.breasts().physics().floppiness().get();
        float bounceAmount = 0.45f * (1f - percent) + 0.15f;
        bounceAmount = Math.clamp(bounceAmount, 0.15f, 0.6f);
        float delta = 2.25f - bounceAmount;

        float distanceFromMin = Math.abs(bounceVel + 1.5f) * 0.5f;
        float distanceFromMax = Math.abs(bounceVel - 2.65f) * 0.5f;

        if(bounceVel < -0.5f) {
            targetBounceY += distanceFromMin;
        }
        if(bounceVel > 2.5f) {
            targetBounceY -= distanceFromMax;
        }

        targetBounceY = Math.clamp(targetBounceY, -1.5f, 2.5f);
        targetRotVel = Math.clamp(targetRotVel, -25f, 25f);

        this.velocity = Mth.lerp(bounceAmount, this.velocity, (this.targetBounceY - this.bounceVel) * delta);
        this.bounceVel += this.velocity * percent * 1.1625f;

        //X
        this.velocityX = Mth.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
        this.bounceVelX += this.velocityX * percent;

        this.rotVelocity = Mth.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
        this.bounceRotVel += this.rotVelocity * percent;

        this.bounceRotation = this.bounceRotVel;
        this.positionX = this.bounceVelX;
        this.positionY = this.bounceVel;

        if(this.positionY < -0.5f) this.positionY = -0.5f;
        if(this.positionY > 1.5f) {
            this.positionY = 1.5f;
            this.velocity = 0;
        }
    }

    public float getPrePositionY() {
        return this.prePositionY;
    }
    public float getPositionY() {
        return this.positionY;
    }

    public float getPrePositionX() {
        return this.prePositionX;
    }
    public float getPositionX() {
        return this.positionX;
    }

    public float getBounceRotation() {
        return this.bounceRotation;
    }
    public float getPreBounceRotation() {
        return this.preBounceRotation;
    }

    public float getBreastSize() {
        return this.breastSize;
    }
    public float getPreBreastSize() {
        return this.preBreastSize;
    }

    private int clampMovement(float movement) {
        return Math.max((int) (10 - 2 * movement), 1);
    }

    /// Return the distance from the median of the two provided boundary points from a given point
    ///
    /// @param p1    Lower boundary point (inclusive)
    /// @param p2    Upper boundary point (inclusive)
    /// @param point The target point within the range of `p1` and `p2` to get the distance from the median of
    ///
    /// @return A `float` indicating how far the provided `point` is from the median of the two boundary points, with `1f` being at the median exactly, and `0f` being at
    /// either of the two provided boundary points.
    ///
    /// If the provided point is in the latter half of the range between the two boundary points, the returned float will be negative.
    ///
    /// @throws IllegalArgumentException If `p1` is equal to or greater than `p2`, or if `point` is not within the specified range.
    @SuppressWarnings("SameParameterValue")
    private static float distanceFromMedian(final int p1, final int p2, float point) {
        // sanity checks
        if(p1 >= p2) {
            throw new IllegalArgumentException("p2 must be greater than p1");
        }
        if(point < p1 || point > p2) {
            throw new IllegalArgumentException(point + " is not within bounds of (" + p1 + ", " + p2 + ")");
        }

        if(point == p1 || point == p2) {
            return 0;
        }
        // subtract p1 to get the actual inner range, then divide to get the median
        float median = (p2 - p1) / 2f;
        point -= p1;
        if(point > median) {
            // invert the provided point to instead become smaller the further we are away from the median
            // in the latter half of the specified range
            point = -(median - (point - median));
        }
        return point / median;
    }

    @ApiStatus.Internal
    public static SwingState getSwingState(final LivingEntity entity) {
        final int swingDuration, swingTime;
        final HumanoidArm swingingArm;
        //~ if >26.2 'swinging' -> 'isSwinging()'
        final boolean isSwinging = entity.isSwinging();

        //? if >26.2 {
        var swing = entity.swingState;
        var currentSwing = swing.currentSwing;
        swingDuration = currentSwing == null ? 6 : currentSwing.durationTicks();
        swingTime = currentSwing == null ? 0 : swing.ticks;
        swingingArm = currentSwing == null ? entity.getMainArm() : currentSwing.hand().asArm(entity.getMainArm());
        //?} else {
        /*swingDuration = entity.getCurrentSwingDuration();
        swingTime = entity.swingTime;
        swingingArm = entity.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        *///?}

        final float rawAmplifier;
        if(swingDuration < 6) {
            rawAmplifier = 0.15f * (6 - swingDuration);
        } else if(swingDuration > 6) {
            rawAmplifier = -0.055f * (swingDuration - 6);
        } else {
            rawAmplifier = 0f;
        }

        final float reduced = rawAmplifier < 0 ? 1 + (rawAmplifier / 1.5f) : (float)Math.pow(1 + rawAmplifier, -0.65);
        final float xAmp = Mth.clamp(reduced, 0.25f, 1.225f);

        return new SwingState(
            /*isSwinging=*/ isSwinging,
            /*tick =*/ swingTime,
            /*duration =*/ swingDuration,
            /*arm=*/ swingingArm,
            /*amplifier=*/ rawAmplifier,
            /*xAmplifier=*/ xAmp
        );
    }

    @ApiStatus.Internal
    public record SwingState(
        boolean isSwinging,
        int tick,
        int duration,
        HumanoidArm arm,
        float amplifier,
        float xAmplifier
    ) {
    }
}
