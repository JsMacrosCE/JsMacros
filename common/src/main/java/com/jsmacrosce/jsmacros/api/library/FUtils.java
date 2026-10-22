package com.jsmacrosce.jsmacros.api.library;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.util.NameUtil;

import java.util.List;
import java.util.Objects;

/**
 * Small standalone helpers that are useful in any script: hashing a value, base64 encoding it,
 * pulling a player name back out of a formatted chat line, and failing loudly on a value that
 * should not be null.<br>
 * Nothing here reaches into Minecraft. That makes these the safe functions to call from a script
 * that runs on a timer or a service, where the wrapped world helpers may not be usable.
 * example:
 * <pre>
 * // hashing, sha-256 in hex by default
 * Chat.log(Utils.hashString("hello"));
 * // the same digest in base64, which is the form a lot of APIs ask for
 * Chat.log(Utils.hashString("hello", "sha256", true));
 *
 * // a base64 round trip
 * const encoded = Utils.encode("hello");
 * Chat.log(`${encoded} decodes back to ${Utils.decode(encoded)}`);
 *
 * // pulling the sender's name back out of a chat line
 * const name = Utils.guessName("Notch whispers to you: meet me at spawn");
 * Chat.log(name === null ? "no name could be guessed" : `that was ${name}`);
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@Library("Utils")
@SuppressWarnings("unused")
public class FUtils extends BaseLibrary {

    public FUtils(Core<?, ?> runner) {
        super(runner);
    }
//
//    /**
//     * Tries to guess the name of the sender of a given message. This is not guaranteed to work and
//     * for specific servers it may be better to use regex instead.
//     *
//     * @param text the text to check
//     * @return the name of the sender or {@code null} if it couldn't be guessed.
//     * @since 1.8.4
//     */
//    public String guessName(TextHelper text) {
//        return guessName(text.getStringStripFormatting());
//    }

    /**
     * Tries to guess the name of the sender of a given message. This is not guaranteed to work and
     * for specific servers it may be better to use regex instead.
     *
     * @param text the text to check
     * @return the name of the sender or {@code null} if it couldn't be guessed.
     * @since 1.8.4
     */
    public String guessName(String text) {
        List<String> names = guessNameAndRoles(text);
        return names.isEmpty() ? null : names.get(0);
    }
