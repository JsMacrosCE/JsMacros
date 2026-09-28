package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.FakeServerCommandSource;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.EnchantmentHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * the parsed command behind a client command, holding the arguments the player typed and the
 * source the command was run from. A script gets one as the argument of a
 * {@link com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder#executes(com.jsmacrosce.jsmacros.core.MethodWrapper) command callback}
 * or of a
 * {@link com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder#suggest(com.jsmacrosce.jsmacros.core.MethodWrapper) suggestion callback},
 * and {@link #getArg(String)} is how it reads a named argument back.<br>
 * This class carries the {@code @Event("CommandContext")} annotation and extends
 * {@link BaseEvent}, so it has the shape of an event, but it is not one and the event system never
 * delivers it. Nothing triggers it
 * and it is not registered as an event name, so
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#on(String, com.jsmacrosce.jsmacros.core.MethodWrapper) JsMacros.on("CommandContext", ...)}
 * is refused with an error saying the event was not found. The way to get one is a command
 * callback, as in the example below.<br>
 * Being a {@link BaseEvent} does still earn its keep: a command callback can hand the context
 * straight to
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#runScript(String, BaseEvent) JsMacros.runScript()},
 * which is the recommended way to do anything that waits, since the callback is holding a lock
 * while it runs.<br>
 * This event is not cancellable, so calling {@code cancel()} on it throws, and it is never
 * cancellable through the event registry either since it is not registered there. Command success
 * is reported by the return value of the callback, not by cancelling anything.
 * example:
 * <pre>
 * const builder = Chat.getCommandManager().createCommandBuilder("hello");
 * builder.greedyStringArg("name");
 * builder.executes(JavaWrapper.methodToJava(function (ctx) {
 *   // getArg hands the argument back already wrapped for its type,
 *   // a greedy string comes back as a plain string
 *   Chat.log(`hello ${ctx.getArg("name")}`);
 *   return true;
 * }));
 * builder.register();
 * </pre>
 * @since 1.4.2
 */
@DocletCategory("System/Lifecycle")
@Event("CommandContext")
@SuppressWarnings("unused")
public class CommandContextHelper extends BaseEvent {
    protected CommandContext<?> base;

    public CommandContextHelper(CommandContext<?> base) {
        super(JsMacrosClient.clientCore);
        this.base = base;
    }

    /**
     * the underlying brigadier context, unwrapped. Use it only for what JsMacros does not already
     * cover: {@link #getArg(String)} and the other members here are the wrapped form of the
     * commands a script normally needs.<br>
     * It is the game's own object rather than a copy, and it is not a helper, so a script reading
     * through it gets raw brigadier values rather than JsMacros wrappers.
     * example:
     * <pre>
     * // inside a command callback, where ctx is this object
     * const raw = ctx.getRaw();
     * // the whole command line, straight from brigadier
     * print(`the whole command was ${raw.getInput()}`);
     * </pre>
     *
     * @return the brigadier context this wraps.
     */
    public CommandContext<?> getRaw() {
        return base;
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CommandContextHelper) {
            return base.equals(((CommandContextHelper) obj).base);
        }
        return base.equals(obj);
    }

    /**
     * reads one named argument out of the command, already wrapped for the kind of argument it is
     * rather than as the raw brigadier value. That is the main reason to use this class at all,
     * since a bare brigadier argument is close to useless to a script.<br>
     * What comes back depends entirely on the type the argument was declared with, so check the
     * list of argument types on
     * {@link com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder} against the type
     * you declared. The conversions that are in place are:<br>
     * a block argument becomes a
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper}, an identifier
     * becomes its {@code namespace:path} string, an item becomes an
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper} of one item, an
     * NBT argument becomes the matching
     * {@link com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper}, text becomes a
     * {@link com.jsmacrosce.jsmacros.client.api.helper.TextHelper}, a colour becomes a
     * {@link com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper}, an angle becomes a plain
     * number, an enchantment becomes an
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.EnchantmentHelper}, a particle
     * and a status effect each become their identifier string, a position becomes a
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper}, and an entity
     * selector becomes a plain list of
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper}. An item
     * predicate and a block predicate become a Java predicate, which takes the thing being tested
     * and answers whether it is accepted, with an item stack for the first and a block position for
     * the second. Anything not on that list comes back as the raw brigadier value.<br>
     * Two of those need care. The item an item argument produces is a stack of one, whatever count
     * the player typed, because the argument only ever parses an item and never a stack. And the
     * angle, the position and the entity selector are all resolved against a
     * {@link com.jsmacrosce.jsmacros.client.api.classes.FakeServerCommandSource FakeServerCommandSource}
     * that is only built when the context's own source is a client suggestion provider, which is
     * the case for a JsMacros client command. That fake source is built from the local player and
     * has no server behind it, so the entity selector in particular can fail on a command that was
     * not the client's own, or before a world has been joined.<br>
     * The declared type of the return value is only as good as the argument name passed in, so a
     * name that does not exist on the command cannot be caught at compile time. Check the argument
     * really is there before reading it.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("mark");
     * builder.blockPosArg("pos");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // a declared position comes back as a BlockPosHelper, not as three numbers
     *   const pos = ctx.getArg("pos");
     *   const count = ctx.getArg("count");
     *   Chat.log(`${count} marks at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
     *   const block = World.getBlock(pos);
     *   if (block !== null) {
     *     Chat.log(`that is ${block.getId()}`);
     *   }
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @param name the name the argument was declared with on the command
     * @return the argument's value, wrapped as described above
     * @throws IllegalArgumentException if no argument of that name exists on the command, which is
     *         the error a wrong or misspelled name actually produces
     * @throws CommandSyntaxException if an entity selector matched nothing, or more than one entity
     *         where only one was asked for
     * @since 1.4.2
     */
    public Object getArg(String name) throws CommandSyntaxException {
        Object arg = base.getArgument(name, Object.class);
        CommandSourceStack fakeServerSource = null;
        if (base.getSource() instanceof ClientSuggestionProvider) {
            fakeServerSource = new FakeServerCommandSource((ClientSuggestionProvider) base.getSource(), Minecraft.getInstance().player);
        }
        if (arg instanceof BlockInput) {
            arg = new BlockStateHelper(((BlockInput) arg).getState());
        } else if (arg instanceof ResourceLocation) {
            arg = ((ResourceLocation) arg).toString();
        } else if (arg instanceof ItemInput) {
            //? if >=26.1 {
            /*arg = new ItemStackHelper(((ItemInput) arg).createItemStack(1));
            *///?} else {
            arg = new ItemStackHelper(((ItemInput) arg).createItemStack(1, false));
            //?}
        } else if (arg instanceof Tag) {
            arg = NBTElementHelper.resolve((Tag) arg);
        } else if (arg instanceof Component) {
            arg = TextHelper.wrap((Component) arg);
        } else if (arg instanceof ChatFormatting) {
            arg = new FormattingHelper((ChatFormatting) arg);
        } else if (arg instanceof AngleArgument.SingleAngle) {
            arg = ((AngleArgument.SingleAngle) arg).getAngle(fakeServerSource);
        } else if (arg instanceof ItemPredicateArgument.Result) {
            ItemPredicateArgument.Result itemPredicate = (ItemPredicateArgument.Result) arg;
            arg = (Predicate<ItemStackHelper>) item -> itemPredicate.test(item.getRaw());
        } else if (arg instanceof BlockPredicateArgument.Result) {
            BlockPredicateArgument.Result blockPredicate = (BlockPredicateArgument.Result) arg;
            arg = (Predicate<BlockPosHelper>) block -> blockPredicate.test(new BlockInWorld(Minecraft.getInstance().level, block.getRaw(), false));
        } else if (arg instanceof Coordinates) {
            arg = new BlockPosHelper(((Coordinates) arg).getBlockPos(fakeServerSource));
        } else if (arg instanceof Holder<?>) {
            if (((Holder<?>) arg).value() instanceof Enchantment) {
                arg = new EnchantmentHelper((Holder<Enchantment>) arg);
            }
        } else if (arg instanceof EntitySelector) {
            arg = ((EntitySelector) arg).findEntities(fakeServerSource).stream().map(EntityHelper::create).collect(Collectors.toList());
        } else if (arg instanceof ParticleOptions) {
            arg = BuiltInRegistries.PARTICLE_TYPE.getKey(((ParticleOptions) arg).getType()).toString();
        } else if (arg instanceof MobEffect) {
            arg = BuiltInRegistries.MOB_EFFECT.getKey(((MobEffect) arg)).toString();
        }
        return arg;
    }

    /**
     * the context of the next level down, for a command that was redirected into another one with
     * {@code or()} or an argument fork. A plain command with nothing below it has no child, since
     * brigadier reports that as nothing at all rather than as an empty context.<br>
     * The wrapper this returns is always a fresh object and never {@code null}, but it is built
     * around that missing context in the no-child case, so {@link #getRaw()} on it comes back
     * {@code null} and anything else on it throws. Check {@link #getRaw()} first rather than the
     * result of this call.<br>
     * The declared return type does not say any of that, and a child wrapper is never the same
     * object as its parent even though the two compare equal.
     * example:
     * <pre>
     * // inside a command callback, where ctx is this object
     * const child = ctx.getChild();
     * if (child.getRaw() === null) {
     *   // nothing was redirected below this context
     * } else {
     *   print(`the child context covers ${child.getInput()}`);
     * }
     * </pre>
     *
     * @return a wrapper around the context one level down, whose own {@link #getRaw()} is
     *         {@code null} when there is no child context.
     */
    public CommandContextHelper getChild() {
        return new CommandContextHelper(base.getChild());
    }

    /**
     * where in the command text this context sits, as the brigadier range covering it, which gives
     * the start and end offsets with {@code getStart()} and {@code getEnd()}.<br>
     * It is the game's own range object rather than a JsMacros helper, so the offsets are read off
     * the returned value directly. They count from the start of the command line that
     * {@link #getInput()} returns, so they can be used to slice it.
     * example:
     * <pre>
     * // inside a command callback, where ctx is this object
     * const range = ctx.getRange();
     * print(`this context covers ${range.getStart()} to ${range.getEnd()}`);
     * </pre>
     *
     * @return the range of this context within the command line.
     */
    public StringRange getRange() {
        return base.getRange();
    }

    /**
     * the whole command line this context was parsed out of, rather than just the part this
     * context covers. A command with several arguments gives all of them here, and the command name
     * is part of it too.<br>
     * It is the text the command parser was given, so it has already been through whatever
     * trimming the chat box or the client command source does before the command is dispatched.
     * example:
     * <pre>
     * // inside a command callback, where ctx is this object
     * print(`the player typed ${ctx.getInput()}`);
     * </pre>
     *
     * @return the full command line that produced this context.
     */
    public String getInput() {
        return base.getInput();
    }

}
