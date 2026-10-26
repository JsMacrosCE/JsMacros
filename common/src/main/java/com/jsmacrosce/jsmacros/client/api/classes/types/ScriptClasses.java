package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.CustomImage;
import com.jsmacrosce.jsmacros.client.api.library.impl.FKeyBind;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Gives scripts access to image creation and key-tracking utilities through the {@code ScriptClasses} global.
 * Call their static methods directly on the named type.
 * <pre>
 * const image = ScriptClasses.CustomImage.createWidget(16, 16, "badge");
 * image.setGraphicsColor(Color.rgb(0xFF, 0xFF, 0xFF));
 * image.fillRect(0, 0, 16, 16);
 * image.update();
 * </pre>
 */
@ScriptNamespace("ScriptClasses")
@DocletCategory("Script Type Namespaces")
public final class ScriptClasses {
    private ScriptClasses() {
    }

    public static final Class<CustomImage> CustomImage = CustomImage.class;
    public static final Class<FKeyBind.KeyTracker> KeyTracker = FKeyBind.KeyTracker.class;
}