//
//    /**
//     * Tries to guess the name, as well as the titles and roles of the sender of the given message.
//     * This is not guaranteed to work and for specific servers it may be better to use regex
//     * instead.
//     *
//     * @param text the text to check
//     * @return a list of names, titles and roles of the sender or an empty list if it couldn't be
//     * guessed.
//     * @since 1.8.4
//     */
//    public List<String> guessNameAndRoles(TextHelper text) {
//        return guessNameAndRoles(text.getStringStripFormatting());
//    }

    /**
     * Tries to guess the name, as well as the titles and roles of the sender of the given message.
     * This is not guaranteed to work and for specific servers it may be better to use regex
     * instead.<br>
     * The first element is the name and anything after it is a title, prefix or role the server
     * put around it, so this is the form to use when a name alone is not enough, such as when a
     * server's rank prefix should be ignored. It is a heuristic on punctuation rather than a
     * parser: it scans at most the first 60 characters, it understands the whisper form, and it
     * stops at the first colon, or the first of the bracket shapes a server uses to mark off a
     * name, once a candidate has been seen. A line it cannot make sense of comes back as an empty
     * list rather than a null, so check the size.
     * example:
     * <pre>
     * // a bracketed prefix becomes an extra entry rather than part of the name
     * const parts = Utils.guessNameAndRoles("[Admin] Notch: hello");
     * const name = Utils.guessName("[Admin] Notch: hello");
     * Chat.log(`name: ${name}, ${parts.size()} entries in total`);
     * // index 0 is the name, so the rest is whatever the server prefixed it with
     * for (let i = 1; i !== parts.size(); i += 1) {
     *   Chat.log(`prefix: ${parts.get(i)}`);
     * }
     * // a line with nothing name shaped in it is an empty list, not a null
     * Chat.log(Utils.guessNameAndRoles("...").size() === 0);
     * </pre>
     *
     * @param text the text to check
     * @return a list of names, titles and roles of the sender or an empty list if it couldn't be
     * guessed.
     * @since 1.8.4
     */
    public List<String> guessNameAndRoles(String text) {
        return NameUtil.guessNameAndRoles(text);
    }

    /**
     * Hashes the given string with sha-256 and returns the digest as lower case hex, which is the
     * two argument form's {@code sha256} case spelled on its own. The digest is computed straight
     * from the string, so a {@code null} message is not tolerated here.
     * example:
     * <pre>
     * // the same value always hashes to the same 64 character hex string
     * const digest = Utils.hashString("hello");
     * Chat.log(`${digest} is 64 characters long: ${digest.length === 64}`);
     * </pre>
     *
     * @param message the message to hash
     * @return the hashed message.
     * @since 1.8.4
     */
    public String hashString(@Nullable String message) {
        return DigestUtils.sha256Hex(message);
    }

    /**
     * Hashes the given string with the selected algorithm and returns the digest as lower case
     * hex.<br>
     * The algorithm name has to be one of the six listed below, spelled exactly that way. A name
     * that is not on that list is not an error and does not throw: the message is handed back
     * unchanged, so a misspelling quietly gives the plaintext back instead of a digest. A
     * {@code null} message is only tolerated on that path, and comes back as {@code null}; on
     * every other path it is not tolerated at all. That path is also the only one this can hand
     * back a {@code null} at all, so the result is nullable rather than always a digest, which is
     * why the example narrows it before using it.
     * example:
     * <pre>
     * // a correct name gives a 64 character hex digest. the result is nullable,
     * // since an algorithm that is not on the list hands the input back instead
     * const digest = Utils.hashString("hello", "sha256") ?? "";
     * // a misspelled name is not reported anywhere, it just hands the input back
     * const notADigest = Utils.hashString("hello", "sha-256") ?? "";
     * Chat.log(`${digest.length} characters of hash, versus the input itself: ${notADigest}`);
     * </pre>
     *
     * @param message the message to hash
     * @param algorithm sha1 | sha256 | sha384 | sha512 | md2 | md5
     * @return the hashed message (Hex), or {@code null} when the message was {@code null} and
     *         the algorithm was not one of the six
     * @since 1.8.4
     */
    @Nullable
    public String hashString(@Nullable String message, String algorithm) {
        switch (algorithm) {
            case "sha256":
                return DigestUtils.sha256Hex(message);
            case "sha512":
                return DigestUtils.sha512Hex(message);
            case "sha1":
                return DigestUtils.sha1Hex(message);
            case "sha384":
                return DigestUtils.sha384Hex(message);
            case "md2":
                return DigestUtils.md2Hex(message);
            case "md5":
                return DigestUtils.md5Hex(message);
            default:
                return message;
        }
    }

    /**
     * Hashes the given string with the selected algorithm.<br>
     * The third argument only changes how the digest is written out, never which digest is
     * computed. With {@code true} the raw digest bytes are base64 encoded, so what comes back is
     * the base64 of the same bytes the hex form represents rather than the base64 of that hex, and
     * the two results are not interchangeable. It is still not plain text: the digest is computed
     * first either way.<br>
     * An algorithm name that is not one of the six listed below hands the message back untouched,
     * exactly as the two argument form does, so the same misspelling is quiet in both. The result
     * is nullable for the same reason it is there, so it has to be narrowed before it is used.
     * example:
     * <pre>
     * // the same sha-256 digest, written two ways: 64 characters of hex against
     * // 44 characters of base64 over the same bytes
     * const hex = Utils.hashString("hello", "sha256", false) ?? "";
     * const base64 = Utils.hashString("hello", "sha256", true) ?? "";
     * Chat.log(`hex is ${hex.length} characters and base64 is ${base64.length}`);
     * </pre>
     *
     * @param message the message to hash
     * @param algorithm sha1 | sha256 | sha384 | sha512 | md2 | md5
     * @param base64 encode the result in base64
     * @return the hashed message (Hex or Base64), or {@code null} when the message was
     *         {@code null} and the algorithm was not one of the six
     * @since 1.9.1
     */
    @Nullable
    public String hashString(@Nullable String message, String algorithm, Boolean base64) {
        switch (algorithm) {
            case "sha256":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.sha256(message)));
                return DigestUtils.sha256Hex(message);
            case "sha512":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.sha512(message)));
                return DigestUtils.sha512Hex(message);
            case "sha1":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.sha1(message)));
                return DigestUtils.sha1Hex(message);
            case "sha384":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.sha384(message)));
                return DigestUtils.sha384Hex(message);
            case "md2":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.md2(message)));
                return DigestUtils.md2Hex(message);
            case "md5":
                if (base64)
                    return new String(Base64.encodeBase64(DigestUtils.md5(message)));
                return DigestUtils.md5Hex(message);
            default:
                return message;
        }
    }

    /**
     * Encodes the given string with Base64.<br>
     * The bytes are taken with the platform's default charset rather than UTF-8, so a script
     * encoding text outside ASCII is at the mercy of whatever that is. Use
     * {@link #hashString(String, String, Boolean) hashString(message, "sha256", true)} when the
     * goal is a digest to hand to something else, rather than round tripping a value.
     * example:
     * <pre>
     * const encoded = Utils.encode("hello");
     * Chat.log(`${encoded} round trips to ${Utils.decode(encoded)}`);
     * </pre>
     *
     * @param message the message to encode
     * @return the encoded message.
     * @since 1.8.4
     */
    public String encode(String message) {
        return new String(Base64.encodeBase64(message.getBytes()));
    }

    /**
     * Decodes the given string with Base64.
     *
     * @param message the message to decode
     * @return the decoded message.
     * @since 1.8.4
     */
    public String decode(String message) {
        return new String(Base64.decodeBase64(message.getBytes()));
    }

    /**
     * Checks that the specified object reference is not {@code null}.<br>
     * In a script language that hands back a null for a missing value, this is the way to say
     * "this one is required" and have the failure land where the value was read rather than
     * several lines later. It throws the same {@link NullPointerException} as the bare check, so
     * a script that does not catch it dies the same way either does, and with no message of its
     * own: the two argument form is the one that says what the value was wanted for.
     * example:
     * <pre>
     * // getPlayer is null outside a world, and this turns that into a clear
     * // failure at the point the value was read rather than several lines later
     * const player = Utils.requireNonNull(Player.getPlayer());
     * Chat.log(`in a world, looking at ${player.getPos()}`);
     * </pre>
     *
     * @param obj the object reference to check for nullity
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.9.1
     */
    @DocletReplaceReturn("T & {}")
    public <T> T requireNonNull(T obj) {
        return Objects.requireNonNull(obj);
    }

    /**
     * Checks that the specified object reference is not {@code null} and
     * throws a customized {@link NullPointerException} if it is.<br>
     * Same check as the one argument form, with a message that says what the value was wanted for.
     * That message is what shows up in the log, so it is the difference between a failure that
     * can be traced back to the line that read the value and one that cannot.
     * example:
     * <pre>
     * // the two argument form is the one worth reaching for, the message is the point
     * const player = Utils.requireNonNull(Player.getPlayer(), "getPlayer came back null");
     * Chat.log(`a world is loaded, player is at ${player.getPos()}`);
     * </pre>
     *
     * @param obj     the object reference to check for nullity
     * @param message detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.9.1
     */
    @DocletReplaceReturn("T & {}")
    public <T> T requireNonNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }

}
