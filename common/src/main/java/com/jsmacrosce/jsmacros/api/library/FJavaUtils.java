package com.jsmacrosce.jsmacros.api.library;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.*;

/**
 * Factories for the plain Java collection classes the rest of the API hands out, plus a couple of
 * small conversions that a guest language has no equivalent of.<br>
 * The reason these exist is that the wrapped helpers return Java collections rather than script
 * ones, so a list handed back by something like {@code World.getEntities} is a Java
 * {@link List} with a {@code size()} and a {@code get(i)} rather than something with a
 * {@code length}. This library is how a script gets one of its own to build up, which is the
 * reason to prefer building a Java collection here and passing it on rather than keeping a script
 * one.
 * example:
 * <pre>
 * // the wrapped entity helpers hand back a java List, so this is the list to build.
 * // a java List has size() and get(i) rather than length, and getEntities is null
 * // whenever there is no local player yet
 * const nearby = World.getEntities(32);
 * if (nearby !== null) {
 *   const types = JavaUtils.createArrayList();
 *   for (let i = 0; i !== nearby.size(); i += 1) {
 *     types.add(nearby.get(i).getType());
 *   }
 *   Chat.log(`${types.size()} entities nearby`);
 * }
 *
 * // a seeded random gives the same sequence on every run, which makes a script testable
 * const random = JavaUtils.getRandom(1234);
 * Chat.log(`first roll was ${random.nextInt(100)}`);
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@Library("JavaUtils")
@SuppressWarnings("unused")
public class FJavaUtils extends BaseLibrary {

    public FJavaUtils(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Creates a java {@link ArrayList}.<br>
     * The declared element type is a wildcard, so the list accepts anything and the script sees
     * it as a list of unknown rather than as a list of a particular thing. It is the right list
     * to build when the point is to collect results and hand them to something that already
     * wants a Java collection, which is most of the wrapped API.
     * example:
     * <pre>
     * const list = JavaUtils.createArrayList();
     * list.add("diamond");
     * list.add("emerald");
     * // a java List is indexed with get(i) and sized with size(), not length
     * Chat.log(`${list.size()} entries, the second is ${list.get(1)}`);
     * </pre>
     *
     * @return a java ArrayList.
     * @since 1.8.4
     */
    public ArrayList<?> createArrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a java {@link ArrayList}.
     * <br>
     * The array argument is a Java array, not a script array literal, so this is the form to use
     * when the elements have come out of the Java side already, such as another library's return
     * value. The list is a fresh copy, so later changes to the array are not seen through it.
     * example:
     * <pre>
     * // the argument is a Java array rather than a script array literal. FS.list
     * // hands back one, and null when the path is not a readable directory
     * const entries = FS.list(".");
     * if (entries !== null) {
     *   const names = JavaUtils.createArrayList(entries);
     *   Chat.log(`read ${names.size()} names from the script folder`);
     * }
     * </pre>
     *
     * @param array the array to add to the list
     * @param <T>   the type of the array
     * @return a java ArrayList from the given array.
     * @since 1.8.4
     */
    public <T> ArrayList<T> createArrayList(T[] array) {
        return Lists.newArrayList(array);
    }

    /**
     * Creates a java {@link HashMap}.
     *
     * @return a java HashMap.
     * @since 1.8.4
     */
    public HashMap<?, ?> createHashMap() {
        return new HashMap<>();
    }

    /**
     * Creates a java {@link HashSet}.
     *
     * @return a java HashSet.
     * @since 1.8.4
     */
    public HashSet<?> createHashSet() {
        return new HashSet<>();
    }

    /**
     * Returns a {@link SplittableRandom}.
     *
     * @return a SplittableRandom.
     * @since 1.8.4
     */
    public SplittableRandom getRandom() {
        return new SplittableRandom();
    }

    /**
     * Returns {@link SplittableRandom}, initialized with the seed to get identical sequences of
     * values at all times.<br>
     * The sequence is decided entirely by the seed and the order values are drawn in, so the same
     * seed and the same calls give the same numbers on every run and on every machine. That is
     * the difference from the no argument form, and it is what makes a script that picks things
     * at random reproducible rather than surprising. The seed is a 64 bit value, so any whole
     * number a script can write works.
     * example:
     * <pre>
     * // the same seed and the same calls give the same numbers, every time
     * const first = JavaUtils.getRandom(1234);
     * const second = JavaUtils.getRandom(1234);
     * print(`${first.nextInt(100)}, ${first.nextInt(100)}, ${first.nextInt(100)}`);
     * print(`${second.nextInt(100)}, ${second.nextInt(100)}, ${second.nextInt(100)}`);
     * // the two lines above are identical, and a different seed is not
     * print(JavaUtils.getRandom(5678).nextInt(100));
     * </pre>
     *
     * @param seed the seed
     * @return a SplittableRandom.
     * @since 1.8.4
     */
    public SplittableRandom getRandom(long seed) {
        return new SplittableRandom(seed);
    }

    /**
     * wraps a raw vanilla object in the JsMacros helper registered for that kind of object, which
     * is the way to get a usable wrapper for something a script was handed by something other
     * than a JsMacros library.<br>
     * Only a small set of types actually has a helper registered, so a {@code null} here is a
     * completely ordinary answer rather than a failure: it means nothing wraps that type. Check
     * it before using the result. The registered types are the advancement objects, block, nbt
     * and state predicates, a command node, a dye colour, a chat formatting, the interaction
     * manager, the client's options object, a packet buffer, and the nbt tags themselves. The
     * entities, blocks and item stacks that the client libraries hand out are not in that list,
     * because those libraries wrap them themselves when you ask for them.
     * example:
     * <pre>
     * const filterer = JsMacros.createEventFilterer("SendPacket")
     *   .setType("ChatMessageC2SPacket");
     * JsMacros.on("SendPacket", filterer, JavaWrapper.methodToJava(function (event) {
     *   // a helper knows the vanilla object behind it, and this turns such an object
     *   // back into a helper, which is the round trip that makes it reusable
     *   const buffer = event.getPacketBuffer();
     *   const helper = JavaUtils.getHelperFromRaw(buffer.getRaw());
     *   if (helper !== null) {
     *     print(`about to send ${helper.readString()}`);
     *   }
     * }));
     * </pre>
     *
     * @param raw the object to wrap
     * @return the correct instance of {@link BaseHelper} for the given object if it exists and
     * {@code null} otherwise.
     * @since 1.8.4
     */
    @Nullable
    public Object getHelperFromRaw(@NotNull Object raw) {
        Objects.requireNonNull(raw, "Object cannot be null.");
        return runner.helperRegistry.wrap(raw);
    }

    /**
     * @param array the array to convert
     * @return the String representation of the given array.
     * @since 1.8.4
     */
    public String arrayToString(Object[] array) {
        return Arrays.toString(array);
    }

    /**
     * This method will convert any objects hold in the array data to Strings and should be used for
     * multidimensional arrays.<br>
     * The plain form above prints the identity of any element that is itself an array rather than
     * its contents, so a nested array comes back as a reference and not as text. This one walks
     * all the way down, which is why it is the one to reach for when the array came out of
     * something multi dimensional.
     * example:
     * <pre>
     * // a flat array prints the same either way
     * print(JavaUtils.arrayToString(["diamond", "emerald"]));
     * print(JavaUtils.arrayDeepToString(["diamond", "emerald"]));
     * // an array holding other arrays is where the two differ: the plain form has
     * // nothing printable for the inner ones and writes a reference instead, while
     * // the deep form walks all the way down
     * print(JavaUtils.arrayToString([["diamond"], ["emerald"]]));
     * print(JavaUtils.arrayDeepToString([["diamond"], ["emerald"]]));
     * </pre>
     *
     * @param array the array to convert
     * @return the String representation of the given array.
     * @since 1.8.4
     */
    public String arrayDeepToString(Object[] array) {
        return Arrays.deepToString(array);
    }

}
