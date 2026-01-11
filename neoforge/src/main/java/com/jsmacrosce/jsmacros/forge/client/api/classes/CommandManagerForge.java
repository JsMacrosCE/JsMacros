package com.jsmacrosce.jsmacros.forge.client.api.classes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.ClientCommandHandler;
import com.jsmacrosce.jsmacros.client.access.CommandNodeAccessor;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandManager;
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;

public class CommandManagerForge extends CommandManager {

    @Override
    public CommandBuilder createCommandBuilder(String name) {
        return new CommandBuilderForge(name);
    }

    @Override
    public CommandNodeHelper unregisterCommand(String command) throws IllegalAccessException {
        CommandNode<?> cnf = CommandNodeAccessor.remove(ClientCommandHandler.getDispatcher().getRoot(), command);
        CommandNode<?> cn = null;
        ClientPacketListener p = Minecraft.getInstance().getConnection();
        if (p != null) {
            CommandDispatcher<?> cd = p.getCommands();
            cn = CommandNodeAccessor.remove(cd.getRoot(), command);
        }
        return cn != null || cnf != null ? new CommandNodeHelper(cn, cnf) : null;
    }

    @Override
    public void reRegisterCommand(CommandNodeHelper node) {
        if (node.fabric != null) {
            ClientCommandHandler.getDispatcher().getRoot().addChild((CommandNode) node.fabric);
        }
        ClientPacketListener nh = Minecraft.getInstance().getConnection();
        if (nh != null) {
            CommandDispatcher<?> cd = nh.getCommands();
            if (node.getRaw() != null) {
                cd.getRoot().addChild((CommandNode) node.getRaw());
            }
        }
    }

}
