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
 * anything that is alive: a mob, a player, an armour stand. It adds the things a living
 * thing has to a plain entity, which are health, equipment, the effects on it and the
 * handful of flags the game keeps about whether it is fighting, sleeping or swimming.
 * <p>
 * Everything here is a read on the client. A living thing decides for itself on the
 * server, so what these say is the last thing the client was told rather than a live
 * decision, and nothing on this class changes any of it.
 * <p>
 * A script narrows to this with {@code asLiving()} on an entity helper, which is a cast
 * rather than a check: it answers with the right type or throws, so it is only safe on
 * an entity that really is alive. Filtering by type first with
 * {@code World.getEntities(String...)} is what makes that safe, because a plain
 * {@code getEntities()} list holds dropped items and projectiles too. There is no
 * narrowing for the subclasses below it: a mob helper, a player helper and a villager
 * helper all extend this, so casting to one of those is a script's own business.
 * example:
     * <pre>
 * // the living things within twenty blocks, and their health
 * const mobs = World.getEntities(20, "zombie", "skeleton", "creeper", "cow");
 * if (mobs !== null) {
 *   for (const entity of mobs) {
 *     // every one of these really is alive, so the cast is safe
 *     const alive = entity.asLiving();
 *     Chat.log(`${alive.getType()}: ${alive.getHealth()} of ${alive.getMaxHealth()}`);
 *   }
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class LivingEntityHelper<T extends LivingEntity> extends EntityHelper<T> {

    public LivingEntityHelper(T e) {
        super(e);
    }

    /**
     * the effects currently on the entity, as a list built at the moment of the call.
     * An effect that has run out is not in here, and neither is one whose remaining time
     * has been set to nothing, so the list is what is affecting the entity right now.
     * <p>
     * The list is a new one each call, so writing to it changes nothing.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const effects = player.getStatusEffects();
     *   Chat.log(`${effects.size()} effect(s) on the player`);
     *   for (const effect of effects) {
     *     Chat.log(`  ${effect.getId()}`);
     *   }
     * }
     * </pre>
     *
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
     * whether this entity is the sort of thing the effect can be applied to at all.
     * This is about the entity and not about what is on it: a zombie cannot be given
     * every effect regardless of how it was hurt, so asking here answers about the kind
     * of entity rather than about the current state.
     * <p>
     * It says nothing about whether the effect is on the entity right now; that is
     * {@link #hasStatusEffect(String)}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // an effect the player already has, asked about the player
     *   const effects = player.getStatusEffects();
     *   if (effects.size() > 0) {
     *     const effect = effects.get(0);
     *     Chat.log(`the player can have ${effect.getId()}: ${player.canHaveStatusEffect(effect)}`);
     *   }
     * }
     * </pre>
     *
     * @param effect the status effect
     * @return if the entity can have a certain status effect
     * @since 1.8.4
     */
    public boolean canHaveStatusEffect(StatusEffectHelper effect) {
        return canHaveStatusEffect(effect.getRaw());
    }

    /**
     * whether the entity has the named effect on it right now, found by looking through
     * the effects that are active rather than by asking the game directly.
     * <p>
     * For client side entities, excluding the player, this will most likely return {@code false}
     * even if the entity has the effect, as effects are not synced to the client. The
     * effect list on an entity the client has been sent is whatever the client was told,
     * and for a mob that is often nothing at all. The local player is the exception,
     * because the client is told about its own effects.
     * <p>
     * An effect that is on the entity but has run out is not counted, and an effect that
     * the game has tagged as not applying to this entity is counted if it is there.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.hasStatusEffect("minecraft:regeneration")) {
     *     Chat.log("the player is regenerating");
     *   }
     *   // the namespace may be left off
     *   Chat.log(`has night vision: ${player.hasStatusEffect("night_vision")}`);
     * }
     * </pre>
     *
     * @param id the id of the status effect to look for
     * @return {@code true} if the entity has the specified status effect, {@code false} otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public boolean hasStatusEffect(String id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(RegistryHelper.parseIdentifier(id));
        return base.getActiveEffects().stream().anyMatch(statusEffectInstance -> statusEffectInstance.getEffect().value().equals(effect));
    }

    /**
     * whether the entity is holding the named item in either hand. Air counts as an item
     * here, so naming air asks whether the hands are empty, and anything that is not a
     * registered item answers {@code false} rather than throwing.
     * <p>
     * The name is an item id, and the namespace may be left off and is read as
     * {@code minecraft}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.isHolding("shield")) {
     *     Chat.log("a shield is up in one of the hands");
     *   }
     *   // an empty hand
     *   Chat.log(`holding nothing: ${player.isHolding("air")}`);
     * }
     * </pre>
     *
     * @param item the id of the item to look for
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
     * the item in the entity's main hand. A mob that holds nothing gives an empty stack
     * rather than {@code null}, so a caller that wants to know whether there is anything
     * there is asking {@code isEmpty()} on the result.
     * <p>
     * This is the slot the game uses for the hand it swings and the one it puts a
     * weapon in, and it is the hand the bow pull progress and the mining speed are read
     * against.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const held = player.getMainHand();
     *   if (!held.isEmpty()) {
     *     Chat.log(`main hand: ${held.getCount()} ${held.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the entity's main hand.
     * @since 1.2.7
     */
    public ItemStackHelper getMainHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    /**
     * the item in the entity's off hand, which is the second hand slot. It is empty
     * unless something is being held there, and a shield or a totem is what the game
     * puts there.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const off = player.getOffHand();
     *   Chat.log(off.isEmpty() ? "off hand empty" : `off hand: ${off.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the entity's off hand.
     * @since 1.2.7
     */
    public ItemStackHelper getOffHand() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    /**
     * the item in the entity's helmet slot, which is empty unless something is worn
     * there. The slot exists whether or not it is filled, so an empty result is the
     * answer rather than a failure.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const head = player.getHeadArmor();
     *   Chat.log(head.isEmpty() ? "no helmet" : `wearing ${head.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the entity's head armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getHeadArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.HEAD));
    }

    /**
     * the item in the entity's chestplate slot, which is the body armour piece. The
     * armour value and toughness the entity gets from all its pieces are read from the
     * entity rather than from the pieces.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`${player.getArmor()} armour, ${player.getArmorToughness()} toughness`);
     * }
     * </pre>
     *
     * @return the item in the entity's chest armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getChestArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.CHEST));
    }

    /**
     * the item in the entity's leggings slot.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const legs = player.getLegArmor();
     *   Chat.log(legs.isEmpty() ? "no leggings" : `wearing ${legs.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the entity's leg armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getLegArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.LEGS));
    }

    /**
     * the item in the entity's boots slot.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const feet = player.getFootArmor();
     *   Chat.log(feet.isEmpty() ? "barefoot" : `wearing ${feet.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the entity's foot armor slot.
     * @since 1.2.7
     */
    public ItemStackHelper getFootArmor() {
        return new ItemStackHelper(base.getItemBySlot(EquipmentSlot.FEET));
    }

    /**
     * the entity's health as it is right now, which counts down from
     * {@link #getMaxHealth()} rather than up from zero. It is a plain number rather than
     * a count of half hearts, so the value the game shows is this doubled.
     * <p>
     * An entity that has been killed reads zero, and one that has never been hurt reads
     * its maximum. The health of a mob the client has been sent is the last the client
     * was told, because the server owns it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`${player.getHealth()} of ${player.getMaxHealth()}`);
     * }
     * // a mob that is more than half hurt
     * const mobs = World.getEntities(20, "zombie");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const zombie = entity.asLiving();
     *     // half of the maximum, worked out rather than compared with 'less than'
     *     const half = zombie.getMaxHealth() / 2;
     *     if (half > zombie.getHealth()) {
     *       Chat.log(`a hurt zombie at ${zombie.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return entity's health
     * @since 1.3.1
     */
    public float getHealth() {
        return base.getHealth();
    }

    /**
     * the health the entity can be taken down to, which is the number
     * {@link #getHealth()} is measured against. It comes from the entity's own maximum
     * health attribute, so it is whatever the gamemode, the equipment and any effect on
     * the entity have made it rather than a figure from the kind of entity.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const max = player.getMaxHealth();
     *   Chat.log(`${player.getHealth()} of ${max}, `
     *     + `${Math.round(player.getHealth() / max * 100)}%`);
     * }
     * </pre>
     *
     * @return entity's max health
     * @since 1.6.5
     */
    public float getMaxHealth() {
        return base.getMaxHealth();
    }

    /**
     * the health the entity is carrying on top of its own, which is what a golden apple
     * or an absorption effect gives it. Damage comes off this before it comes off the
     * entity's own health, so this is the buffer rather than part of the total.
     * <p>
     * It is empty on an entity with nothing on it, so a number here is a bonus rather
     * than a replacement.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const extra = player.getAbsorptionHealth();
     *   if (extra > 0) {
     *     // the buffer, which is on top of the health itself
     *     Chat.log(`${player.getHealth()} health plus ${extra} absorption`);
     *   }
     * }
     * </pre>
     *
     * @return the entity's absorption amount.
     * @since 1.8.4
     */
    public float getAbsorptionHealth() {
        return base.getAbsorptionAmount();
    }

    /**
     * the entity's armour value, which is the flat reduction the game applies to damage
     * and which comes from all the armour pieces it is wearing at once. The figure is
     * read off the entity's armour attribute rather than worked out from the pieces, so
     * an effect or an attribute change is included.
     * <p>
     * A player with nothing on has none of it, and it climbs as pieces are put on.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`${player.getArmor()} armour, ${player.getArmorToughness()} toughness`);
     * }
     * </pre>
     *
     * @return the entity's armor value.
     * @since 1.8.4
     */
    public int getArmor() {
        return base.getArmorValue();
    }

    /**
     * the entity's armour toughness, which is the second half of the armour system: it
     * softens the damage that gets past the flat reduction, and matters more against
     * damage that is already large. It is read off the entity's attribute, so an effect
     * that changes it is included.
     * <p>
     * It is a fraction rather than a whole number, and a bare player has none of it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the two halves of the armour system
     *   Chat.log(`${player.getArmor()} armour and ${player.getArmorToughness()} toughness`);
     * }
     * </pre>
     *
     * @return the entity's armor toughness.
     * @since 2.1.0
     */
    public double getArmorToughness() {
        return base.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    /**
     * the entity's maximum health, which is the same number as
     * {@link #getMaxHealth()}. The two are kept because the name this used to have is
     * the one a script is likely to reach for, and the value behind both is the
     * entity's own maximum health attribute on this version of the game.
     * <p>
     * There is no longer a separate figure for the health the entity was spawned with,
     * so this does not tell a script what an entity's health was before anything
     * changed it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the same figure under both names
     *   Chat.log(`${player.getDefaultHealth()} == ${player.getMaxHealth()}`);
     * }
     * </pre>
     *
     * @return the entity's default health.
     * @since 1.8.4 (returns float since 2.1.1)
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
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "skeleton", "creeper");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const alive = entity.asLiving();
     *     for (const tag of alive.getMobTags()) {
     *       Chat.log(`${alive.getType()} is in ${tag}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the tags on this entity's type
     * @since 1.9.1
     */
    @DocletReplaceReturn("JavaList<MobTag>")
    public List<String> getMobTags() {
        return base.getType().builtInRegistryHolder().tags().map(TagKey::location).map(ResourceLocation::toString).toList();
    }

    /**
     * whether the entity has a sleeping position, which is what the game reads to know
     * an entity is asleep in a bed. A player in a bed is the case that really answers
     * this; a mob answers {@code true} only once it has actually got into one, which a
     * villager given a bed does and most other mobs never do.
     * <p>
     * This says whether the entity is in a bed at all rather than whether it has been
     * there long enough, which on a player is a separate question.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const villager = entity.asVillager();
     *     if (villager.isSleeping()) {
     *       Chat.log(`a villager is in a bed at ${villager.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity is in a bed.
     * @since 1.2.7
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * whether the entity has an elytra out and is gliding. An entity with an elytra in
     * its chest slot that is not wearing it, or is wearing it and is on the ground, is
     * not gliding, so this is the whole of it rather than a check of the equipment.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "pig", "player");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const alive = entity.asLiving();
     *     if (alive.isFallFlying()) {
     *       Chat.log(`gliding at ${alive.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity has elytra deployed
     * @since 1.5.0
     */
    public boolean isFallFlying() {
        return base.isFallFlying();
    }

    /**
     * whether the entity is standing on something. This is the flag rather than a check
     * of what is under the entity, so an entity that has just jumped reads {@code false}
     * and one that is falling reads {@code false} even part way through a descent.
     * <p>
     * An entity that is gliding reads {@code false} as well, since it is in the air on
     * purpose.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the game slows mining down in the air, and this is the flag it checks
     *   Chat.log(`on the ground: ${player.isOnGround()}`);
     * }
     * </pre>
     *
     * @return if the entity is on the ground
     * @since 1.8.4
     */
    public boolean isOnGround() {
        return base.onGround();
    }

    /**
     * whether the entity can breathe underwater, which is a property of the kind of
     * entity rather than of what it is doing: the game decides it from a tag on the
     * entity type, so a drowned answers {@code true} and a zombie {@code false}.
     * <p>
     * A player wearing a helmet that lets them breathe answers the same as a player
     * without one, because this reads the entity type and not the equipment.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "drowned", "zombie", "guardian");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const alive = entity.asLiving();
     *     Chat.log(`${alive.getType()} breathes underwater: ${alive.canBreatheInWater()}`);
     *   }
     * }
     * </pre>
     *
     * @return if the entity can breathe in water
     * @since 1.8.4
     */
    public boolean canBreatheInWater() {
        return base.canBreatheUnderwater();
    }

    /**
     * whether the entity has been set to discard its friction, which is a flag rather
     * than a check of how it is moving. An entity with it on slides rather than gripping
     * what it is standing on, which is how a pushed entity is made to carry on.
     * <p>
     * Nothing on this class sets the flag, so a script reads it rather than turning it
     * on.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.hasNoDrag()) {
     *     Chat.log("sliding rather than gripping");
     *   }
     * }
     * </pre>
     *
     * @return if the entity has no drag
     * @since 1.8.4
     */
    public boolean hasNoDrag() {
        return base.shouldDiscardFriction();
    }

    /**
     * whether the entity is set to ignore gravity, which is a flag rather than a check
     * of whether it is falling. An entity with it on holds still in the air unless
     * something moves it, which is how a display entity or a piece of custom machinery
     * stays put.
     * <p>
     * Nothing on this class sets the flag, so a script reads it rather than turning it
     * on.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "skeleton", "arrow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // filtered to living things, so the cast is safe
     *     const alive = entity.asLiving();
     *     if (alive.hasNoGravity()) {
     *       Chat.log(`floating at ${alive.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
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
     * whether this entity is allowed to attack the one given, which is the game's own
     * check and covers the things that stop an attack rather than the ones that make it
     * unlikely. A spectator, a dead entity and one this entity is already riding are all
     * refused, and so is a player who has switched the game into peaceful.
     * <p>
     * This is about whether an attack is permitted rather than about whether the entity
     * wants to make one, so a mob that has no target at all still answers {@code true}
     * for a mob it is allowed to hit.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const mobs = World.getEntities(20, "zombie") ?? [];
     * if (player !== null) {
     *   for (const entity of mobs) {
     *     const zombie = entity.asLiving();
     *     if (zombie.canTarget(player)) {
     *       Chat.log("a zombie is allowed to hit the player");
     *     }
     *   }
     * }
     * </pre>
     *
     * @param target the target entity
     * @return if the entity can target a target entity
     * @since 1.8.4
     */
    public boolean canTarget(LivingEntityHelper<?> target) {
        return canTarget(target.getRaw());
    }

    /**
     * whether the game would let this entity be hurt right now, which is the game's own
     * test rather than a guess. It is {@code false} when the entity is invulnerable, a
     * spectator, or already dead, and {@code true} otherwise.
     * <p>
     * It is a check of the entity's state rather than of the damage about to arrive, so
     * a damage source the game treats as bypassing invulnerability can still land on an
     * entity that answers {@code false} here.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "skeleton", "cow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const alive = entity.asLiving();
     *     // the mobs the game would currently let be hurt
     *     if (alive.canTakeDamage()) {
     *       Chat.log(`${alive.getType()} at ${alive.getHealth()} health`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity can take damage
     * @since 1.8.4
     */
    public boolean canTakeDamage() {
        return base.canBeSeenAsEnemy();
    }

    /**
     * whether the entity is still part of the game, which is the two conditions of
     * being alive and not being a spectator together. A dead entity and a spectating one
     * both answer {@code false} here, and either one on its own is enough.
     * <p>
     * This is a narrower question than {@link #canTakeDamage()}, which adds invulnerability
     * on top of these two.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "skeleton", "cow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // filtered to living things, so the cast is safe
     *     const alive = entity.asLiving();
     *     if (!alive.isPartOfGame()) {
     *       Chat.log(`${alive.getType()} is not in play`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity is part of the game (is alive and not spectator)
     * @since 1.8.4
     */
    public boolean isPartOfGame() {
        return base.canBeSeenByAnyone();
    }

    /**
     * whether the entity is spectating rather than playing, which is the mode a player
     * is in after dying. A spectator is not part of the game and cannot be hurt, which
     * is the other half of what {@link #isPartOfGame()} asks.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.isSpectator()) {
     *     Chat.log("watching rather than playing");
     *   }
     * }
     * </pre>
     *
     * @return if the entity is in spectator
     * @since 1.8.4
     */
    public boolean isSpectator() {
        return base.isSpectator();
    }

    /**
     * whether the entity's type is in the game's undead tag, which is the group that
     * holds the zombies, the skeletons and the rest of the lot. This is a property of
     * the kind of entity rather than of this one, so every zombie answers the same way
     * however it has been changed.
     * <p>
     * The answer comes from {@link #getMobTags()}, which is the list being looked
     * through.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "zombie", "skeleton", "cow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // filtered to living things, so the cast is safe
     *     const alive = entity.asLiving();
     *     if (alive.isUndead()) {
     *       Chat.log(`${alive.getType()} at ${alive.getPos()} is undead`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity is undead
     * @since 1.8.4
     */
    public boolean isUndead() {
        return base.getType().builtInRegistryHolder().tags().anyMatch(e -> EntityTypeTags.UNDEAD.location().equals(e.location()));
    }

    /**
     * how far the entity has pulled a bow back, as a fraction from nothing at all up to
     * fully drawn. It reaches one after twenty ticks of holding, and the curve is the
     * game's own rather than a straight line, so half drawn is well short of half.
     * <p>
     * This is {@code 0} for anything not holding a bow in its main hand, and a mob
     * holding one that is not using it also reads zero because nothing has been held
     * for any ticks.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = player.getBowPullProgress();
     *   if (draw > 0) {
     *     Chat.log(`drawn back ${Math.round(draw * 100)}%`);
     *   }
     * }
     * </pre>
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
     * how many ticks are left of using the item the entity is holding, counted down
     * from the item's own use duration. It is zero for anything not currently using
     * something, so an entity with a bow in its hand that is not drawing it reads zero
     * rather than the bow's full duration.
     * <p>
     * The number counts down as the item is used, so a second read of the same entity
     * gives a smaller one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const left = player.getItemUseTimeLeft();
     *   if (left > 0) {
     *     Chat.log(`using the held item for ${left} more tick(s)`);
     *   }
     * }
     * </pre>
     *
     * @return the ticks left of using the held item
     * @since 1.9.0
     */
    public int getItemUseTimeLeft() {
        return base.getUseItemRemainingTicks();
    }

    /**
     * whether the entity is a baby, which is the game's own growth flag rather than a
     * check of the model. A baby is smaller, moves differently and grows into an adult,
     * and the flag is what all of that hangs on.
     * <p>
     * An adult mob for which the question makes no sense answers {@code false} rather
     * than throwing, so this is safe to ask of anything.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "pig", "cow", "sheep");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // all of these are animals, so the cast is safe
     *     const animal = entity.asLiving();
     *     if (animal.isBaby()) {
     *       Chat.log(`a baby ${animal.getType()} at ${animal.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity is a baby, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBaby() {
        return base.isBaby();
    }

    /**
     * whether this entity can see the one given, which is a line of sight check through
     * the world rather than a check of distance or of either entity's state. Three rays
     * are cast from this entity's eyes to three points on the other: its eyes, half way
     * up it and its feet, and a clear one to any of them is a {@code true}.
     * <p>
     * Nothing blocks a ray but a block that stops it, so a target behind glass is seen
     * and one behind a wall is not. Fluids never block here, and the ray stops at the
     * first thing in the way rather than at the target, so an entity standing behind
     * another is not seen through it.
     * <p>
     * This is the simpler of the two forms: it tries the three points and stops there.
     * The other one goes on to look for a gap, which finds a target that is mostly
     * hidden.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const mobs = World.getEntities(20, "zombie") ?? [];
     * if (player !== null) {
     *   for (const entity of mobs) {
     *     const zombie = entity.asLiving();
     *     if (zombie.canSeeEntity(player)) {
     *       Chat.log(`a zombie at ${zombie.getPos()} has the player in sight`);
     *     }
     *   }
     * }
     * </pre>
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
     * whether this entity can see the one given, with a choice about how hard to look.
     * <p>
     * With {@code simpleCast} on, this is the three-ray check and nothing more. With it
     * off, a target the three rays all miss is looked for again: the target's bounding
     * box is walked upwards in steps of a tenth of a block, and four rays are cast per
     * step, at the four vertical faces of the box. That finds a target that is standing
     * behind cover with a gap in it, which the simple form misses entirely. The search
     * runs over the target's own height, so a tall target costs more than a short one.
     * <p>
     * Nothing blocks a ray but a block that stops it, and fluids never block here, so
     * this sees through glass on either form.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const mobs = World.getEntities(30, "creeper") ?? [];
     * if (player !== null) {
     *   for (const entity of mobs) {
     *     const creeper = entity.asLiving();
     *     // the careful form, which looks for a gap as well
     *     if (creeper.canSeeEntity(player, false)) {
     *       Chat.log(`a creeper at ${creeper.getPos()} can see the player`);
     *     }
     *   }
     * }
     * </pre>
     *
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
