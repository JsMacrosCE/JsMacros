package com.jsmacrosce;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/** Stores strings in a prefix trie and provides collection operations and prefix searches. */
@SuppressWarnings("unused")
public class StringHashTrie implements Collection<String> {
    private Map<String, StringHashTrie> children = new HashMap<>();
    private final Set<String> leafs = new HashSet<>();
    private int keyLength;
    private StringHashTrie parent;
    private String key;

    private StringHashTrie(String initialLeaf, StringHashTrie parent, String key) {
        this.keyLength = initialLeaf.length();
        leafs.add(initialLeaf);
        this.parent = parent;
        this.key = key;
    }

    private StringHashTrie(int keyLength, String childKey, StringHashTrie initialChild, StringHashTrie parent, String key) {
        this.keyLength = keyLength;
        this.children.put(childKey, initialChild);
        this.parent = parent;
        this.key = key;
    }

    /** Initializes an empty root trie with no parent. */
    public StringHashTrie() {
        this.parent = null;
    }

    /** Returns the number of strings stored in this trie, including strings in descendant nodes.
     *
     * @return the total stored string count
     */
    @Override
    public int size() {
        Optional<Integer> childrenSize = children.values().stream().map(StringHashTrie::size).reduce(Integer::sum);
        return leafs.size() + childrenSize.orElse(0);
    }

    /** Returns whether this node has no directly stored strings and no children.
     *
     * @return {@code true} when this node has neither leaves nor children
     */
    @Override
    public boolean isEmpty() {
        return leafs.size() == 0 && children.size() == 0;
    }

    /** Tests whether this trie contains the supplied string, using case-sensitive matching.
     * Non-string values, including {@code null}, are not contained.
     *
     * @param o value to test
     * @return {@code true} if {@code o} is a stored string
     */
    @Override
    public boolean contains(Object o) {
        if (o instanceof String) {
            if (((String) o).length() <= keyLength) {
                return leafs.contains(o);
            } else {
                StringHashTrie child = children.get(((String) o).substring(0, keyLength));
                if (child != null) {
                    return child.contains(((String) o).substring(keyLength));
                }
            }
        }
        return false;
    }

    /** Returns an iterator over a snapshot of the strings currently stored in this trie. Its order is unspecified;
     * removing through the iterator affects only the snapshot, and later trie changes are not reflected.
     *
     * @return an iterator over the snapshot
     */
    @NotNull
    @Override
    public Iterator<String> iterator() {
        return getAll().iterator();
    }

    /** Returns the strings currently stored in this trie as a new array; its order is unspecified.
     *
     * @return a new array containing the current strings
     */
    @NotNull
    @Override
    public String[] toArray() {
        return getAll().toArray(new String[0]);
    }

