package com.jsmacrosce.jsmacros.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for guessing player names and roles from chat-message prefixes.
 * @author Etheradon
 * @since 1.8.4
 */
public final class NameUtil {

    private static final String VALID_NAME = "[A-Za-z0-9_]+";
    private static final Pattern PATTERN_NAME = Pattern.compile(VALID_NAME);
    private static final Pattern PATTERN_WHISPER = Pattern.compile("(" + VALID_NAME + ") whispers to you.*");

    private static final int MAX_STRING_LENGTH = 60;
    private static final int DEFAULT_BUILDER_CAPACITY = 16;

    private NameUtil() {
    }

    /**
     * Guesses a player name and preceding roles from at most the first 60 characters of a chat message.
     * Returns an empty list when no supported prefix pattern is recognized.
     * @param text chat message to inspect
     * @return guessed roles and player name, or an empty list when no supported prefix is recognized
     */
    public static List<String> guessNameAndRoles(String text) {
        String toAnalyze = text.substring(0, Math.min(MAX_STRING_LENGTH, text.length()));

        Matcher whisperMatcher = PATTERN_WHISPER.matcher(toAnalyze);
        if (whisperMatcher.find()) {
            return Collections.singletonList(whisperMatcher.group(1));
        }

        List<String> roles = new LinkedList<>();

        boolean foundName = false;
        String potentialLastName = "";
        int spaceCounter = 0;
        String lastString = "";
        StringBuilder nameBuilder = new StringBuilder(DEFAULT_BUILDER_CAPACITY);

        char current;
        char peek;

        for (int i = 0; i < toAnalyze.length(); i++) {
            current = toAnalyze.charAt(i);
            peek = i < toAnalyze.length() - 1 ? toAnalyze.charAt(i + 1) : Character.MIN_VALUE;

            if (Character.isLetter(current) || Character.isDigit(current) || current == '_') {
                nameBuilder.append(current);
            } else {
                if (nameBuilder.length() != 0) {
                    lastString = nameBuilder.toString();
                    nameBuilder = new StringBuilder(DEFAULT_BUILDER_CAPACITY);
                }

                if (foundName) {
                    roles.add(0, getNameOrDefault(lastString));
                    return roles;
                }

                // Optional check for closing bracket >
                if (current == ' ') {
                    spaceCounter++;
                } else if (current == '<') {
                    foundName = true;
                } else if (current == ':') {
                    roles.add(0, getNameOrDefault(lastString));
                    return roles;
                } else if (current == ']' || current == ')') {
                    roles.add(lastString);
                    potentialLastName = lastString;
                    spaceCounter = 0;
                } else if (current == '-' && peek == '>' && !lastString.isEmpty()) {
                    roles.add(0, getNameOrDefault(lastString));
                    return roles;
                } else if (current == '>') {
                    if (peek == '>') {
                        roles.add(0, getNameOrDefault(lastString));
                        return roles;
                    } else {
                        potentialLastName = lastString;
                        spaceCounter = 0;
                    }
                }

                if (spaceCounter > 2) {
                    roles.remove(potentialLastName);
                    roles.add(0, getNameOrDefault(potentialLastName));
                    return roles;
                }

            }
        }
        return Collections.emptyList();
    }

    private static String getNameOrDefault(CharSequence potentialName) {
        return getNameOrDefault(potentialName, "");
    }

    private static String getNameOrDefault(CharSequence potentialName, String defaultValue) {
        return PATTERN_NAME.matcher(potentialName).matches() ? potentialName.toString() : StringUtils.isBlank(defaultValue) ? "" : getNameOrDefault(defaultValue, "");
    }

}
