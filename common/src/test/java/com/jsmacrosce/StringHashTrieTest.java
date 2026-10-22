package com.jsmacrosce;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regression checks for {@link StringHashTrie}. Run through the {@code testStringHashTrie} Gradle
 * task; it throws on the first failure and prints a summary otherwise.
 *
 * <p>The trie keeps a compressed prefix structure that gets rebuilt while strings are added, and
 * that used to lose entries, store them twice, or throw depending on insertion order. The checks
 * below pin the behaviour of every public operation against a plain {@link Set}, plus the internal
 * invariants the lookups rely on.
 */
public final class StringHashTrieTest {

    private static int checks = 0;

    public static void main(String[] args) {
        knownBreakages();
        fixedInputs();
        randomSequences();
        System.out.println("StringHashTrie: " + checks + " checks passed");
    }

    /** Sequences that used to fail, kept as readable cases. */
    private static void knownBreakages() {
        // a leaf of exactly the new key length stayed a leaf, so the descent after the rekey found
        // no child and threw
        expect("a,ab", List.of("a", "ab"));
        expect("abc,xy,abcd", List.of("abc", "xy", "abcd"));
        // collapsing children overwrote each other, dropping the earlier subtree
        expect("off/on/open/once/openUrl/listeners", List.of(
                "FJsMacros.off(", "FJsMacros.on(", "FJsMacros.open(",
                "FJsMacros.once(", "FJsMacros.openUrl(", "FJsMacros.listeners("));
        // a value ending exactly on a child key lives inside that child, so it was invisible to
        // contains and could not be removed, and adding it again stored it twice
        expect("abc,xyz,ab", List.of("abc", "xyz", "ab"));
        expect("abc,abd", List.of("abc", "abd"));
        expect("prefix then its own extensions", List.of("aabb", "aabba", "aabbb"));
        expect("removals and re-adds", List.of("abc", "abd", "abe", "xyz"));
        expect("duplicates", List.of("abc", "abc", "abd", "abc"));
        System.out.println("  known breakages: ok");
    }

    private static void fixedInputs() {
        expect("nested paths", List.of(
                "KeyBind.getKeyMapping(", "KeyBind.setKeyMapping(", "KeyBind.matches(",
                "Chat.log(", "Chat.sayf(", "Chat.getHistory(", "Chat.logf(",
                "FFS.exists(", "FFS.isDir(", "FFS.walkFiles(",
                "Player.getPos(", "Player.getBlockPos(", "World.getBlockAt("));
        expect("mixed case", List.of("Alpha(", "alpha(", "ALPHA(", "Alphabet(", "beta("));
        expect("empty and single characters", List.of("", "a", "b", "ab"));
        System.out.println("  fixed inputs: ok");
    }

    /**
     * Random add/remove sequences over alphabets chosen to force prefix sharing, checked after
     * every mutation against a reference set.
     */
    private static void randomSequences() {
        for (int seed = 0; seed < 400; seed++) {
            randomSequence(seed, sharedAlphabet(seed), 6, 24);
        }
        for (int seed = 0; seed < 400; seed++) {
            randomSequence(seed, new char[]{'a', 'b'}, 5, 30);
        }
        for (int seed = 0; seed < 60; seed++) {
            randomSequence(seed, new char[]{'F', 'J', 's', '.', '(', 'A'}, 22, 150);
        }
        for (int seed = 0; seed < 60; seed++) {
            randomSequence(seed, new char[]{'a'}, 1, 10);
        }
        System.out.println("  random sequences: ok");
    }

    private static char[] sharedAlphabet(int seed) {
        char[] base = {'a', 'b', 'c', ':', '.', '(', 'A', 'B'};
        char[] out = new char[2 + (seed % (base.length - 1))];
        System.arraycopy(base, 0, out, 0, out.length);
        return out;
    }

    private static void randomSequence(int seed, char[] alphabet, int maxLength, int operations) {
        Random random = new Random(seed);
        StringHashTrie trie = new StringHashTrie();
        Set<String> reference = new LinkedHashSet<>();
        for (int i = 0; i < operations; i++) {
            String value = randomString(random, alphabet, maxLength);
            if (!reference.isEmpty() && random.nextInt(4) == 0) {
                boolean removed = remove(trie, value, "seed " + seed + ":" + i);
                boolean expected = reference.remove(value);
                require(removed == expected, "seed " + seed + " remove(\"" + value + "\") returned " + removed);
            } else {
                add(trie, value, "seed " + seed + ":" + i);
                reference.add(value);
            }
            verifyAfterMutation(trie, reference, value, "seed " + seed + ":" + i);
        }
        verifyFully(trie, reference, "seed " + seed + " after " + operations + " operations");

        // dropping everything and putting it back exercises the pruning and the empty nodes
        List<String> order = new ArrayList<>(reference);
        Collections.shuffle(order, random);
        for (String value : order) {
            remove(trie, value, "seed " + seed);
            reference.remove(value);
            verifyAfterMutation(trie, reference, value, "seed " + seed + " drop");
            add(trie, value, "seed " + seed);
            reference.add(value);
            verifyAfterMutation(trie, reference, value, "seed " + seed + " restore");
        }
        verifyFully(trie, reference, "seed " + seed + " after the drop and restore pass");
    }