    /** Copies the strings currently stored in this trie into an array, following {@code Set.toArray(T[])} semantics.
     * The result is a snapshot with unspecified element order. If {@code a} is too small, a new array of the same runtime
     * type is returned; if it is larger, the first unused element is set to {@code null}. A null array causes
     * {@code NullPointerException}, and an incompatible component type causes {@code ArrayStoreException}.
     *
     * @param <T> the array element type
     * @param a destination array
     * @return the destination array if large enough, otherwise a new array of the same runtime type
     */
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return getAll().toArray(a);
    }

    /** Adds a case-sensitive string if it is not already stored. A null string causes {@link NullPointerException}.
     *
     * @param s string to add
     * @return {@code true} if the string was added; {@code false} if it was already present
     */
    @Override
    public synchronized boolean add(String s) {
        if (children.size() == 0 && leafs.size() == 0) {
            this.keyLength = s.length();
            this.leafs.add(s);
            return true;
        }
        if (s.length() <= keyLength || children.size() == 0) {
            if (leafs.contains(s)) {
                return false;
            }
            for (int i = Math.min(s.length(), keyLength); i > 0; --i) {
                for (String key : leafs) {
                    if (key.length() >= i && key.substring(0, i).equals(s.substring(0, i))) {
                        rekey(i);
                        return children.get(s.substring(0, keyLength)).add(s.substring(keyLength));
                    }
                }
            }
            keyLength = Math.max(s.length(), keyLength);
            return leafs.add(s);
        } else {
            String newKey = s.substring(0, keyLength);
            if (children.containsKey(newKey)) {
                return children.get(newKey).add(s.substring(keyLength));
            } else {
                Set<String> keys = children.keySet();
                for (int i = keyLength - 1; i > 0; --i) {
                    for (String key : keys) {
                        if (key.substring(0, i).equals(newKey.substring(0, i))) {
                            rekey(i);
                            return children.get(s.substring(0, keyLength)).add(s.substring(keyLength));
                        }
                    }
                    for (String key : leafs) {
                        if (key.length() >= i && key.substring(0, i).equals(newKey.substring(0, i))) {
                            rekey(i);
                            return children.get(s.substring(0, keyLength)).add(s.substring(keyLength));
                        }
                    }
                }
                children.put(newKey, new StringHashTrie(s.substring(keyLength), this, newKey));
                return true;
            }
        }
    }

    private void removeChild(String childKey) {
        children.remove(childKey);
        if (leafs.size() == 0 && children.size() == 0 && parent != null) {
            parent.removeChild(key);
        }
    }

    /**
     * Removes a string from this trie if present.
     *
     * @param o string to remove
     * @return {@code true} if the string was present and removed
     */
    @Override
    public boolean remove(Object o) {
        if (o instanceof String) {
            if (((String) o).length() <= keyLength) {
                if (leafs.remove(o)) {
                    if (leafs.size() == 0 && children.size() == 0 && parent != null) {
                        parent.removeChild(key);
                    }
                    return true;
                }
            } else {
                StringHashTrie trie = children.get(((String) o).substring(0, keyLength));
                if (trie != null) {
                    return trie.remove(((String) o).substring(keyLength));
                }
            }
        }
        return false;
    }

    /** Tests whether every element of the collection is a stored string. Non-strings and {@code null} elements are not contained; a null collection causes {@link NullPointerException}.
     *
     * @param c collection of values to test
     * @return {@code true} if every element is contained, including when the collection is empty
     */
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        final boolean[] a = {true};
        c.forEach(e -> a[0] = a[0] && contains(e));
        return a[0];
    }

    /** Tests whether every supplied string is stored; unlike the collection overload, this accepts an array of strings.
     * Null elements are not contained; a null array causes {@link NullPointerException}.
     *
     * @param o strings to test
     * @return {@code true} if every string is contained, including when the array is empty
     */
    public boolean containsAll(String... o) {
        final boolean[] a = {true};
        Arrays.stream(o).forEach(e -> a[0] = a[0] && contains(e));
        return a[0];
    }

    /** Adds each string in the collection. Processing is not rolled back if an element fails; a null collection or null element causes {@link NullPointerException}.
     *
     * @param c strings to add
     * @return {@code true} if at least one string was added
     */
    @Override
    public boolean addAll(@NotNull Collection<? extends String> c) {
        final boolean[] a = {false};
        c.forEach(e -> a[0] = add(e) || a[0]);
        return a[0];
    }

    /** Adds each supplied string. Processing is not rolled back if an element fails; a null array or null element causes {@link NullPointerException}.
     *
     * @param o strings to add
     * @return {@code true} if at least one string was added
     */
    public boolean addAll(String... o) {
        final boolean[] a = {false};
        Arrays.stream(o).forEach(e -> a[0] = add(e) || a[0]);
        return a[0];
    }

    /** Removes each supplied element that is a string in this trie. The collection must not be {@code null}; otherwise this method throws {@code NullPointerException}.
     *
     * @param c non-null collection of elements to remove; non-strings and null elements are ignored
     * @return {@code true} if at least one string was removed
     */
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        final boolean[] a = {false};
        c.forEach(e -> a[0] = remove(e) || a[0]);
        return a[0];
    }

    /** Removes each supplied string from this trie. The array must not be {@code null}; otherwise this method throws {@code NullPointerException}.
     *
     * @param o non-null array of strings to remove
     * @return {@code true} if at least one string was removed
     */
    public boolean removeAll(String... o) {
        final boolean[] a = {false};
        Arrays.stream(o).forEach(e -> a[0] = remove(e) || a[0]);
        return a[0];
    }

    /** Removes every stored string not contained in the supplied collection. The collection must not be {@code null}; otherwise this method throws {@code NullPointerException}.
     *
     * @param c non-null collection of strings to retain
     * @return {@code true} if at least one string was removed
     */
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Set<String> content = getAll();
        content.removeAll(c);
        return removeAll(content);
    }

    /** Removes every stored string not present in the supplied strings. The array must not be {@code null}; otherwise this method throws {@code NullPointerException}.
     *
     * @param o non-null array of strings to retain
     * @return {@code true} if at least one string was removed
     */
    public boolean retainAll(String... o) {
        Set<String> content = getAll();
        content.removeAll(Arrays.asList(o));
        return removeAll(content);
    }

    /** Removes all strings stored directly or below this trie node. */
    @Override
    public void clear() {
        leafs.clear();
        children.clear();
    }

    /**
     * Searches for strings beginning with the given case-sensitive prefix. When the prefix ends within a trie key,
     * this returns the first matching leaf if present; otherwise, it returns the first matching child subtree, so
     * other matches may be omitted.
     *
     * @param prefix prefix to match
     * @return matching strings, or all strings when {@code prefix} is empty
     */
    public Set<String> getAllWithPrefix(String prefix) {
        if (prefix.length() > keyLength) {
            String start = prefix.substring(0, keyLength);
            String rest = prefix.substring(keyLength);
            StringHashTrie next = children.get(start);
            if (next != null) {
                return next.getAllWithPrefix(rest).stream().map(e -> start + e).collect(Collectors.toSet());
            } else {
                return new HashSet<>();
            }
        } else if (prefix.length() > 0) {
            for (String leaf : leafs) {
                if (leaf.startsWith(prefix)) {
                    return Sets.newHashSet(leaf);
                }
            }
            for (String key : children.keySet()) {
                if (key.startsWith(prefix)) {
                    return children.get(key).getAll().stream().map(e -> key + e).collect(Collectors.toSet());
                }
            }
            return new HashSet<>();
        } else {
            return getAll();
        }
    }

    /**
     * Searches for strings beginning with the given prefix, ignoring case. When the prefix is longer than a trie key,
     * matching is performed segment by segment, which may omit matches when case-insensitive comparison changes length.
     *
     * @param prefix prefix to match
     * @return matching strings, or all strings when {@code prefix} is empty
     */
    public Set<String> getAllWithPrefixCaseInsensitive(String prefix) {
        if (prefix.length() > keyLength) {
            String start = prefix.substring(0, keyLength);
            String rest = prefix.substring(keyLength);
            Set<String> contents = new HashSet<>();
            for (String key : children.keySet()) {
                if (key.equalsIgnoreCase(start)) {
                    contents.addAll(children.get(key).getAllWithPrefixCaseInsensitive(rest).stream().map(e -> key + e).collect(Collectors.toSet()));
                }
            }
            return contents;
        } else if (prefix.length() > 0) {
            Set<String> contents = new HashSet<>();
            String lowerCasePrefix = prefix.toLowerCase(Locale.ROOT);
            for (String leaf : leafs) {
                if (leaf.toLowerCase(Locale.ROOT).startsWith(lowerCasePrefix)) {
                    contents.add(leaf);
                }
            }
            for (String key : children.keySet()) {
                if (key.toLowerCase(Locale.ROOT).startsWith(lowerCasePrefix)) {
                    contents.addAll(children.get(key).getAll().stream().map(e -> key + e).collect(Collectors.toSet()));
                }
            }
            return contents;
        } else {
            return getAll();
        }
    }

    /**
     * Returns a snapshot of the strings in this trie; modifying the set does not modify the trie.
     *
     * @return all strings
     */
    public Set<String> getAll() {
        Set<String> results = new HashSet<>(leafs);
        children.forEach((k, v) -> results.addAll(v.getAll().stream().map(e -> k + e).collect(Collectors.toSet())));
        return results;
    }

    private void rekey(int newKeyLength) {
        int innerKeyLength = keyLength - newKeyLength;
        Map<String, StringHashTrie> newMap = new HashMap<>();
        for (String key : children.keySet()) {
            String newKey = key.substring(0, newKeyLength);
            String childKey = key.substring(newKeyLength);
            StringHashTrie innerChild = children.get(key);
            if (innerChild.children.size() == 0 && innerChild.leafs.size() == 1) {
                newMap.put(newKey, new StringHashTrie(childKey + innerChild.leafs.toArray()[0], this, newKey));
            } else {
                innerChild.parent = newMap.put(newKey, new StringHashTrie(innerKeyLength, childKey, innerChild, this, newKey));
                innerChild.key = childKey;
            }
        }
        leafs.removeIf(leaf -> {
            if (leaf.length() > newKeyLength) {
                String newKey = leaf.substring(0, newKeyLength);
                newMap.put(newKey, new StringHashTrie(leaf.substring(newKeyLength), this, newKey));
                return true;
            }
            return false;
        });
        keyLength = newKeyLength;
        children = newMap;
    }

    private String jsonChildrenString() {
        StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<String, StringHashTrie> entry : children.entrySet()) {
            builder.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().toString()).append(", ");
        }
        builder.setLength(Math.max(1, builder.length() - 2));
        builder.append("}");
        return builder.toString();
    }

    private String jsonLeafString() {
        StringBuilder builder = new StringBuilder("[");
        for (String leaf : leafs) {
            builder.append("\"").append(leaf).append("\", ");
        }
        builder.setLength(Math.max(1, builder.length() - 2));
        builder.append("]");
        return builder.toString();
    }

    /**
     * @return a structural representation for debugging; it may not be valid JSON
     */
    public String toString() {
        return String.format("{\"leafs\":%s, \"children\":%s}", jsonLeafString(), jsonChildrenString());
    }

}
