package com.jsmacrosce.jsmacros.fabric.client.api.classes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import com.jsmacrosce.jsmacros.client.access.CommandNodeAccessor;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandManager;
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;

public class CommandManagerFabric extends CommandManager {

    @Override
    public CommandBuilder createCommandBuilder(String name) {
        return new CommandBuilderFabric(name);
    }

    @Override
    public CommandNodeHelper unregisterCommand(String command) throws IllegalAccessException {
        CommandDispatcher<FabricClientCommandSource> activeDispatcher = ClientCommandManager.getActiveDispatcher();
        CommandNode<?> cnf = activeDispatcher == null ? null : CommandNodeAccessor.remove(activeDispatcher.getRoot(), command);
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
            CommandDispatcher<FabricClientCommandSource> activeDispatcher = ClientCommandManager.getActiveDispatcher();
            if (activeDispatcher != null) {
                activeDispatcher.getRoot().addChild((CommandNode) node.fabric);
            }
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
