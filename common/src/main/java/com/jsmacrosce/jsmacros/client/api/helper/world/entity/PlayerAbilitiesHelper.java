package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.player.Abilities;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * the abilities the game hands a player: whether they can fly, whether they can be
 * hurt, how fast they move and whether they are allowed to change the world.
 * <p>
 * This is a read and a write on the abilities block itself rather than on the player,
 * and the distinction matters. The server sends the player a set of abilities and the
 * client keeps them, so a change made through one of these setters is a client-side
 * one. The server still holds the values it thinks are true, and the next time it sends
 * a fresh set, which it does on joining, on respawning and when the gamemode changes,
 * that set is what replaces whatever was written here. What a change does reliably
 * affect in the meantime is the client's own behaviour, so setting a flag here changes
 * how the local player moves.
 * <p>
 * A helper comes from {@link PlayerEntityHelper#getAbilities()} on the player being
 * asked about, and it is a wrapper on the same block rather than a copy, so two reads
 * of the same player see the same values.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const abilities = player.getAbilities();
 *   if (abilities.getCreativeMode()) {
 *     // the game reads the walk speed as a fraction of its normal value
 *     abilities.setWalkSpeed(0.4);
 *     Chat.log(`walk speed is now ${abilities.getWalkSpeed()}`);
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.3
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class PlayerAbilitiesHelper extends BaseHelper<Abilities> {

    public PlayerAbilitiesHelper(Abilities a) {
        super(a);
    }

    /**
     * whether the player is set to take no damage at all. This is the flag a gamemode
     * uses for a player it does not want hurt: the game turns damage away before
     * applying it, and the same flag is what lets a player eat whatever is to hand
     * without being hungry first.
     * <p>
     * It is not quite absolute. A damage source the game has tagged as bypassing
     * invulnerability gets through anyway, so this is a flag the game consults rather
     * than a wall. There is no setter here either, so a script reads it rather than
     * turning it on; it is set by whatever put the player in the gamemode.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (abilities.getInvulnerable()) {
     *     Chat.log("nothing is going to hurt you");
     *   }
     * }
     * </pre>
     *
     * @return whether the player can be damaged.
     * @since 1.0.3
     */
    public boolean getInvulnerable() {
        return base.invulnerable;
    }

    /**
     * whether the player is in flight right now, which is the gamemode's flying flag
     * rather than a question of whether the player is in the air. A player who jumped is
     * not flagged, and a player who is flagged can still be falling.
     * <p>
     * This is writable through {@link #setFlying(boolean)}, and setting it does not move
     * the player: it is the same switch the gamemode uses, so the flight itself is
     * worked out by the game on the next tick.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   // the game only lets a player take off if flying is allowed first
     *   abilities.setAllowFlying(true).setFlying(true);
     *   Chat.log(`flying is set, and it now reads ${abilities.getFlying()}`);
     * }
     * </pre>
     *
     * @return if the player is currently flying.
     * @since 1.0.3
     */
    public boolean getFlying() {
        return base.flying;
    }

    /**
     * whether the player is allowed to start flying at all, which is the gamemode's
     * allow-fly flag and is separate from whether they are flying now. Both have to be
     * on for the player to be in the air: this one lets them take off, and
     * {@link #getFlying()} says whether they have.
     * <p>
     * The field this reads is marked deprecated on the game's side, which is worth
     * knowing before relying on it, and the setter writes the same field. What the game
     * makes of the flag is its own business, so turning this on is a change to the
     * client's copy rather than a guarantee that the player will be able to fly.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (abilities.getAllowFlying()) {
     *     Chat.log("flight is on offer, and in use right now is "
     *       + abilities.getFlying());
     *   }
     * }
     * </pre>
     *
     * @return if the player is allowed to fly.
     * @since 1.0.3
     */
    public boolean getAllowFlying() {
        return base.mayfly;
    }

    /**
     * whether the player has the creative flag on the abilities block, which is what a
     * creative gamemode sets. It is a flag rather than a gamemode reading in its own
     * right: the game asks {@code Player.isCreative()} when it wants the gamemode, and
     * that is a separate question answered from the player's game mode rather than from
     * here.
     * <p>
     * What the game actually uses this flag for is narrower than the word creative
     * suggests. It is what gives a player infinite materials in the inventory, what
     * stops items dropping out of an emptied hand, what stops <b>blocks</b> dropping
     * items when they are broken, and one half of what lets a player use an operator
     * command block, the other half being a permission the server holds.
     * <p>
     * The block part of that is about blocks rather than about mobs: it is consulted
     * only where a block would give its items up, so a creative player who kills a mob
     * still gets the mob's drops, and nothing about the death loot of an entity is
     * gated on this flag. The operator command block half is the game's own
     * {@code canUseGameMasterBlocks()} on the player, which asks this flag and the
     * server's gamemaster permission together, so either one being off is enough to
     * refuse.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (abilities.getCreativeMode()) {
     *     // the walk speed setter takes a double, whatever the reader gives back
     *     abilities.setWalkSpeed(0.2);
     *     Chat.log(`creative, walking at ${abilities.getWalkSpeed()}`);
     *   }
     * }
     * </pre>
     *
     * @return if the player is in creative.
     * @since 1.0.3
     */
    public boolean getCreativeMode() {
        return base.instabuild;
    }

    /**
     * Even if this method returns true, the player may not be able to modify the world due to other
     * restrictions such as plugins and mods. Modifying the world includes, placing, breaking or
     * interacting with blocks.
     * <p>
     * The two are not the same flag: the creative flag is what a creative gamemode sets,
     * and this is the separate may-build flag, which starts out on and is what a server
     * or a plugin turns off to stop a player changing the world. It is checked on its own
     * rather than as part of the creative reading, so it is the one that governs placing
     * in adventure mode, where a block is placed only when the flag is on and the item
     * in hand is one that supports it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (abilities.getCreativeMode()) {
     *     if (abilities.canModifyWorld()) {
     *       Chat.log("creative, and allowed to change the world");
     *     } else {
     *       Chat.log("creative, but something has blocked world changes");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the player is allowed to modify the world, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canModifyWorld() {
        return base.mayBuild;
    }

    /**
     * sets the flying state on the abilities block. It is the flag rather than a
     * movement, so the player is not moved by this; the game works out the flight on
     * the next tick, and only if {@link #getAllowFlying()} is also on.
     * <p>
     * A change here is a local one, as the ones on this class are: the server keeps the
     * abilities it thinks the player has, and the next set it sends replaces this.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (!abilities.getFlying()) {
     *     // stopping first, so the game is willing to let them take off
     *     abilities.setFlying(true);
     *     Chat.log("set to flying");
     *   }
     * }
     * </pre>
     *
     * @param b whether the flying state should be set
     * @return self for chaining.
     * @since 1.0.3
     */
    public PlayerAbilitiesHelper setFlying(boolean b) {
        base.flying = b;
        return this;
    }

    /**
     * sets whether the player is allowed to start flying. This is the permission half of
     * flying rather than the state half: with it on and {@link #getFlying()} off the
     * player may take off, and with {@link #getFlying()} on as well they are in the air.
     * <p>
     * Like the other setters here, this writes the client's copy of the abilities and
     * not the server's, so it is the client's own idea of what the player may do that
     * changes. The field it writes is marked deprecated on the game's side.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   // both halves, in the order the game checks them
     *   abilities.setAllowFlying(true);
     *   abilities.setFlying(true);
     *   Chat.log(`allowed: ${abilities.getAllowFlying()}, flying: ${abilities.getFlying()}`);
     * }
     * </pre>
     *
     * @param b whether flying should be allowed
     * @return self for chaining.
     * @since 1.0.3
     */
    public PlayerAbilitiesHelper setAllowFlying(boolean b) {
        base.mayfly = b;
        return this;
    }

    /**
     * the multiplier on the player's flight speed, where the game's own value is
     * {@code 0.05} and the number the game moves the player at is that multiplied into
     * its normal flying pace. It is a speed rather than a rate of change, so a bigger
     * number is faster and there is no unit to keep track of.
     * <p>
     * Nothing here clamps it, so a value outside the range the game expects is stored
     * as given.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   Chat.log(`flying at ${abilities.getFlySpeed()}`);
     *   // the setter takes a double, and the reader gives back a float
     *   abilities.setFlySpeed(0.2);
     *   Chat.log(`now ${abilities.getFlySpeed()}`);
     * }
     * </pre>
     *
     * @return the player fly speed multiplier.
     * @since 1.0.3
     */
    public float getFlySpeed() {
        return base.getFlyingSpeed();
    }

    /**
     * sets the multiplier on the player's flight speed. The value is cast down to a
     * float on the way in, so a double that will not fit loses precision rather than
     * being refused.
     * <p>
     * Like the other setters here this is a local change to the client's copy of the
     * abilities, and the speed only applies while the player is actually flying.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   player.getAbilities().setFlySpeed(0.1);
     * }
     * </pre>
     *
     * @param flySpeed the new flight speed multiplier
     * @return self for chaining.
     * @since 1.0.3
     */
    public PlayerAbilitiesHelper setFlySpeed(double flySpeed) {
        base.setFlyingSpeed((float) flySpeed);
        return this;
    }

    /**
     * the multiplier on the player's walking speed, where the game's own value is
     * {@code 0.1} and one is the pace of a player walking in a normal gamemode. The
     * number is read straight out of the abilities block and is what the game moves the
     * player at, so a bigger number is faster.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   Chat.log(`walking at ${abilities.getWalkSpeed()}`);
     *   // sprinting, jumps and being in water all change the pace on top of this
     *   abilities.setWalkSpeed(0.3);
     *   Chat.log(`now ${abilities.getWalkSpeed()}`);
     * }
     * </pre>
     *
     * @return the player's walk speed.
     * @since 1.8.4
     */
    public float getWalkSpeed() {
        return base.getWalkingSpeed();
    }

    /**
     * sets the multiplier on the player's walking speed. The value is cast down to a
     * float on the way in and is not clamped, so a number the game would not normally
     * use is stored as given.
     * <p>
     * This is a local change to the client's copy of the abilities, so the server can
     * put its own value back on the next packet that carries them.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   // back to the game's own value
     *   abilities.setWalkSpeed(0.1);
     *   Chat.log(`back to ${abilities.getWalkSpeed()}`);
     * }
     * </pre>
     *
     * @param speed the new walk speed
     * @return self for chaining.
     * @since 1.8.4
     */
    public PlayerAbilitiesHelper setWalkSpeed(double speed) {
        base.setWalkingSpeed((float) speed);
        return this;
    }

    @Override
    public String toString() {
        return "PlayerAbilitiesHelper:{"
                + "\"invulnerable\": " + base.invulnerable
                + ", \"creativeMode\": " + base.instabuild
                + ", \"modifyWorld\": " + base.mayBuild
                + ", \"flying\": " + base.flying
                + ", \"allowFlying\": " + base.mayfly
                + ", \"flySpeed\": " + base.getFlyingSpeed()
                + ", \"walkSpeed\": " + base.getWalkingSpeed()
                + "}";
    }

}
