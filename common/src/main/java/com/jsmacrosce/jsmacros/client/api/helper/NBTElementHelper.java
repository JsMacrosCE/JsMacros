package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 1.5.1
 */
@SuppressWarnings("unused")
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
     * @since 1.5.1
     */
    public int getType() {
        return base.getId();
    }

    /**
     * resolves the nbt path to elements, or null if not found.<br>
     * <a href="https://minecraft.wiki/w/NBT_path_format">NBT path format wiki</a>
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
     * @since 1.5.1
     */
    public boolean isNull() {
        return base.getId() == 0;
    }

    /**
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTNumberHelper")
    public boolean isNumber() {
        return base.getId() != 0 && base.getId() < 7;
    }

    /**
     * @since 1.5.1
     */
    public boolean isString() {
        return base.getId() == 8;
    }

    /**
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTListHelper")
    public boolean isList() {
        return base.getId() == 7 || base.getId() == 9 || base.getId() == 11 || base.getId() == 12;
    }

    /**
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTCompoundHelper")
    public boolean isCompound() {
        return base.getId() == 10;
    }

    /**
     * if element is a string, returns value.
     * otherwise returns toString representation.
     *
     * @since 1.5.1
     */
    public String asString() {
        return base.asString().orElseGet(base::toString);
    }

    /**
     * check with {@link #isNumber()} first
     *
     * @since 1.5.1
     */
    public NBTNumberHelper asNumberHelper() {
        return (NBTNumberHelper) this;
    }

    /**
     * check with {@link #isList()} first
     *
     * @since 1.5.1
     */
    public NBTListHelper asListHelper() {
        return (NBTListHelper) this;
    }

    /**
     * check with {@link #isCompound()} first
     *
     * @since 1.5.1
     */
    public NBTCompoundHelper asCompoundHelper() {
        return (NBTCompoundHelper) this;
    }

    public String toString() {
        return String.format("NBTElementHelper:{%s}", base.toString());
    }

    /**
     * pretty print text version
     *
     * @since 2.0.0
     */
    public TextHelper asText() {
        return TextHelper.wrap(NbtUtils.toPrettyComponent(base));
    }

    /**
     * @since 1.9.0
     */
    @Nullable
    public static NBTCompoundHelper wrapCompound(@Nullable CompoundTag compound) {
        return compound == null ? null : new NBTCompoundHelper(compound);
    }

    /**
     * @since 1.9.1
     */
    public static NBTElementHelper<?> wrap(@Nullable Tag element) {
        return element == null ? null : resolve(element);
    }

    /**
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

    /**
     * @since 1.5.1
     */
    public static class NBTNumberHelper extends NBTElementHelper<NumericTag> {

        public NBTNumberHelper(NumericTag base) {
            super(base);
        }

        /**
         * @since 1.5.1
         */
        public long asLong() {
            return base.longValue();
        }

        /**
         * @since 1.5.1
         */
        public int asInt() {
            return base.intValue();
        }

        /**
         * @since 1.5.1
         */
        public short asShort() {
            return base.shortValue();
        }

        /**
         * @since 1.5.1
         */
        public byte asByte() {
            return base.byteValue();
        }

        /**
         * @since 1.5.1
         */
        public float asFloat() {
            return base.floatValue();
        }

        /**
         * @since 1.5.1
         */
        public double asDouble() {
            return base.doubleValue();
        }

        /**
         * @since 1.5.1
         */
        public Number asNumber() {
            return base.box();
        }

    }

    /**
     * @since 1.5.1
     */
    public static class NBTListHelper extends NBTElementHelper<CollectionTag> {

        public NBTListHelper(CollectionTag base) {
            super(base);
        }

        /**
         * @return
         * @since 1.8.3
         */
        public boolean isPossiblyUUID() {
            return base.getId() == Tag.TAG_INT_ARRAY && base.size() == 4;
        }

        /**
         * @return
         * @since 1.8.3
         */
        @Nullable
        public UUID asUUID() {
            if (!isPossiblyUUID()) {
                return null;
            }
            return UUIDUtil.uuidFromIntArray(base.asIntArray().orElseThrow());
        }

        /**
         * @return
         * @since 1.5.1
         */
        public int length() {
            return base.size();
        }

        /**
         * @since 1.5.1
         */
        @Nullable
        public NBTElementHelper<?> get(int index) {
            return resolve(base.get(index));
        }

        /**
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

    /**
     * @since 1.5.1
     */
    public static class NBTCompoundHelper extends NBTElementHelper<CompoundTag> {

        public NBTCompoundHelper(CompoundTag base) {
            super(base);
        }

        /**
         * @return
         * @since 1.6.0
         */
        public Set<String> getKeys() {
            return base.keySet();
        }

        /**
         * @since 1.5.1
         */
        public int getType(String key) {
            var child = base.get(key);
            return child == null ? -1 : child.getId();
        }

        /**
         * @since 1.5.1
         */
        public boolean has(String key) {
            return base.contains(key);
        }

        /**
         * @since 1.5.1
         */
        @Nullable
        public NBTElementHelper<?> get(String key) {
            return resolve(base.get(key));
        }

        /**
         * @since 1.5.1
         */
        public String asString(String key) {
            var child = base.get(key);
            return child == null ? null : child.asString().orElseGet(child::toString);
        }

    }

}
