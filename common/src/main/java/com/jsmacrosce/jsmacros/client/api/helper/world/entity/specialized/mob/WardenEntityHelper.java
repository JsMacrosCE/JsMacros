package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.warden.Warden;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the warden, and the five poses and one attack it works through.
 * <p>
 * Four of these are pose checks rather than flags: the game has a pose for each of
 * {@link #isDigging() isDigging}, {@link #isEmerging() isEmerging},
 * {@link #isRoaring() isRoaring} and {@link #isSniffing() isSniffing}, and a warden is in
 * at most one of them at a time. {@link #isChargingSonicBoom() isChargingSonicBoom} is not
 * a pose at all — it is the client animation the server starts — and
 * {@link #getAnger() getAnger} is a running total rather than a state.
 * <p>
 * None of them are set from here. What they answer is the warden's pose as the client was
 * last told it, which for a warden the script cannot see in the world is often the only
 * reading of what it is doing.
 * example:
 * <pre>
 * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
 * const wardens = World.getEntities(32, "warden");
 * if (wardens !== null) {
 *   for (const entity of wardens) {
 *     const warden = WardenEntityHelper.class.cast(entity);
 *     // the four poses are one at a time, so the first that fits is the one it is in
 *     if (warden.isDigging()) {
 *       Chat.log(`digging at ${warden.getPos()}`);
 *     } else {
 *       if (warden.isEmerging()) {
 *         Chat.log(`emerging at ${warden.getPos()}`);
 *       } else {
 *         if (warden.isRoaring()) {
 *           Chat.log(`roaring at ${warden.getPos()}, anger ${warden.getAnger()}`);
 *         } else {
 *           if (warden.isSniffing()) {
 *             Chat.log("sniffing");
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class WardenEntityHelper extends MobEntityHelper<Warden> {

    public WardenEntityHelper(Warden base) {
        super(base);
    }

    /**
     * The warden's anger, as a running total the server syncs down. The game reads it
     * through three named bands: below 40 the warden is calm, 40 puts it in the agitated
     * band and 80 in the angry one, and the band is what changes the sounds it makes.
     * <p>
     * Nothing on this class sets it, and it is a total rather than a mood: a warden whose
     * anger is high is not necessarily roaring, and a roaring warden is not necessarily at
     * a high number.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     // 80 is where the game calls a warden angry
     *     if (warden.getAnger() >= 80) {
     *       Chat.log(`a warden at ${warden.getPos()} is at anger ${warden.getAnger()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return this warden's anger towards its active target.
     * @since 1.8.4
     */
    public int getAnger() {
        return base.getClientAngerLevel();
    }

    /**
     * Whether the warden is in its digging pose, which is what it does when it is going
     * into the ground rather than coming out of it. The two are separate poses, so a warden
     * in one never answers {@code true} for the other.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isDigging()) {
     *       // going in, not coming out: the other pose is the one to ask for that
     *       Chat.log(`a warden at ${warden.getPos()} is digging, emerging: ${warden.isEmerging()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is digging into the ground, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isDigging() {
        return base.hasPose(Pose.DIGGING);
    }

    /**
     * Whether the warden is in its emerging pose, which is what it does coming out of the
     * ground. It is the other half of {@link #isDigging() isDigging} and the game keeps the
     * two apart, so a warden in one never answers {@code true} for the other.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isEmerging()) {
     *       Chat.log(`a warden at ${warden.getPos()} is coming up out of the ground`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is emerging from the ground, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isEmerging() {
        return base.hasPose(Pose.EMERGING);
    }

    /**
     * Whether the warden is in its roaring pose, which is what it does between a heartbeat
     * and an attack. It is a pose rather than a flag, so it goes down again when the warden
     * leaves the pose, and the anger behind it is a separate number.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isRoaring()) {
     *       // a pose, so it can be true at a low anger number
     *       Chat.log(`roaring at anger ${warden.getAnger()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is roaring, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRoaring() {
        return base.hasPose(Pose.ROARING);
    }

    /**
     * Whether the warden is in its sniffing pose, which is what it does when it is working
     * out where a vibration came from. Like the other three it is a pose rather than a flag
     * and the game takes it away again on its own.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isSniffing()) {
     *       Chat.log(`a warden at ${warden.getPos()} is following a sound`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is sniffing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSniffing() {
        return base.hasPose(Pose.SNIFFING);
    }

    /**
     * Whether the warden's sonic boom animation is running, which is the closest this gets
     * to the attack itself. The server tells the client to start the animation, and the
     * client runs it off a tick count from that moment, so this is the client-side
     * animation rather than a decision the client made about an attack.
     * <p>
     * That makes it the one call on this class that is about the animation rather than
     * about the warden's pose, and the animation is left started rather than being wound
     * back, so it stays {@code true} for as long as the client has it.
     * example:
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isChargingSonicBoom()) {
     *       // the animation, so this is the window a script gets before the boom lands
     *       Chat.log(`a warden at ${warden.getPos()} is winding up a sonic boom`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is charging its sonic boom attack, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isChargingSonicBoom() {
        return base.sonicBoomAnimationState.isStarted();
    }

}
