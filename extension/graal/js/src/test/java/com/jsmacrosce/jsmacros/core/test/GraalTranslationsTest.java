package com.jsmacrosce.jsmacros.core.test;

import com.jsmacrosce.jsmacros.graal.GraalExtension;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the bundled Graal extension's option labels are found.
 *
 * <p>The extension ships its translations under the {@code jsmacrosce} asset namespace. The core
 * used to look only under the older {@code jsmacros} namespace, so these labels never reached
 * the settings screen.
 */
public class GraalTranslationsTest {

    @Test
    public void findsTheGraalOptionLabels() {
        Map<String, String> translations = new GraalExtension().getTranslations("en_us");
        assertEquals("Graal Options", translations.get("jsmacrosce.settings.languages.graaloptions"));
    }

    @Test
    public void reportsNothingForAnUnknownLanguage() {
        assertTrue(new GraalExtension().getTranslations("xx_xx").isEmpty());
    }

}
