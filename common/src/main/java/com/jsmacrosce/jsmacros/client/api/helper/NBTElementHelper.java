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
 * one piece of nbt, and the tree it belongs to.
 * <p>
 * Nbt is the game's own data format, a tree of typed values, and this wraps one node of it. The
 * base class here is for a node of <i>any</i> type, and it is where the type questions are
 * asked: {@link #getType()}, {@link #isString()}, {@link #isNumber()}, {@link #isList()} and
 * {@link #isCompound()}. Three of the last four pair with a method that narrows to the
 * matching specialised subclass, so a script normally asks the type question first and then
 * converts: {@link #asNumberHelper()}, {@link #asListHelper()} or {@link #asCompoundHelper()}. A
 * string is the exception, because {@link #asString()} already reads one directly. Asking for
 * the wrong one is a cast that fails, which is why the type check is the first half of the
 * pattern.
 * <br>
 * A compound is the node the rest of the data hangs off, and the three subclasses are the whole
 * model: {@link NBTCompoundHelper} for a map of named values, {@link NBTListHelper} for a
 * sequence, and {@link NBTNumberHelper} for a single number. A helper is handed to a script by
 * whatever owns the data — an item's {@code getNBT()}, an entity's, a block's — rather than
 * being built from scratch, and {@link #resolve(String)} reaches into one by a path.
 * <br>
 * The type numbers are the game's own nbt type ids, which are stable enough to switch on:
 * 0 for the end of a compound, 1 to 6 for the number sizes, 7 for a byte array, 8 for a string,
 * 9 for a list, 10 for a compound, 11 for an int array and 12 for a long array.
 * example:
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
     * the game's nbt type id for this element.
     * <p>
     * This is the number the format itself uses and is what the type questions
     * {@link #isString()}, {@link #isNumber()}, {@link #isList()} and {@link #isCompound()} are
     * built on, so a script that would rather switch on it directly can. The values are 0 for
     * the end of a compound, 1 to 6 for the number sizes, 7 for a byte array, 8 for a string,
     * 9 for a list, 10 for a compound, 11 for an int array and 12 for a long array.
     * <br>
     * Note that the number types share a range rather than having ids of their own, so this
     * alone does not distinguish an int from a long; {@link #isNumber()} covers the whole range.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   // 10 is a compound, 8 is a string, 9 is a list
     *   const type = nbt.getType();
     *   Chat.log(type === 10 ? "a compound" : `type ${type}`);
     * }
     * </pre>
     *
     * @since 1.5.1
     */
    public int getType() {
        return base.getId();
    }

    /**
     * resolves the nbt path to elements, or null if not found.<br>
     * <a href="https://minecraft.wiki/w/NBT_path_format">NBT path format wiki</a>
     * <p>
     * This is how a script reaches into data it was handed rather than walking it key by key.
     * The path is the same syntax a command's nbt argument takes, so {@code foo.bar[0]} means
     * the {@code bar} key's first element, and a bare key means the top level of this element.
     * A path that matches several things gives a list, and one that matches nothing gives
     * {@code null}.
     * <br>
     * The exception is for a path the game cannot parse at all, which is a different thing from
     * a path that parses and finds nothing: a malformed path throws, a missing one gives
     * {@code null}. Parsed paths are cached, so repeating a path costs nothing after the first
     * time.
     * example:
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
     * @return the elements the path matched, or {@code null} if it matched none
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
     * whether this element is the end of a compound rather than a value.
     * <p>
     * Nbt marks the end of a compound's contents with a value of its own, and this is the
     * question that identifies it. A script walking a compound will meet one of these and it
     * means "nothing follows", not "there is a null here", so it is normally a sign to stop.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   if (nbt.getKeys().isEmpty()) {
     *     Chat.log("this stack carries no data at all");
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this is the end marker rather than a value
     * @since 1.5.1
     */
    public boolean isNull() {
        return base.getId() == 0;
    }

    /**
     * whether this element is a number, of any width.
     * <p>
     * This covers the whole numeric range at once — byte, short, int, long, float and double —
     * so it is the check to do before reaching for {@link #asNumberHelper()}. A number does
     * not carry the width it was written as, so the value is read back at whichever width is
     * asked for.
     * <br>
     * The end marker is not a number despite sharing the range's edge, so an end marker answers
     * {@code false} here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   const damage = nbt.resolve("Damage");
     *   if (damage !== null) {
     *     for (const element of damage) {
     *       if (element.isNumber()) {
     *         Chat.log(`damage is ${element.asNumberHelper().asInt()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this element is a number
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTNumberHelper")
    public boolean isNumber() {
        return base.getId() != 0 && base.getId() < 7;
    }

    /**
     * whether this element is a string.
     * <p>
     * The one type with a single id, and the one {@link #asString()} reads directly rather than
     * through a specialised helper — though {@code asString()} also gives a readable
     * representation of the other types, so a script that does not care which type it is can
     * just call that.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   for (const key of nbt.getKeys()) {
     *     const child = nbt.get(key);
     *     if (child !== null) {
     *       if (child.isString()) {
     *         Chat.log(`${key} is the string ${child.asString()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this element is a string
     * @since 1.5.1
     */
    public boolean isString() {
        return base.getId() == 8;
    }

    /**
     * whether this element is a sequence, of any element type.
     * <p>
     * This covers every list kind at once — a list itself and the three array types — so it is
     * the check to do before {@link #asListHelper()}. A sequence's elements are all of one
     * type, and {@link NBTListHelper#getHeldType()} says which.
     * <br>
     * A byte array holding four numbers is also the shape the game uses for a uuid, and
     * {@link NBTListHelper#isPossiblyUUID()} is the way to tell that case apart.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   for (const key of nbt.getKeys()) {
     *     const child = nbt.get(key);
     *     if (child !== null) {
     *       if (child.isList()) {
     *         const list = child.asListHelper();
     *         Chat.log(`${key} holds ${list.length()} entries`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this element is a list or an array
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTListHelper")
    public boolean isList() {
        return base.getId() == 7 || base.getId() == 9 || base.getId() == 11 || base.getId() == 12;
    }

    /**
     * whether this element is a compound, which is the map-of-named-values kind.
     * <p>
     * A compound is the shape almost everything interesting is, which is why it has the
     * richest of the three specialised helpers: {@link NBTCompoundHelper} can list its keys and
     * reach one without knowing its name in advance. This is the check to do before
     * {@link #asCompoundHelper()}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   if (nbt.isCompound()) {
     *     Chat.log(`the stack's data has ${nbt.getKeys().size} keys`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this element is a compound
     * @since 1.5.1
     */
    @DocletReplaceReturn("this is NBTElementHelper$NBTCompoundHelper")
    public boolean isCompound() {
        return base.getId() == 10;
    }

    /**
     * if element is a string, returns value.
     * otherwise returns toString representation.
     * <p>
     * This is the readable form of any element, whatever its type, so it is the quickest way to
     * log data without asking what type it is. A string gives its own value; anything else
     * gives nbt's own textual form, which for a compound is the whole thing in one line rather
     * than the pretty form {@link #asText()} produces.
     * <br>
     * On a compound this is a shortcut for the no-argument {@link NBTCompoundHelper#asString()},
     * so the two agree.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   for (const key of nbt.getKeys()) {
     *     const child = nbt.get(key);
     *     if (child !== null) {
     *       // works whatever the child's type is
     *       Chat.log(`${key} = ${child.asString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.5.1
     */
    public String asString() {
        return base.asString().orElseGet(base::toString);
    }

    /**
     * check with {@link #isNumber()} first
     * <p>
     * This is the same object, not a copy, so nothing is converted and no state is lost; it
     * just narrows the type so the number-reading methods are available. On an element that is
     * not a number it fails, which is why the type check comes first.
     * <br>
     * The width the number was written as is not kept, so reading it back at a different width
     * is allowed and gives that width's view of the same value.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   const damage = nbt.resolve("Damage");
     *   if (damage !== null) {
     *     for (const element of damage) {
     *       if (element.isNumber()) {
     *         const number = element.asNumberHelper();
     *         Chat.log(`as int ${number.asInt()}, as double ${number.asDouble()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.5.1
     */
    public NBTNumberHelper asNumberHelper() {
        return (NBTNumberHelper) this;
    }

    /**
     * check with {@link #isList()} first
     * <p>
     * This is the same object, not a copy, so nothing is converted; it narrows the type so the
     * sequence methods are available. On an element that is not a sequence it fails, which is
     * why the type check comes first.
     * <br>
     * All of a sequence's entries are of one type, and {@link NBTListHelper#getHeldType()} is
     * how a script finds out which before reading an entry.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   for (const key of nbt.getKeys()) {
     *     const child = nbt.get(key);
     *     if (child === null) { continue; }
     *     if (!child.isList()) { continue; }
     *     const list = child.asListHelper();
     *     // count down from length(), so the index stops at zero
     *     for (let i = list.length(); i > 0; i--) {
     *       const entry = list.get(i - 1);
     *       if (entry !== null) {
     *         Chat.log(`entry ${i - 1} is ${entry.asString()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.5.1
     */
    public NBTListHelper asListHelper() {
        return (NBTListHelper) this;
    }

    /**
     * check with {@link #isCompound()} first
     * <p>
     * This is the same object, not a copy, so nothing is converted; it narrows the type so the
     * compound methods are available. On an element that is not a compound it fails, which is
     * why the type check comes first.
     * <br>
     * A compound is the shape the other data hangs off — an item, an entity and a block all
     * have one at the top with named values inside — so this is the conversion reached for
     * whenever a script is handed a whole piece of data rather than one field of it, and the
     * reason {@code getKeys()} and the by-key {@code get} are only on the subclass.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   if (nbt.isCompound()) {
     *     for (const key of nbt.getKeys()) {
     *       Chat.log(`${key} = ${nbt.asString(key)}`);
     *     }
     *   }
     * }
     * </pre>
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
     * the game's own pretty-printed form of this element, as text.
     * <p>
     * This is the same rendering the game shows when it prints nbt for a command, and unlike
     * {@link #asString()} it is laid out over several lines with the types and names spelled
     * out, so it is the one to put on screen rather than into a log line. It is built as rich
     * text, so it carries the game's own colouring and a script can hand it straight to a
     * display call.
     * <br>
     * The layout is the point, so this is a worse choice than {@link #asString()} for anything
     * a script is going to parse.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const nbt = player.getMainHand().getNBT();
     *   if (nbt !== null) {
     *     // the game's own formatting, laid out over several lines
     *     Chat.log(nbt.asText());
     *   }
     * }
     * </pre>
     *
     * @since 2.0.0
     */
    public TextHelper asText() {
        return TextHelper.wrap(NbtUtils.toPrettyComponent(base));
    }

    /**
     * wraps a compound, or {@code null} if there is none.
     * <p>
     * The shortcut for getting a {@link NBTCompoundHelper} out of a raw compound, which is
     * what the calls that hand over data use internally. {@code null} in gives {@code null} out
     * rather than a wrapper around nothing, so it is safe to call without checking first.
     * <br>
     * This does not check that the compound is what it claims; it is for wrapping a value that
     * is already known to be a compound.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const nbt = player.getMainHand().getNBT();
     *   if (nbt !== null) {
     *     Chat.log(`${nbt.getKeys().size} keys`);
     *   }
     * }
     * </pre>
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
     * wraps any nbt element, or {@code null} if there is none.
     * <p>
     * The generic entry point: it works out which of the three specialised helpers suits the
     * element's type and gives that back, so a script reaching raw nbt does not have to switch
     * on the type itself. {@code null} in gives {@code null} out.
     * <br>
     * This is what the runtime uses when a script-facing call hands over nbt, which is why the
     * helpers a script gets are already the right subclass rather than the base one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   // a child of the compound, whose type is not known until it is asked
     *   const child = nbt.get("Damage");
     *   if (child !== null) {
     *     if (child.isCompound()) {
     *       Chat.log(`compound with ${child.getKeys().size} keys`);
     *     } else if (child.isNumber()) {
     *       Chat.log(`number ${child.asNumberHelper().asInt()}`);
     *     } else if (child.isList()) {
     *       Chat.log(`list of ${child.asListHelper().length()}`);
     *     } else if (child.isString()) {
     *       Chat.log(`string ${child.asString()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * picks the right subclass for an element, or {@code null} for {@code null}.
     * <p>
     * The switch that {@link #wrap(Tag)} and {@link #resolve(String)} both go through, which
     * is why the helpers a script is handed are already narrowed: a number arrives as a
     * {@link NBTNumberHelper}, a compound as a {@link NBTCompoundHelper}, and anything else as
     * the base class.
     * <br>
     * The type questions are still worth asking on a result, because the base class is the
     * answer for anything not covered — notably the end marker, which has no subclass of its
     * own.
     * <p>
     * A script has no reason to call this directly; it is here because the runtime does.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const nbt = player.getMainHand().getNBT();
     * if (nbt !== null) {
     *   // resolve gives the same narrowing, and reaches in by path
     *   const damage = nbt.resolve("Damage");
     *   if (damage !== null) {
     *     for (const element of damage) {
     *       if (element.isNumber()) {
     *         Chat.log(`damage ${element.asNumberHelper().asInt()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
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
         * @return the element at that index, or {@code null} if the index is past the end
         * @since 1.5.1
         */
        @Nullable
        public NBTElementHelper<?> get(int index) {
            return resolve(base.get(index));
        }

        /**
         * @return the tag type one element of this list holds, which is the element type with the
         * array types narrowed to the type they are an array of
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
         * @return the key set of this compound
         * @since 1.6.0
         */
        public Set<String> getKeys() {
            return base.keySet();
        }

        /**
         * @return the tag type of that key's value, or {@code -1} if the compound has no such
         * key
         * @since 1.8.4
         */
        public int getType(String key) {
            var child = base.get(key);
            return child == null ? -1 : child.getId();
        }

        /**
         * @return whether this compound has that key
         * @since 1.5.1
         */
        public boolean has(String key) {
            return base.contains(key);
        }

        /**
         * @return the value under that key, or {@code null} if this compound has no such key
         * @since 1.5.1
         */
        @Nullable
        public NBTElementHelper<?> get(String key) {
            return resolve(base.get(key));
        }

        /**
         * @return that key's value as a string, or {@code null} if this compound has no such
         * key
         * @since 1.5.1
         */
        public String asString(String key) {
            var child = base.get(key);
            return child == null ? null : child.asString().orElseGet(child::toString);
        }

    }

}