    private static void expect(String label, List<String> input) {
        StringHashTrie trie = new StringHashTrie();
        Set<String> reference = new LinkedHashSet<>();
        for (String value : input) {
            add(trie, value, label);
            reference.add(value);
            verifyFully(trie, reference, label + " after add(\"" + value + "\")");
        }
        for (String value : new ArrayList<>(reference)) {
            remove(trie, value, label);
            reference.remove(value);
            verifyFully(trie, reference, label + " after remove(\"" + value + "\")");
        }
        for (String value : input) {
            add(trie, value, label);
            reference.add(value);
            verifyFully(trie, reference, label + " after re-add(\"" + value + "\")");
        }
    }

    private static void add(StringHashTrie trie, String value, String context) {
        try {
            trie.add(value);
        } catch (RuntimeException e) {
            throw new AssertionError(context + ": add(\"" + value + "\") threw " + e, e);
        }
    }

    private static boolean remove(StringHashTrie trie, String value, String context) {
        try {
            return trie.remove(value);
        } catch (RuntimeException e) {
            throw new AssertionError(context + ": remove(\"" + value + "\") threw " + e, e);
        }
    }

    /**
     * The cheap tier, run after every mutation: the internal shape, the whole contents, and the
     * lookups for the value that was just touched.
     */
    private static void verifyAfterMutation(StringHashTrie trie, Set<String> reference, String value, String context) {
        checks++;
        String broken = shapeProblem(trie);
        require(broken == null, context + ": " + broken);
        Set<String> all = trie.getAll();
        if (!all.equals(reference)) {
            throw new AssertionError(context + ": getAll() lost " + lost(reference, all)
                    + " and invented " + lost(all, reference));
        }
        require(trie.size() == reference.size(),
                context + ": size() is " + trie.size() + " but there are " + reference.size() + " values");
        require(trie.contains(value) == reference.contains(value),
                context + ": contains(\"" + value + "\") is " + trie.contains(value)
                        + " but it should be " + reference.contains(value));
        checkPrefixes(trie, reference, List.of(value, "", "a"), context);
    }

    private static Set<String> lost(Set<String> wanted, Set<String> present) {
        Set<String> missing = new TreeSet<>(wanted);
        missing.removeAll(present);
        return missing;
    }

    /** Every observable operation has to agree with the reference set. */
    private static void verifyFully(StringHashTrie trie, Set<String> reference, String context) {
        checks++;
        String broken = shapeProblem(trie);
        require(broken == null, context + ": " + broken);

        Set<String> all = trie.getAll();
        if (!all.equals(reference)) {
            Set<String> missing = new TreeSet<>(reference);
            missing.removeAll(all);
            Set<String> extra = new TreeSet<>(all);
            extra.removeAll(reference);
            throw new AssertionError(context + ": getAll() lost " + missing + " and invented " + extra);
        }
        require(trie.size() == reference.size(),
                context + ": size() is " + trie.size() + " but there are " + reference.size() + " values");
        require(trie.isEmpty() == reference.isEmpty(), context + ": isEmpty() disagrees with the reference");

        for (String value : reference) {
            require(trie.contains(value), context + ": contains(\"" + value + "\") is false for a stored value");
        }
        require(trie.containsAll(reference), context + ": containsAll is false for its own values");
        require(trie.containsAll(reference.toArray(new String[0])),
                context + ": containsAll(String[]) is false for its own values");
        require(new HashSet<>(trie).equals(reference), context + ": iteration disagrees with the reference");
        require(new HashSet<>(Arrays(trie.toArray(new String[0]))).equals(reference),
                context + ": toArray disagrees with the reference");

        Set<String> probes = new LinkedHashSet<>(List.of("", "a", "b", "ab", "abc", "xy", "A", ":", ".", "zz"));
        for (String value : reference) {
            for (int i = 0; i <= value.length(); i++) {
                probes.add(value.substring(0, i));
            }
        }
        checkPrefixes(trie, reference, probes, context);
    }

