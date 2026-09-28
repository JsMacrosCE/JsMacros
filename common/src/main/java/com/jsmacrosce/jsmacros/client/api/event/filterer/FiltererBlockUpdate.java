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
 * A filter for the {@code BlockUpdate} event, which by itself fires very often, since every block
 * change the server sends raises it. A filterer is built with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and handed to the {@code JsMacros.on} overload that takes a filterer, which then only calls back
 * for the block updates this filterer accepts. The fields below are the filter itself, and the
 * setters return the same filterer so a filter can be built in one chain.<br>
 * Every field starts out {@code null}, which means that part of the filter is not checked, so an
 * untouched filterer accepts every block update. The checks are combined, so setting a block id
 * and a position together only accepts a block of that id at that position.<br>
 * Position comes in two forms. {@link #pos} on its own is a single block, and adding {@link #pos2}
 * turns the pair into an inclusive box, which is what {@code setArea} sets up. Block states are
 * matched through {@link #blockState}, where an entry with a {@code null} value means the block
 * must not have that property at all.<br>
 * The filterer can only be used with {@code BlockUpdate}: handing it to {@code JsMacros.on} for
 * any other event throws an error.
 * example:
 * <pre>
 * // only block updates inside a 64 by 64 area, and only for dirt
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
     * the block position this filter is centred on, or {@code null} to not filter by position.
     * <br>
     * On its own it matches that one block exactly. Together with {@link #pos2} it is the first
     * corner of the box that position filtering then checks against, inclusive on every axis.<br>
     * This field is writable, but assigning to it directly leaves {@link #pos2} alone, so a stale
     * second corner would still widen the filter. Use
     * {@link FiltererBlockUpdate#setPos(BlockPosHelper) setPos()}, which clears the second corner
     * for you.
     */
    @Nullable
    public BlockPosHelper pos;
    /**
     * if this and pos are not null, filters area<br>
     * this should always be larger or equal than pos
     * <br>
     * The far corner of the box {@link #pos} starts, matched inclusively on every axis. Use
     * {@link FiltererBlockUpdate#setArea(BlockPosHelper, BlockPosHelper) setArea()}, which takes
     * care of putting the two corners in the right order.
     */
    @Nullable
    public BlockPosHelper pos2;
    /**
     * the block id to match, in the {@code namespace:name} form, or {@code null} to not filter by
     * block. This is compared against the id of the block that changed, so only an exact match
     * passes.
     */
    @Nullable
    @DocletReplaceReturn("BlockId | null")
    public String blockId;
    /**
     * block state properties to match, as a map of property name to the value it has to have, or
     * {@code null} to not filter by block state. Every entry has to match for the filter to pass,
     * but a property that is not listed is not checked, so this is a "must have" list rather than
     * a full description of the state. A {@code null} value for a property means the block must
     * not have that property at all.
     * <br>
     * Prefer {@link FiltererBlockUpdate#setBlockState(String, String) setBlockState()}, which
     * builds this map for you, over replacing the map outright.
     */
    @Nullable
    public Map<String, String> blockState;
    /**
     * the update type to match, {@code 'STATE'} or {@code 'ENTITY'}, or {@code null} to accept
     * both. {@code 'STATE'} is a block state change and {@code 'ENTITY'} is a block entity data
     * update.
     */
    @Nullable
    @DocletReplaceReturn("BlockUpdateType | null")
    public String updateType;

    /**
     * whether this filterer can be used for the given event name. It only answers {@code true}
     * for {@code "BlockUpdate"}, which is the event it is registered for, and the {@code on}
     * overload of {@code JsMacros} that takes a filterer refuses a filterer that cannot filter
     * the event it was given.
     *
     * @param event the name of the event being listened to
     * @return {@code true} only for {@code "BlockUpdate"}
     */
    @Override
    public boolean canFilter(String event) {
        return "BlockUpdate".equals(event);
    }

    /**
     * the predicate this filterer evaluates. It is called for every
     * {@link EventBlockUpdate} the game raises, so it is on the hot path, and it only decides
     * whether the listener runs, it never changes or blocks the block update itself.<br>
     * A {@code null} field is not checked, so an untouched filterer accepts everything. The
     * filters are combined: a set block id, a set position or box, every entry in the block state
     * map and the update type all have to line up before {@code true} is returned. The position
     * is a single block when {@link #pos2} is {@code null} and an inclusive box when it is not.
     * A {@code null} value in the block state map means the block must not have that property,
     * while any other value means it has to be exactly that. Anything that is not a
     * {@link EventBlockUpdate} is rejected outright.
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
     * sets the position this filter is centred on, which clears any area set before, so the
     * filter matches that one block exactly until an area is set again. Returns the same
     * filterer so calls can be chained.
     *
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @return this filterer, for chaining
     */
    public FiltererBlockUpdate setPos(int x, int y, int z) {
        return setPos(new BlockPosHelper(x, y, z));
    }

    /**
     * sets the position this filter is centred on, which clears any area set before, so the
     * filter matches that one block exactly until an area is set again. Passing {@code null}
     * turns position filtering off entirely. Returns the same filterer so calls can be chained.
     *
     * @param pos the block position to match, or {@code null} to match any position
     * @return this filterer, for chaining
     */
    public FiltererBlockUpdate setPos(@Nullable BlockPosHelper pos) {
        this.pos = pos;
        pos2 = null;
        return this;
    }

    /**
     * sets the box of blocks this filter accepts, inclusive on every axis. The two corners can be
     * given in any order, they are put the right way round for you. Returns the same filterer so
     * calls can be chained.
     *
     * @param x1 the x coordinate of one corner
     * @param y1 the y coordinate of one corner
     * @param z1 the z coordinate of one corner
     * @param x2 the x coordinate of the other corner
     * @param y2 the y coordinate of the other corner
     * @param z2 the z coordinate of the other corner
     * @return this filterer, for chaining
     */
    public FiltererBlockUpdate setArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        return setArea(new BlockPosHelper(x1, y1, z1), new BlockPosHelper(x2, y2, z2));
    }

    /**
     * sets the box of blocks this filter accepts, inclusive on every axis. The two corners can be
     * given in any order, each axis is put the right way round for you, so a box where only one
     * axis is given backwards still comes out correct. Returns the same filterer so calls can be
     * chained.
     *
     * @param pos1 one corner of the box
     * @param pos2 the other corner of the box
     * @return this filterer, for chaining
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
     * sets the block id this filter accepts, which turns off block id filtering when passed
     * {@code null}. An id without a namespace is given the {@code minecraft} one, so
     * {@code "chest"} and {@code "minecraft:chest"} are the same. The id has to match the block
     * that changed exactly, so an id that does not exist never matches anything. Returns the same
     * filterer so calls can be chained.
     *
     * @param id the block id to match, in the {@code namespace:name} form
     * @return this filterer, for chaining
     */
    @DocletReplaceParams("id: BlockId")
    public FiltererBlockUpdate setBlockId(@Nullable String id) {
        blockId = id == null ? null : RegistryHelper.parseNameSpace(id);
        return this;
    }

    /**
     * sets the update type this filter accepts, which turns off update type filtering when passed
     * {@code null}. The value has to match the event's update type exactly, so anything that is
     * not {@code 'STATE'} or {@code 'ENTITY'} lets nothing through. Returns the same filterer so
     * calls can be chained.
     *
     * @param type the update type to match
     * @return this filterer, for chaining
     */
    @DocletReplaceParams("type: BlockUpdateType")
    public FiltererBlockUpdate setUpdateType(@Nullable String type) {
        updateType = type;
        return this;
    }

    /**
     * replaces every block state requirement with the given map, so any requirement added earlier
     * is gone. Passing {@code null} turns block state filtering off entirely. Returns the same
     * filterer so calls can be chained.
     *
     * @param states the properties to require, or {@code null} to require nothing
     * @return this filterer, for chaining
     */
    public FiltererBlockUpdate setBlockStates(@Nullable Map<String, String> states) {
        blockState = states;
        return this;
    }

    /**
     * requires the block to have the given state property set to the given value, keeping any
     * requirement added before. The value has to be written the way the game names that property
     * value, which is what the event's own block state map holds, so read it from there rather
     * than guessing. Passing {@code null} for the value requires the block not to have that
     * property at all. Returns the same filterer so calls can be chained.
     *
     * @param property setting to null will make sure the block doesn't have this property
     * @param value the value the property has to have, or {@code null} to require the property to be absent
     * @return this filterer, for chaining
     */
    public FiltererBlockUpdate setBlockState(String property, @Nullable String value) {
        if (blockState == null) blockState = new HashMap<>();
        blockState.put(property, value);
        return this;
    }

}
