package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps an NBT tag and provides type checks, typed helper views, and path lookup. The numeric
 * type ids are the game's tag ids: 0 is the end marker, 1–6 are numeric tags, 7 is a byte array,
 * 8 a string, 9 a list, 10 a compound, 11 an int array, and 12 a long array.
 * <p>
 * A tag helper wraps the original tag rather than copying it; compound, list, and numeric tags
 * have specialized helper subclasses.
 * <pre>
 * const player = Player.getPlayer();
 * if (player === null) { throw new Error("not in a world"); }
 *
 * // an item's data, which is a compound
 * const nbt = player.getMainHand().getNBT();
 * if (nbt !== null) {
 *   // ask the type before reaching into it
 *   if (nbt.isCompound()) {
 *     for (const key of nbt.getKeys()) {
 *       Chat.log(`${key}: ${nbt.asString(key)}`);
 *     }
 *   }
 *
 *   // and the game's own pretty-printed form, as styled text
 *   Chat.log(nbt.asText().getString());
 * }
 * </pre>
 *
 * @param <T> the Minecraft NBT tag type wrapped by this helper
 * @since 1.5.1
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class NBTElementHelper<T extends Tag> extends BaseHelper<T> {
    private static final LinkedHashMap<String, NbtPathArgument.NbtPath> nbtPaths = new LinkedHashMap<>(32, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, NbtPathArgument.NbtPath> eldest) {
            return size() > 24;
        }

    };

    private NBTElementHelper(T base) {
        super(base);
    }

    /**
     * Returns this tag's Minecraft NBT type id; see the class documentation for the mapping.
     *
     * @return this tag's NBT type id
     * @since 1.5.1
     */
    public int getType() {
        return base.getId();
    }

    /**
     * Resolves a Minecraft NBT path against this tag.
     * <p>
     * Returns all matching tags, or {@code null} if evaluation yields no matches or raises a
     * syntax exception. Invalid syntax during parsing throws {@link CommandSyntaxException}.
     * Parsed paths are cached, with at most 24 entries.
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   // a key at the top level
     *   const damage = nbt.resolve("Damage");
     *   if (damage !== null) {
     *     for (const element of damage) {
     *       Chat.log(`Damage is ${element.asString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param nbtPath the path to resolve, in the game's nbt path syntax
     * @return the matched elements, or {@code null} if evaluation yields no matches or raises a
     * syntax exception
     * @throws CommandSyntaxException if the path format is incorrect
     * @since 1.9.0
     */
    @Nullable
    public List<NBTElementHelper<?>> resolve(String nbtPath) throws CommandSyntaxException {
        NbtPathArgument.NbtPath path = nbtPaths.get(nbtPath);
        if (path == null) {
            path = NbtPathArgument.nbtPath().parse(new StringReader(nbtPath));
            nbtPaths.put(nbtPath, path);
        }
        try {
            return path.get(base).stream().map(NBTElementHelper::resolve).collect(Collectors.toList());
        } catch (CommandSyntaxException ignored) {}
        return null;
    }

    /**
     * Returns whether this tag is the NBT end marker (type id 0).
     *
     * @return {@code true} if this is the end marker rather than a value
     * @since 1.5.1
     */
    public boolean isNull() {
        return base.getId() == 0;
    }

    /**
     * Returns whether this tag is numeric (byte, short, int, long, float, or double).
     *
     * @return {@code true} if this element is a number
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTNumberHelper")
    public boolean isNumber() {
        return base.getId() != 0 && base.getId() < 7;
    }

    /**
     * Returns whether this tag is a string.
     *
     * @return {@code true} if this element is a string
     * @since 1.5.1
     */
    public boolean isString() {
        return base.getId() == 8;
    }

    /**
     * Returns whether this tag is a list or an array tag. An int array of length four can be
     * interpreted as a UUID by {@link NBTListHelper#asUUID()}.
     *
     * @return {@code true} if this element is a list or an array
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTListHelper")
    public boolean isList() {
        return base.getId() == 7 || base.getId() == 9 || base.getId() == 11 || base.getId() == 12;
    }

    /**
     * Returns whether this tag is a compound.
     *
     * @return {@code true} if this element is a compound
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTCompoundHelper")
    public boolean isCompound() {
        return base.getId() == 10;
    }

    /**
     * Returns the string value for string tags, or the tag's textual representation otherwise.
     *
     * @return the string value or textual representation
     * @since 1.5.1
     */
    public String asString() {
        return base.asString().orElseGet(base::toString);
    }

    /**
     * Returns this helper as a numeric helper; the tag must be numeric or the cast fails. This
     * returns the same helper instance, not a copy.
     *
     * @return this helper as a numeric view
     * @since 1.5.1
     */
    public NBTNumberHelper asNumberHelper() {
        return (NBTNumberHelper) this;
    }

    /**
     * Returns this helper as a list/array helper; the tag must be a list or array or the cast
     * fails. {@link NBTListHelper#getHeldType()} returns the array component id, but returns
     * {@code TAG_COMPOUND} for ordinary lists regardless of their elements.
     *
     * @return this helper as a list/array view
     * @since 1.5.1
     */
    public NBTListHelper asListHelper() {
        return (NBTListHelper) this;
    }

    /**
     * Returns this helper as a compound helper; the tag must be a compound or the cast fails.
     *
     * @return this helper as a compound view
     * @since 1.5.1
     */
    public NBTCompoundHelper asCompoundHelper() {
        return (NBTCompoundHelper) this;
    }

    /** Returns a diagnostic string containing the wrapped tag's {@code toString()} representation.
     *
     * @return this helper's diagnostic representation of the wrapped NBT tag
     */
    public String toString() {
        return String.format("NBTElementHelper:{%s}", base.toString());
    }

    /**
     * Returns Minecraft's pretty-printed representation of this tag as styled text.
     *
     * @return the pretty-printed tag as styled text
     * @since 2.0.0
     */
    public TextHelper asText() {
        return TextHelper.wrap(NbtUtils.toPrettyComponent(base));
    }

    /**
     * Wraps the given compound tag without copying it; returns {@code null} for {@code null}
     * input.
     *
     * @param compound the compound to wrap, or {@code null}
     * @return a helper for it, or {@code null} if {@code compound} was {@code null}
     * @since 1.9.0
     */
    @Nullable
    public static NBTCompoundHelper wrapCompound(@Nullable CompoundTag compound) {
        return compound == null ? null : new NBTCompoundHelper(compound);
    }

    /**
     * Wraps a tag without copying it, using the helper for its type; returns {@code null} for
     * {@code null} input.
     *
     * @param element the element to wrap, or {@code null}
     * @return a helper of the matching subclass, or {@code null} if {@code element} was
     * {@code null}
     * @since 1.9.1
     */
    public static NBTElementHelper<?> wrap(@Nullable Tag element) {
        return element == null ? null : resolve(element);
    }

    /**
     * Wraps a tag without copying it, using a specialized helper for numeric, list/array, and
     * compound tags; other tags use the base helper. Returns {@code null} for {@code null} input.
     *
     * @param element the element to wrap, or {@code null}
     * @return a helper of the matching subclass, or {@code null} if {@code element} was
     * {@code null}
     * @since 1.5.1
     */
    @Nullable
    public static NBTElementHelper<?> resolve(@Nullable Tag element) {
        if (element == null) {
            return null;
        }
        switch (element.getId()) {
            case Tag.TAG_END: //0
                return new NBTElementHelper<>(element);
            case Tag.TAG_BYTE: //1
            case Tag.TAG_SHORT: //2
            case Tag.TAG_INT: //3
            case Tag.TAG_LONG: //4
            case Tag.TAG_FLOAT: //5
            case Tag.TAG_DOUBLE: //6
                return new NBTNumberHelper((NumericTag) element);
            case Tag.TAG_BYTE_ARRAY: //7
            case Tag.TAG_LIST: //9
            case Tag.TAG_INT_ARRAY: //11
            case Tag.TAG_LONG_ARRAY: //12
                return new NBTListHelper((CollectionTag) element);
            case Tag.TAG_COMPOUND: //10
                return new NBTCompoundHelper((CompoundTag) element);
            case Tag.TAG_STRING: //8
        }
        return new NBTElementHelper<>(element);
    }

    /** Wraps a numeric tag and provides conversions to primitive numeric types.
     *
     * @since 1.5.1
     */
    public static class NBTNumberHelper extends NBTElementHelper<NumericTag> {

        /** Wraps the supplied numeric tag without copying it.
         *
         * @param base the numeric tag to wrap
         */
        public NBTNumberHelper(NumericTag base) {
            super(base);
        }

        /** Returns this numeric tag converted to a {@code long}.
         *
         * @return the value converted to {@code long}
         * @since 1.5.1
         */
        public long asLong() {
            return base.longValue();
        }

        /** Returns this numeric tag converted to an {@code int}.
         *
         * @return the value converted to {@code int}
         * @since 1.5.1
         */
        public int asInt() {
            return base.intValue();
        }

        /** Returns this numeric tag converted to a {@code short}.
         *
         * @return the value converted to {@code short}
         * @since 1.5.1
         */
        public short asShort() {
            return base.shortValue();
        }

        /** Returns this numeric tag converted to a {@code byte}.
         *
         * @return the value converted to {@code byte}
         * @since 1.5.1
         */
        public byte asByte() {
            return base.byteValue();
        }

        /** Returns this numeric tag converted to a {@code float}.
         *
         * @return the value converted to {@code float}
         * @since 1.5.1
         */
        public float asFloat() {
            return base.floatValue();
        }

        /** Returns this numeric tag converted to a {@code double}.
         *
         * @return the value converted to {@code double}
         * @since 1.5.1
         */
        public double asDouble() {
            return base.doubleValue();
        }

        /** Returns the boxed value in this tag's original numeric type.
         *
         * @return the boxed numeric value in its original type
         * @since 1.5.1
         */
        public Number asNumber() {
            return base.box();
        }

    }

    /** Provides indexed access to list and array tags, and converts four-int arrays to UUIDs.
     *
     * @since 1.5.1
     */
    public static class NBTListHelper extends NBTElementHelper<CollectionTag> {

        /** Wraps the supplied collection tag without copying it.
         *
         * @param base the collection tag to wrap
         */
        public NBTListHelper(CollectionTag base) {
            super(base);
        }

        /** Returns whether this tag is a four-entry int array, the shape accepted by {@link #asUUID()}.
         * @return {@code true} if this is a four-entry int array; {@code false} otherwise
         *
         * @since 1.8.3
         */
        public boolean isPossiblyUUID() {
            return base.getId() == Tag.TAG_INT_ARRAY && base.size() == 4;
        }

        /**
         * Returns the UUID represented by a four-entry int array, or {@code null} for other tags.
         * @return the UUID, or {@code null} if this is not a four-entry int array
         * @since 1.8.3
         */
        @Nullable
        public UUID asUUID() {
            if (!isPossiblyUUID()) {
                return null;
            }
            return UUIDUtil.uuidFromIntArray(base.asIntArray().orElseThrow());
        }

        /** Returns the number of elements in this list or array.
         * @return the number of elements
         *
         * @since 1.5.1
         */
        public int length() {
            return base.size();
        }

        /**
         * Returns the element at the zero-based index. An index outside the collection throws
         * {@link IndexOutOfBoundsException}.
         *
         * @param index zero-based index
         * @return the element at that index
         * @throws IndexOutOfBoundsException if the index is outside the collection
         * @since 1.5.1
         */
        public NBTElementHelper<?> get(int index) {
            return resolve(base.get(index));
        }

        /**
         * Returns the component type id for byte, int, and long arrays. For lists, returns
         * {@code TAG_COMPOUND} regardless of their elements.
         * @return the array component type id, or {@code TAG_COMPOUND} for a list
         * @since 1.5.1
         */
        public int getHeldType() {
            return switch (base.getId()) {
                case Tag.TAG_BYTE_ARRAY -> Tag.TAG_BYTE;
                case Tag.TAG_INT_ARRAY -> Tag.TAG_INT;
                case Tag.TAG_LONG_ARRAY -> Tag.TAG_LONG;
                default -> Tag.TAG_COMPOUND;
            };
        }

    }

    /** Wraps a compound tag and provides access to its keys and values.
     *
     * @since 1.5.1
     */
    public static class NBTCompoundHelper extends NBTElementHelper<CompoundTag> {

        /** Wraps the supplied compound tag without copying it.
         *
         * @param base the compound tag to wrap
         */
        public NBTCompoundHelper(CompoundTag base) {
            super(base);
        }

        /** Returns the compound's live key set.
         *
         * @return the key set backed by the compound
         * @since 1.6.0
         */
        public Set<String> getKeys() {
            return base.keySet();
        }

        /** Returns the tag type id for the value under {@code key}, or {@code -1} if absent.
         *
         * @param key the key to check
         * @return the value's tag type id, or {@code -1} if absent
         * @since 1.8.4
         */
        public int getType(String key) {
            var child = base.get(key);
            return child == null ? -1 : child.getId();
        }

        /** Returns whether this compound contains {@code key}.
         *
         * @param key the key to check
         * @return {@code true} if the key is present
         * @since 1.5.1
         */
        public boolean has(String key) {
            return base.contains(key);
        }

        /** Returns the value under {@code key}, or {@code null} if absent.
         *
         * @param key the key to retrieve
         * @return the value, or {@code null} if absent
         * @since 1.5.1
         */
        @Nullable
        public NBTElementHelper<?> get(String key) {
            return resolve(base.get(key));
        }

        /** Returns the string value under {@code key}, or its textual representation for non-string tags.
         *
         * @param key the key to retrieve
         * @return the value's string or textual representation, or {@code null} if absent
         * @since 1.5.1
         */
        public String asString(String key) {
            var child = base.get(key);
            return child == null ? null : child.asString().orElseGet(child::toString);
        }

    }

}
