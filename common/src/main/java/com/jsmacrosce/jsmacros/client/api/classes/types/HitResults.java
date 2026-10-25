package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.HitResultHelper;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Lets scripts distinguish block and entity targeting results through the {@code HitResults} global.
 * {@code Block} includes misses; use {@code isMissed()} to distinguish them.
 * <pre>
 * const interactions = Player.interactions();
 * const hit = interactions === null ? null : interactions.getTarget();
 * if (hit instanceof HitResults.Entity) {
 *   Chat.log(hit.getEntity().getName());
 * }
 * </pre>
 */
@ScriptNamespace("HitResults")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class HitResults {
    private HitResults() {
    }

    public static final Class<HitResultHelper> Result = HitResultHelper.class;
    public static final Class<HitResultHelper.Block> Block = HitResultHelper.Block.class;
    public static final Class<HitResultHelper.Entity> Entity = HitResultHelper.Entity.class;
}
