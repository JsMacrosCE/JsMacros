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
 * Standalone utilities for hashing and Base64 conversion, guessing chat sender names, and
 * rejecting null values.
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

    /**
     * Initializes this library with its owning core.
     *
     * @param runner the core associated with this library
     */
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
     * Guesses the sender name from a chat message; the heuristic may not fit every server's format.
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
     * Guesses a sender name and any extracted role or prefix fragments. It examines at most the
     * first 60 characters; whisper-form messages yield only the name. This is a heuristic, not a
     * chat-format parser, and input the heuristic cannot recognize produces an empty list. When
     * the returned list contains a name, it is the first element.
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
     * Hashes the message with SHA-256 and returns lowercase hexadecimal.
     *
     * @param message the message to hash
     * @return the hashed message.
     * @since 1.8.4
     */
    public String hashString(@Nullable String message) {
        return DigestUtils.sha256Hex(message);
    }

    /**
     * Hashes with the selected algorithm and returns lowercase hexadecimal. Algorithm names are
     * case-sensitive; an unrecognized name returns the input unchanged, including {@code null}.
     * Recognized algorithms do not accept a {@code null} message.
     *
     * @param message the message to hash
     * @param algorithm sha1 | sha256 | sha384 | sha512 | md2 | md5
     * @return the digest as lowercase hexadecimal, or the original message if the algorithm is
     *         unrecognized
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
     * Hashes with the selected algorithm, returning lowercase hexadecimal or Base64 of the raw
     * digest bytes when {@code base64} is {@code true}. An unrecognized algorithm returns the
     * input unchanged; recognized algorithms do not accept a {@code null} message.
     *
     * @param message the message to hash
     * @param algorithm sha1 | sha256 | sha384 | sha512 | md2 | md5
     * @param base64 encode the result in base64
     * @return the digest in the selected encoding, or the original message if the algorithm is
     *         unrecognized
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
     * Base64-encodes the string's bytes, using the platform's default charset to convert the
     * string to bytes and the encoded bytes back to text.
     *
     * @param message the message to encode
     * @return the encoded message.
     * @since 1.8.4
     */
    public String encode(String message) {
        return new String(Base64.encodeBase64(message.getBytes()));
    }

    /**
     * Base64-decodes the string and converts the decoded bytes to text using the platform's
     * default charset.
     *
     * @param message the message to decode
     * @return the decoded message.
     * @since 1.8.4
     */
    public String decode(String message) {
        return new String(Base64.decodeBase64(message.getBytes()));
    }

    /**
     * Returns the reference if non-null; otherwise throws {@link NullPointerException} without a
     * detail message. Use the two-argument overload to provide one.
     *
     * @param obj the object reference to check for nullity
     * @return {@code obj} if not {@code null}
     * @param <T> the reference type
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.9.1
     */
    @DocletReplaceReturn("T & {}")
    public <T> T requireNonNull(T obj) {
        return Objects.requireNonNull(obj);
    }

    /**
     * Returns {@code obj} if it is non-null; otherwise throws a
     * {@link NullPointerException} with {@code message} as its detail message.
     *
     * @param obj     the object reference to check for nullity
     * @param message detail message for the {@link NullPointerException} if {@code obj} is {@code null}
     * @return {@code obj} if not {@code null}
     * @param <T> the reference type
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.9.1
     */
    @DocletReplaceReturn("T & {}")
    public <T> T requireNonNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }

}