    /** Both prefix queries have to return exactly what a scan over the reference returns. */
    private static void checkPrefixes(StringHashTrie trie, Set<String> reference, Collection<String> probes,
                                      String context) {
        for (String probe : probes) {
            Set<String> sensitive = new LinkedHashSet<>();
            Set<String> insensitive = new LinkedHashSet<>();
            String lower = probe.toLowerCase(Locale.ROOT);
            for (String value : reference) {
                if (value.startsWith(probe)) {
                    sensitive.add(value);
                }
                if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                    insensitive.add(value);
                }
            }
            require(trie.getAllWithPrefix(probe).equals(sensitive),
                    context + ": getAllWithPrefix(\"" + probe + "\") is " + new TreeSet<>(trie.getAllWithPrefix(probe))
                            + " but should be " + new TreeSet<>(sensitive));
            require(trie.getAllWithPrefixCaseInsensitive(probe).equals(insensitive),
                    context + ": getAllWithPrefixCaseInsensitive(\"" + probe + "\") is "
                            + new TreeSet<>(trie.getAllWithPrefixCaseInsensitive(probe))
                            + " but should be " + new TreeSet<>(insensitive));
        }
    }

    private static List<String> Arrays(String[] values) {
        List<String> out = new ArrayList<>(values.length);
        Collections.addAll(out, values);
        return out;
    }

    private static String randomString(Random random, char[] alphabet, int maxLength) {
        int length = random.nextInt(maxLength + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------------ internal invariants

    private static String shapeProblem(StringHashTrie root) {
        Map<String, String> seen = new HashMap<>();
        String duplicate = duplicateString(root, "", seen, 0);
        if (duplicate != null) {
            return duplicate;
        }
        return structureProblem(root, 0);
    }

    /** A value ending exactly on a child key is stored inside that child, never beside it twice. */
    private static String duplicateString(StringHashTrie node, String prefix, Map<String, String> seen, int depth) {
        if (depth > 64) {
            return "the tree is deeper than 64 levels, which suggests a cycle";
        }
        for (String leaf : leafsOf(node)) {
            String full = prefix + leaf;
            String previous = seen.put(full, prefix);
            if (previous != null) {
                return "the value \"" + full + "\" is stored twice, under \"" + previous + "\" and \"" + prefix + "\"";
            }
        }
        for (Map.Entry<String, StringHashTrie> entry : childrenOf(node).entrySet()) {
            String deeper = duplicateString(entry.getValue(), prefix + entry.getKey(), seen, depth + 1);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    /** Child keys are exactly as long as the node key length, leaves are no longer than it. */
    private static String structureProblem(StringHashTrie node, int depth) {
        if (depth > 64) {
            return "the tree is deeper than 64 levels, which suggests a cycle";
        }
        int keyLength = keyLengthOf(node);
        for (String leaf : leafsOf(node)) {
            if (leaf.length() > keyLength) {
                return "the leaf \"" + leaf + "\" is longer than the key length " + keyLength + " at " + path(node);
            }
        }
        for (Map.Entry<String, StringHashTrie> entry : childrenOf(node).entrySet()) {
            if (entry.getKey().length() != keyLength) {
                return "the child key \"" + entry.getKey() + "\" is " + entry.getKey().length()
                        + " characters but the key length at " + path(node) + " is " + keyLength;
            }
            String deeper = structureProblem(entry.getValue(), depth + 1);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    private static String path(StringHashTrie node) {
        String key = keyOf(node);
        if (key == null) {
            return "the root";
        }
        StringHashTrie parent = parentOf(node);
        return parent == null ? "\"" + key + "\"" : path(parent) + "/" + key;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, StringHashTrie> childrenOf(StringHashTrie node) {
        return (Map<String, StringHashTrie>) read(CHILDREN, node);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> leafsOf(StringHashTrie node) {
        return (Set<String>) read(LEAFS, node);
    }

    private static int keyLengthOf(StringHashTrie node) {
        return (Integer) read(KEY_LENGTH, node);
    }

    private static String keyOf(StringHashTrie node) {
        return (String) read(KEY, node);
    }

    private static StringHashTrie parentOf(StringHashTrie node) {
        return (StringHashTrie) read(PARENT, node);
    }

    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("could not read the " + field.getName() + " field", e);
        }
    }

    private static final Field CHILDREN = field("children");
    private static final Field LEAFS = field("leafs");
    private static final Field KEY_LENGTH = field("keyLength");
    private static final Field PARENT = field("parent");
    private static final Field KEY = field("key");

    private static Field field(String name) {
        try {
            Field field = StringHashTrie.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}