package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused")
public class LivingEntityHelper<T extends LivingEntity> extends EntityHelper<T> {

    public LivingEntityHelper(T e) {
        super(e);
    }

    /**
     * @return entity status effects.
     * @since 1.2.7
     */
    public List<StatusEffectHelper> getStatusEffects() {
        List<StatusEffectHelper> l = new ArrayList<>();
        for (MobEffectInstance i : ImmutableList.copyOf(base.getActiveEffects())) {
            l.add(new StatusEffectHelper(i));
        }
        return l;
    }

    /**
     * @param effect the status effect
     * @return if the entity can have a certain status effect
     * @since 1.8.4
     */
    private boolean canHaveStatusEffect(MobEffectInstance effect) {
        return base.canBeAffected(effect);
    }

    /**
     * @param effect the status effect
     * @return if the entity can have a certain status effect
     * @since 1.8.4
     */
    public boolean canHaveStatusEffect(StatusEffectHelper effect) {
        return canHaveStatusEffect(effect.getRaw());
    }

    /**
     * For client side entities, excluding the player, this will most likely return {@code false}
     * even if the entity has the effect, as effects are not synced to the client.
     *
     * @return {@code true} if the entity has the specified status effect, {@code false} otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public boolean hasStatusEffect(String id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(RegistryHelper.parseIdentifier(id));
        return base.getActiveEffects().stream().anyMatch(statusEffectInstance -> statusEffectInstance.getEffect().value().equals(effect));
    }

    /**
     * @return {@code true} if the entity is holding the specified item
     * @since 1.9.0
     */
    @DocletReplaceParams("item: ItemId")
    public boolean isHolding(String item) {
        ResourceLocation id = ResourceLocation.parse(item);
        if (id.equals(BuiltInRegistries.ITEM.getDefaultKey())) return base.isHolding(Items.AIR);
        Item it = BuiltInRegistries.ITEM.getValue(id);
        return it != Items.AIR && base.isHolding(it);
    }

    /**
     * @return the item in the entity's main hand.
     * @see ItemStackHelper
     * @since 1.2.7
     */
    public ItemStackHelper getMainHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    /**
     * @return the item in the entity's off hand.
     * @since 1.2.7
     */
    public ItemStackHelper getOffHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    /**
     * @return the item in the entity's head armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getHeadArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.HEAD));
    }

    /**
     * @return the item in the entity's chest armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getChestArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.CHEST));
    }

    /**
     * @return the item in the entity's leg armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getLegArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.LEGS));
    }

    /**
     * @return the item in the entity's foot armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getFootArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.FEET));
    }

    /**
     * @return entity's health
     * @since 1.3.1
     */
    public float getHealth() {
        return base.getHealth();
    }

    /**
     * @return entity's max health
     * @since 1.6.5
     */
    public float getMaxHealth() {
        return base.getMaxHealth();
    }

    /**
     * @return the entity's absorption amount.
     * @since 1.8.4
     */
    public float getAbsorptionHealth() {
        return base.getAbsorptionAmount();
    }

    /**
     * @return the entity's armor value.
     * @since 1.8.4
     */
    public int getArmor() {
        return base.getArmorValue();
    }

    /**
     * @return the entity's default health.
     * @since 1.8.4
     */
    public int getDefaultHealth() {
        return base.invulnerableDuration;
    }

    /**
     * @since 1.9.1
     *
     */
    @DocletReplaceReturn("JavaList<MobTag>")
    public List<String> getMobTags() {
        return base.getType().builtInRegistryHolder().tags().map(TagKey::location).map(ResourceLocation::toString).toList();
    }

    /**
     * @return if the entity is in a bed.
     * @since 1.2.7
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * @return if the entity has elytra deployed
     * @since 1.5.0
     */
    public boolean isFallFlying() {
        return base.isFallFlying();
    }

    /**
     * @return if the entity is on the ground
     * @since 1.8.4
     */
    public boolean isOnGround() {
        return base.onGround();
    }

    /**
     * @return if the entity can breathe in water
     * @since 1.8.4
     */
    public boolean canBreatheInWater() {
        return base.canBreatheUnderwater();
    }

    /**
     * @return if the entity has no drag
     * @since 1.8.4
     */
    public boolean hasNoDrag() {
        return base.shouldDiscardFriction();
    }

    /**
     * @return if the entity has no gravity
     * @since 1.8.4
     */
    public boolean hasNoGravity() {
        return base.isNoGravity();
    }

