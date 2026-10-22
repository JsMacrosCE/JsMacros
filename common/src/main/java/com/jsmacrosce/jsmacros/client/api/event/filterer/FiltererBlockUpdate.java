package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventBlockUpdate;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Filters callbacks for the {@code BlockUpdate} event. Create it with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and pass it to the filterer-taking {@code JsMacros.on} overload. Configured criteria combine;
 * unset criteria do not restrict matches, so an untouched filterer accepts every block update. A
 * position alone matches one block; a second corner makes an inclusive box. A {@code null}
 * required block-state value means that property must be absent. Filtering only controls whether
 * the listener runs, not the update itself. This filterer supports only {@code BlockUpdate}.
 * <pre>
 * // block updates inside this inclusive box, and only for dirt
 * const filterer = JsMacros.createEventFilterer("BlockUpdate")
 *   .setArea(-32, 0, -32, 32, 255, 32)
 *   .setBlockId("dirt");
 * const listener = JsMacros.on("BlockUpdate", filterer, JavaWrapper.methodToJava(function (event) {
 *   const pos = event.block.getBlockPos();
 *   Chat.log(`dirt became ${event.block.getId()} at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
@SuppressWarnings("unused")
public class FiltererBlockUpdate implements EventFilterer {
    /**
     * First position corner, or {@code null} to disable position filtering. With {@link #pos2}
     * non-null, the two corners define an inclusive box; assigning this field directly does not
     * clear {@code pos2}.
     */
    @Nullable
    public BlockPosHelper pos;
    /**
     * Second position corner, or {@code null} to match only {@link #pos}. Coordinates must be
     * greater than or equal to {@code pos} on every axis; {@link #setArea(BlockPosHelper, BlockPosHelper)}
     * orders the corners automatically.
     */
    @Nullable
    public BlockPosHelper pos2;
    /**
     * Block id to match exactly, or {@code null} to disable block-id filtering.
     */
    @Nullable
    @DocletReplaceReturn("BlockId | null")
    public String blockId;
    /**
     * Required block-state properties, or {@code null} to disable state filtering. Every entry
     * must match; unlisted properties are ignored. A {@code null} value requires the property to
     * be absent.
     */
    @Nullable
    public Map<String, String> blockState;
    /**
     * Update type to match: {@code "STATE"} for block-state changes or {@code "ENTITY"} for block
     * entity data updates. {@code null} accepts either.
     */
    @Nullable
    @DocletReplaceReturn("BlockUpdateType | null")
    public String updateType;

    /**
     * Whether this filterer supports the named event; the filterer-taking {@code JsMacros.on}
     * overload rejects unsupported event names.
     *
     * @param event the name of the event being listened to
     * @return {@code true} only for {@code "BlockUpdate"}
     */
    @Override
    public boolean canFilter(String event) {
        return "BlockUpdate".equals(event);
    }

    /**
     * Tests whether the event is a {@link EventBlockUpdate} matching every configured criterion.
     * Non-block-update events are rejected; the result controls listener invocation, not the
     * underlying block update.
     *
     * @param baseEvent the event being filtered, which is a
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventBlockUpdate} here
     * @return {@code true} if this filterer accepts the event
     */
    @Override
    public boolean test(BaseEvent baseEvent) {
        if (!(baseEvent instanceof EventBlockUpdate event)) return false;
        if (updateType != null && !updateType.equals(event.updateType)) return false;
        if (blockId != null && !blockId.equals(event.block.getId())) return false;
        if (pos != null) {
            if (pos2 == null) {
                if (!pos.equals(event.block.getBlockPos())) return false;
            } else {
                BlockPosHelper bp = event.block.getBlockPos();
                if (
                        !(bp.getX() >= pos.getX() && bp.getX() <= pos2.getX()) ||
                        !(bp.getY() >= pos.getY() && bp.getY() <= pos2.getY()) ||
                        !(bp.getZ() >= pos.getZ() && bp.getZ() <= pos2.getZ())
                ) return false;
            }
        }
        if (blockState != null) {
            Map<String, String> states = event.block.getBlockState();
            for (var ent : blockState.entrySet()) {
                boolean contains = states.containsKey(ent.getKey());
                if (ent.getValue() == null) {
                    if (contains) return false;
                } else if (!contains || !Objects.equals(states.get(ent.getKey()), ent.getValue())) return false;
            }
        }
        return true;
    }

    /**
     * Sets the position to match, clearing any second corner so only that block is matched.
     *
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @return this filterer
     */
    public FiltererBlockUpdate setPos(int x, int y, int z) {
        return setPos(new BlockPosHelper(x, y, z));
    }

    /**
     * Sets the position to match, clearing any second corner. Passing {@code null} disables
     * position filtering.
     *
     * @param pos the block position to match, or {@code null} to match any position
     * @return this filterer
     */
    public FiltererBlockUpdate setPos(@Nullable BlockPosHelper pos) {
        this.pos = pos;
        pos2 = null;
        return this;
    }

    /**
     * Sets an inclusive block-coordinate box; the two corners may be supplied in either order.
     *
     * @param x1 the x coordinate of one corner
     * @param y1 the y coordinate of one corner
     * @param z1 the z coordinate of one corner
     * @param x2 the x coordinate of the other corner
     * @param y2 the y coordinate of the other corner
     * @param z2 the z coordinate of the other corner
     * @return this filterer
     */
    public FiltererBlockUpdate setArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        return setArea(new BlockPosHelper(x1, y1, z1), new BlockPosHelper(x2, y2, z2));
    }

    /**
     * Sets an inclusive block-coordinate box, ordering each axis independently so either corner
     * may have the lower coordinate on any axis.
     *
     * @param pos1 one corner of the box
     * @param pos2 the other corner of the box
     * @return this filterer
     */
    public FiltererBlockUpdate setArea(@NotNull BlockPosHelper pos1, @NotNull BlockPosHelper pos2) {
        if (pos1.getX() > pos2.getX() || pos1.getY() > pos2.getY() || pos1.getZ() > pos2.getZ()) {
            return setArea(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ()),
                    Math.max(pos1.getX(), pos2.getX()),
                    Math.max(pos1.getY(), pos2.getY()),
                    Math.max(pos1.getZ(), pos2.getZ())
            );
        }
        this.pos = pos1;
        this.pos2 = pos2;
        return this;
    }

    /**
     * Sets the exact block id to match; {@code null} disables block-id filtering. Unqualified ids
     * are parsed with the default {@code minecraft} namespace.
     *
     * @param id the block id to match, in the {@code namespace:name} form
     * @return this filterer
     */
    @DocletReplaceParams("id: BlockId")
    public FiltererBlockUpdate setBlockId(@Nullable String id) {
        blockId = id == null ? null : RegistryHelper.parseNameSpace(id);
        return this;
    }

    /**
     * Sets the exact update type to match; {@code null} disables update-type filtering. A non-null
     * value that differs from the event's type rejects the event.
     *
     * @param type the update type to match
     * @return this filterer
     */
    @DocletReplaceParams("type: BlockUpdateType")
    public FiltererBlockUpdate setUpdateType(@Nullable String type) {
        updateType = type;
        return this;
    }

    /**
     * Replaces all required block-state properties; {@code null} disables state filtering.
     *
     * @param states properties and required values, or {@code null} to disable state filtering
     * @return this filterer
     */
    public FiltererBlockUpdate setBlockStates(@Nullable Map<String, String> states) {
        blockState = states;
        return this;
    }

    /**
     * Adds or replaces a required block-state property; a {@code null} value requires that the
     * property be absent.
     *
     * @param property the property name
     * @param value the required value, or {@code null} to require the property to be absent
     * @return this filterer
     */
    public FiltererBlockUpdate setBlockState(String property, @Nullable String value) {
        if (blockState == null) blockState = new HashMap<>();
        blockState.put(property, value);
        return this;
    }

}
