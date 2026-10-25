package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
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

/**
 * Adds living-entity state, such as health, equipment, and status effects, to an entity
 * helper.
 * <p>
 * Values reflect the client's current entity state; this helper does not modify it.
 * <p>
 * {@code asLiving()} on an entity helper is a cast, not a type check; it throws if the
 * helper does not wrap a living entity. Use {@code instanceof EntityHelpers.Living}, or a
 * type-filtered entity query, when narrowing from an unfiltered entity list.
 * <pre>
 * // the living things within twenty blocks, and their health
 * const mobs = World.getEntities(20, "zombie", "skeleton", "creeper", "cow");
 * if (mobs !== null) {
 *   for (const entity of mobs) {
 *     if (!(entity instanceof EntityHelpers.Living)) continue;
 *     Chat.log(`${entity.getType()}: ${entity.getHealth()} of ${entity.getMaxHealth()}`);
 *   }
 * }
 * </pre>
 *
 * @param <T> the living entity wrapped by this helper
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class LivingEntityHelper<T extends LivingEntity> extends EntityHelper<T> {

    /** Wraps the supplied living entity, retaining the same entity reference.
     * @param e the living entity to wrap
     */
    public LivingEntityHelper(T e) {
        super(e);
    }

    /**
     * Returns a new list of the entity's active status effects.
     *
     * @return the entity's active status effects
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
     * Checks whether Minecraft considers this entity eligible for the given effect;
     * this does not check whether the effect is currently active.
     *
     * @param effect the status effect
     * @return {@code true} if the entity can be affected by the effect
     * @since 1.8.4
     */
    public boolean canHaveStatusEffect(StatusEffectHelper effect) {
        return canHaveStatusEffect(effect.getRaw());
    }

    /**
     * Checks the client's active-effect list for the given effect ID. For remote
     * entities, this reflects only effects present in the client-side state.
     *
     * @param id the id of the status effect to look for
     * @return {@code true} if an active effect has this ID; {@code false} otherwise
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public boolean hasStatusEffect(String id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(RegistryHelper.parseIdentifier(id));
        return base.getActiveEffects().stream().anyMatch(statusEffectInstance -> statusEffectInstance.getEffect().value().equals(effect));
    }

    /**
     * Checks whether either hand holds the item with this ID. The namespace may be
     * omitted and defaults to {@code minecraft}; the air ID checks for an empty hand.
     *
     * @param item the id of the item to look for
     * @return {@code true} if either hand holds the item; {@code false} otherwise
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
     * Returns the main-hand stack; an empty hand is represented by an empty stack,
     * not {@code null}.
     *
     * @return the item in the entity's main hand.
     * @since 1.2.7
     */
    public ItemStackHelper getMainHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    /**
     * Returns the off-hand stack; an empty slot is represented by an empty stack.
     *
     * @return the item in the entity's off hand.
     * @since 1.2.7
     */
    public ItemStackHelper getOffHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    /**
     * Returns the stack in the head armor slot; an empty slot is represented by an
     * empty stack.
     *
     * @return the item in the entity's head armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getHeadArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.HEAD));
    }

    /**
     * Returns the stack in the chest armor slot; an empty slot is represented by an
     * empty stack.
     *
     * @return the item in the entity's chest armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getChestArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.CHEST));
    }

    /**
     * the item in the entity's leggings slot, or an empty stack if the slot is empty.
     *
     * @return the item in the entity's leg armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getLegArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.LEGS));
    }

    /**
     * the item in the entity's boots slot, or an empty stack if the slot is empty.
     *
     * @return the item in the entity's foot armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getFootArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.FEET));
    }

    /**
     * the entity's current health, on the same scale as {@link #getMaxHealth()}. For
     * entities observed by the client, this is the latest value synchronized to it.
     *
     * @return entity's health
     * @since 1.3.1
     */
    public float getHealth() {
        return base.getHealth();
    }

    /**
     * the entity's maximum health, which sets the upper bound for {@link #getHealth()}.
     *
     * @return entity's max health
     * @since 1.6.5
     */
    public float getMaxHealth() {
        return base.getMaxHealth();
    }

    /**
     * the entity's absorption amount, which absorbs damage before health.
     *
     * @return the entity's absorption amount.
     * @since 1.8.4
     */
    public float getAbsorptionHealth() {
        return base.getAbsorptionAmount();
    }

    /**
     * the entity's armor value as used by the game's damage calculation.
     *
     * @return the entity's armor value.
     * @since 1.8.4
     */
    public int getArmor() {
        return base.getArmorValue();
    }

    /**
     * the entity's armor-toughness attribute value.
     *
     * @return the entity's armor toughness.
     * @since 2.0.0
     */
    public double getArmorToughness() {
        return base.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    /**
     * Returns a version-dependent value under this legacy method name: in Minecraft
     * 1.21.8, the entity's invulnerability duration; from 1.21.11, its maximum health.
     *
     * @return the version-dependent value described above
     * @since 1.8.4 (returns float since 2.0.0)
     */
    public float getDefaultHealth() {
        //? if >=1.21.11 {
        /*return base.getMaxHealth();
        *///? } else {
        return (float) base.invulnerableDuration;
        //? }
    }

    /**
     * the tags on this kind of entity rather than on this one entity, which is what the
     * game's own entity type registry carries. A tag is a group the entity type is in,
     * such as the one the game uses to find the undead, so two entities of the same type
     * give the same list whatever has happened to them.
     * <p>
     * The entries are tag ids as strings with the namespace on the front. This is the
     * list {@link #isUndead()} is answered from, rather than a check run here.
     *
     * @return the tags on this entity's type
     * @since 1.9.1
     */
    @DocletReplaceReturn("JavaList<MobTag>")
    public List<String> getMobTags() {
        return base.getType().builtInRegistryHolder().tags().map(TagKey::location).map(ResourceLocation::toString).toList();
    }

    /**
     * whether the entity has a sleeping position. This indicates sleeping state, not
     * how long it has been asleep.
     *
     * @return if the entity is in a bed.
     * @since 1.2.7
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * whether the entity is currently fall-flying; this reports flight state, not merely
     * whether it has an elytra equipped.
     *
     * @return if the entity has elytra deployed
     * @since 1.5.0
     */
    public boolean isFallFlying() {
        return base.isFallFlying();
    }

    /**
     * whether the entity's on-ground flag is set; this is not a direct check of the block
     * beneath it.
     *
     * @return if the entity is on the ground
     * @since 1.8.4
     */
    public boolean isOnGround() {
        return base.onGround();
    }

    /**
     * whether the entity can breathe underwater according to its entity behavior; this
     * does not account for breathing equipment.
     *
     * @return if the entity can breathe in water
     * @since 1.8.4
     */
    public boolean canBreatheInWater() {
        return base.canBreatheUnderwater();
    }

    /**
     * whether the entity is configured to discard friction; this reports a flag, not its
     * current motion.
     *
     * @return if the entity has no drag
     * @since 1.8.4
     */
    public boolean hasNoDrag() {
        return base.shouldDiscardFriction();
    }

    /**
     * whether the entity is configured to ignore gravity; this reports a flag, not
     * whether it is currently falling.
     *
     * @return if the entity has no gravity
     * @since 1.8.4
     */
    public boolean hasNoGravity() {
        return base.isNoGravity();
    }

    /**
     * @param target entity to test
     * @return whether the game permits this entity to attack {@code target}
     * @since 1.8.4
     */
    private boolean canTarget(LivingEntity target) {
        return base.canAttack(target);
    }

    /**
     * whether the game permits this entity to attack the target. This is an eligibility
     * check, not an indication that the entity wants to attack; players are not attackable
     * in peaceful difficulty.
     *
     * @param target the target entity
     * @return if the entity can target a target entity
     * @since 1.8.4
     */
    public boolean canTarget(LivingEntityHelper<?> target) {
        return canTarget(target.getRaw());
    }

    /**
     * whether the entity passes the game's enemy-visibility check: it must not be
     * invulnerable, must not be a spectator, and must be alive. This is not a guarantee
     * that every damage source can hurt it.
     *
     * @return if the entity can take damage
     * @since 1.8.4
     */
    public boolean canTakeDamage() {
        return base.canBeSeenAsEnemy();
    }

    /**
     * whether the entity is alive and not a spectator. Unlike {@link #canTakeDamage()},
     * this does not also check invulnerability.
     *
     * @return if the entity is part of the game (is alive and not spectator)
     * @since 1.8.4
     */
    public boolean isPartOfGame() {
        return base.canBeSeenByAnyone();
    }

    /**
     * whether the entity is in spectator mode.
     *
     * @return if the entity is in spectator
     * @since 1.8.4
     */
    public boolean isSpectator() {
        return base.isSpectator();
    }

    /**
     * Returns whether this entity's type belongs to the game's undead entity-type tag.
     *
     * @return if the entity is undead
     * @since 1.8.4
     */
    public boolean isUndead() {
        return base.getType().builtInRegistryHolder().tags().anyMatch(e -> EntityTypeTags.UNDEAD.location().equals(e.location()));
    }

    /**
     * Returns the bow draw power for the item in the entity's main hand, or {@code 0} if it is not a bow.
     *
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
     * Returns the remaining use duration of the entity's currently used item, in ticks; zero when not using an item.
     *
     * @return the ticks left of using the held item
     * @since 1.9.0
     */
    public int getItemUseTimeLeft() {
        return base.getUseItemRemainingTicks();
    }

    /**
     * Returns the entity's baby state as reported by Minecraft.
     *
     * @return {@code true} if the entity is a baby, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBaby() {
        return base.isBaby();
    }

    /**
     * Checks line of sight using only the three-point raycast; it does not perform the extended search available through {@link #canSeeEntity(EntityHelper, boolean)}.
     *
     * @param entity the entity to check line of sight to
     * @return {@code true} if the player has line of sight to the specified entity, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canSeeEntity(EntityHelper<?> entity) {
        return canSeeEntity(entity, true);
    }

    /**
     * Checks three target points first. If {@code simpleCast} is false and those fail, also raycasts to four side positions at 0.1-block vertical intervals, starting at the target's base; the upper remainder may be unsampled. These checks use visual-block raycasts and ignore fluids.
     *
     * @param entity     the entity to check line of sight to
     * @param simpleCast whether to use a simple raycast or a more complex one
     * @return {@code true} if a tested ray misses visual-block collisions
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
