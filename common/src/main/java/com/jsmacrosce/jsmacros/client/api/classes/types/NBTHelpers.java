package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Lets scripts distinguish numbers, lists, and compounds in NBT data through the {@code NBTHelpers} global.
 * {@code List} includes array-tag helpers; string tags have no separate helper class.
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const tag = player.getNBT();
 *   if (tag instanceof NBTHelpers.Compound) {
 *     Chat.log(tag.getKeys());
 *   }
 * }
 * </pre>
 */
@ScriptNamespace("NBTHelpers")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class NBTHelpers {
    private NBTHelpers() {
    }

    public static final Class<NBTElementHelper> Element = NBTElementHelper.class;
    public static final Class<NBTElementHelper.NBTNumberHelper> Number = NBTElementHelper.NBTNumberHelper.class;
    public static final Class<NBTElementHelper.NBTListHelper> List = NBTElementHelper.NBTListHelper.class;
    public static final Class<NBTElementHelper.NBTCompoundHelper> Compound = NBTElementHelper.NBTCompoundHelper.class;
}
