package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.brigadier.tree.CommandNode;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

public class CommandNodeHelper extends BaseHelper<CommandNode> {
    public final CommandNode<?> fabric;

    public CommandNodeHelper(CommandNode<?> base) {
        super(base);
        fabric = null;
    }

    public CommandNodeHelper(CommandNode<?> base, CommandNode<?> fabric) {
        super(base);
        this.fabric = fabric;
    }

}
