package com.jsmacrosce.jsmacros.forge.client.api.classes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import com.jsmacrosce.Pair;
import com.jsmacrosce.jsmacros.client.access.CommandNodeAccessor;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder;
import com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommandBuilderForge extends CommandBuilder {
    private static final Map<String, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>> commands = new HashMap<>();

    private final String name;

    private final Stack<Pair<Boolean, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>>> pointer = new Stack<>();

    public CommandBuilderForge(String name) {
        Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> head = (a) -> LiteralArgumentBuilder.literal(name);
        this.name = name;
        pointer.push(new Pair<>(false, head));
    }

    @Override
    protected void argument(String name, Supplier<ArgumentType<?>> type) {
        pointer.push(new Pair<>(true, (e) -> RequiredArgumentBuilder.argument(name, type.get())));
    }

    @Override
    protected void argument(String name, Function<CommandBuildContext, ArgumentType<?>> type) {
        pointer.push(new Pair<>(true, (e) -> RequiredArgumentBuilder.argument(name, type.apply(e))));
    }

    @Override
    public CommandBuilder literalArg(String name) {
        pointer.push(new Pair<>(false, (e) -> LiteralArgumentBuilder.literal(name)));
        return this;
    }

    @Override
    public CommandBuilder executes(MethodWrapper<CommandContextHelper, Object, Object, ?> callback) {
        Pair<Boolean, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>> arg = pointer.pop();
        pointer.push(new Pair<>(arg.getT(), arg.getU().andThen((e) -> e.executes((ctx) -> internalExecutes(ctx, callback)))));
        return this;
    }

    @Override
    protected <S> void suggests(SuggestionProvider<S> suggestionProvider) {
        Pair<Boolean, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>> arg = pointer.pop();
        if (!arg.getT()) {
            throw new AssertionError("SuggestionProvider can only be used on non-literal arguments");
        }
        pointer.push(new Pair<>(true, arg.getU().andThen((e) -> ((RequiredArgumentBuilder) e).suggests(suggestionProvider))));
    }

    @Override
    public CommandBuilder or() {
        if (pointer.size() > 1) {
            Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> oldarg = pointer.pop().getU();
            Pair<Boolean, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>> arg = pointer.pop();
            Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> u = arg.getU();
            pointer.push(new Pair<>(arg.getT(), (ctx) -> u.andThen((e) -> e.then(oldarg.apply(ctx))).apply(ctx)));
        } else {
            throw new AssertionError("Can't use or() on the head of the command");
        }
        return this;
    }

    @Override
    public CommandBuilder or(int argLevel) {
        argLevel = Math.max(1, argLevel);
        while (pointer.size() > argLevel) {
            Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> oldarg = pointer.pop().getU();
            Pair<Boolean, Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>>> arg = pointer.pop();
            Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> u = arg.getU();
            pointer.push(new Pair<>(arg.getT(), (ctx) -> u.andThen((e) -> e.then(oldarg.apply(ctx))).apply(ctx)));
        }
        return this;
    }

    @Override
    public CommandBuilder register() {
        or(1);
        CommandDispatcher<CommandSourceStack> dispatcher = ClientCommandHandler.getDispatcher();
        Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> head = pointer.pop().getU();
        if (dispatcher != null) {
            ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
            if (networkHandler != null) {
                LiteralArgumentBuilder lb = (LiteralArgumentBuilder) head.apply(CommandBuildContext.simple(networkHandler.registryAccess(), networkHandler.enabledFeatures()));
                dispatcher.register(lb);
                networkHandler.getCommands().register(lb);
            }
        }
        commands.put(name, head);
        return this;
    }

    @Override
    public CommandBuilder unregister() throws IllegalAccessException {
        CommandNodeAccessor.remove(ClientCommandHandler.getDispatcher().getRoot(), name);
        ClientPacketListener p = Minecraft.getInstance().getConnection();
        if (p != null) {
            CommandDispatcher<?> cd = p.getCommands();
            CommandNodeAccessor.remove(cd.getRoot(), name);
        }
        commands.remove(name);
        return this;
    }

    public static void onRegisterEvent(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Minecraft mc = Minecraft.getInstance();
        CommandBuildContext registryAccess = CommandBuildContext.simple(mc.getConnection().registryAccess(), mc.getConnection().enabledFeatures());
        for (Function<CommandBuildContext, ArgumentBuilder<CommandSourceStack, ?>> command : commands.values()) {
            dispatcher.register((LiteralArgumentBuilder<CommandSourceStack>) command.apply(registryAccess));
        }
    }

}
