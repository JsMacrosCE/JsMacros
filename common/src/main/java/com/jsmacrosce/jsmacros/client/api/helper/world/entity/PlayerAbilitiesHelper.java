package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.player.Abilities;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Reads and changes the local player's abilities, including flight, invulnerability,
 * movement speed, and permission to build. Changes write directly to the wrapped
 * abilities object; server updates may subsequently replace them.
 * <p>
 * Obtain the helper from {@link PlayerEntityHelper#getAbilities()}.
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

    /**
     * Wraps the supplied abilities object without copying it or selecting a player.
     *
     * @param a the raw player abilities object to wrap
     */
    public PlayerAbilitiesHelper(Abilities a) {
        super(a);
    }

    /**
     * Returns the abilities block's invulnerability flag.
     *
      * @return {@code true} when the invulnerability flag is set; damage sources that
      * bypass invulnerability may still cause damage.
     * @since 1.0.3
     */
    public boolean getInvulnerable() {
        return base.invulnerable;
    }

    /**
     * Returns the abilities block's flying-state flag; this does not indicate merely
     * whether the player is airborne.
     *
     * @return if the player is currently flying.
     * @since 1.0.3
     */
    public boolean getFlying() {
        return base.flying;
    }

    /**
     * Returns the abilities block's permission-to-fly flag, distinct from
     * {@link #getFlying()}.
     *
     * @return if the player is allowed to fly.
     * @since 1.0.3
     */
    public boolean getAllowFlying() {
        return base.mayfly;
    }

    /**
     * Returns the abilities block's instant-build flag; this is not itself a query
     * of the player's current game mode.
     *
     * @return if the player is in creative.
     * @since 1.0.3
     */
    public boolean getCreativeMode() {
        return base.instabuild;
    }

    /**
     * Returns the abilities block's may-build flag. Other game or mod restrictions
     * may still prevent a particular world interaction.
     *
     * @return {@code true} if the player is allowed to modify the world, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canModifyWorld() {
        return base.mayBuild;
    }

    /**
     * Sets the flying-state flag in the abilities block. This changes the flag, not
     * the player's position; server updates may replace the local value.
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
     * Sets the permission-to-fly flag in the wrapped abilities object.
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
     * Returns the wrapped abilities object's flight-speed value. Its default is {@code 0.05}.
     *
     * @return the player fly speed multiplier.
     * @since 1.0.3
     */
    public float getFlySpeed() {
        return base.getFlyingSpeed();
    }

    /**
     * Sets the wrapped abilities object's flight-speed value, converting the input to a float.
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
     * Returns the wrapped abilities object's walking-speed value. Its default is {@code 0.1}.
     *
     * @return the player's walk speed.
     * @since 1.8.4
     */
    public float getWalkSpeed() {
        return base.getWalkingSpeed();
    }

    /**
     * Sets the wrapped abilities object's walking-speed value, converting the input to a float.
     *
     * @param speed the new walk speed
     * @return self for chaining.
     * @since 1.8.4
     */
    public PlayerAbilitiesHelper setWalkSpeed(double speed) {
        base.setWalkingSpeed((float) speed);
        return this;
    }

    /**
     * Returns a diagnostic representation of the wrapped abilities' flags and movement speeds.
     *
     * @return a diagnostic representation of the wrapped abilities
     */
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