    /**
     * @param target the target entity
     * @return if the entity can target a target entity
     * @since 1.8.4
     */
    private boolean canTarget(LivingEntity target) {
        return base.canAttack(target);
    }

    /**
     * @param target the target entity
     * @return if the entity can target a target entity
     * @since 1.8.4
     */
    public boolean canTarget(LivingEntityHelper<?> target) {
        return canTarget(target.getRaw());
    }

    /**
     * @return if the entity can take damage
     * @since 1.8.4
     */
    public boolean canTakeDamage() {
        return base.canBeSeenAsEnemy();
    }

    /**
     * @return if the entity is part of the game (is alive and not spectator)
     * @since 1.8.4
     */
    public boolean isPartOfGame() {
        return base.canBeSeenByAnyone();
    }

    /**
     * @return if the entity is in spectator
     * @since 1.8.4
     */
    public boolean isSpectator() {
        return base.isSpectator();
    }

    /**
     * @return if the entity is undead
     * @since 1.8.4
     */
    public boolean isUndead() {
        return base.getType().builtInRegistryHolder().tags().anyMatch(e -> EntityTypeTags.UNDEAD.location().equals(e.location()));
    }

    /**
     * @return the bow pull progress of the entity, {@code 0} by default.
     * @since 1.8.4
     */
    public double getBowPullProgress() {
        if (base.getMainHandItem().getItem() instanceof BowItem) {
            return BowItem.getPowerForTime(base.getTicksUsingItem());
        } else {
            return 0;
        }
    }

    /**
     * @since 1.9.0
     */
    public int getItemUseTimeLeft() {
        return base.getUseItemRemainingTicks();
    }

    /**
     * @return {@code true} if the entity is a baby, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBaby() {
        return base.isBaby();
    }

    /**
     * @param entity the entity to check line of sight to
     * @return {@code true} if the player has line of sight to the specified entity, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canSeeEntity(EntityHelper<?> entity) {
        return canSeeEntity(entity, true);
    }

    /**
     * @param entity     the entity to check line of sight to
     * @param simpleCast whether to use a simple raycast or a more complex one
     * @return {@code true} if the entity has line of sight to the specified entity, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canSeeEntity(EntityHelper<?> entity, boolean simpleCast) {
        Entity rawEntity = entity.getRaw();

        Vec3 baseEyePos = new Vec3(base.getX(), base.getEyeY(), base.getZ());
        Vec3 vec3d = base.getEyePosition();
        Vec3 vec3d2 = base.getViewVector(1.0F).scale(10);
        Vec3 vec3d3 = vec3d.add(vec3d2);
        AABB box = base.getBoundingBox().expandTowards(vec3d2).inflate(1.0);

        Function<Vec3, Boolean> canSee = pos -> base.level().clip(new ClipContext(baseEyePos, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, base)).getType() == HitResult.Type.MISS;

        if (canSee.apply(new Vec3(rawEntity.getX(), rawEntity.getEyeY(), rawEntity.getZ()))
                || canSee.apply(new Vec3(rawEntity.getX(), rawEntity.getY() + 0.5, rawEntity.getZ()))
                || canSee.apply(new Vec3(rawEntity.getX(), rawEntity.getY(), rawEntity.getZ()))) {
            return true;
        }

        if (simpleCast) {
            return false;
        }

        AABB boundingBox = rawEntity.getBoundingBox();
        double bHeight = boundingBox.maxY - boundingBox.minY;
        int steps = (int) (bHeight / 0.1);
        double diffX = (boundingBox.maxX - boundingBox.minX) / 2;
        double diffZ = (boundingBox.maxZ - boundingBox.minZ) / 2;
        // Create 4 pillars around the mob to check for visibility
        for (int i = 0; i < steps; i++) {
            double y = i * 0.1;
            if (canSee.apply(new Vec3(rawEntity.getX() + diffX, rawEntity.getY() + y, rawEntity.getZ()))
                    || canSee.apply(new Vec3(rawEntity.getX() - diffX, rawEntity.getY() + y, rawEntity.getZ()))
                    || canSee.apply(new Vec3(rawEntity.getX(), rawEntity.getY() + y, rawEntity.getZ() + diffZ))
                    || canSee.apply(new Vec3(rawEntity.getX(), rawEntity.getY() + y, rawEntity.getZ() - diffZ))) {
                return true;
            }
        }
        return false;
    }

}
