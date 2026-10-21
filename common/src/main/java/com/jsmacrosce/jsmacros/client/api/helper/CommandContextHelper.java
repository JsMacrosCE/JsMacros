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
 * The parsed context for a client command, containing its arguments and command source. A script
 * receives one in a
 * {@link com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder#executes(com.jsmacrosce.jsmacros.core.MethodWrapper) command callback}
 * or a
 * {@link com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder#suggest(com.jsmacrosce.jsmacros.core.MethodWrapper) suggestion callback}.
 * {@link #getArg(String)} reads a named argument. Command execution dispatches this {@link BaseEvent}
 * before invoking the command callback, so
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#on(String, com.jsmacrosce.jsmacros.core.MethodWrapper) JsMacros.on("CommandContext", ...)}
 * can observe it. Suggestion callbacks receive a context helper without dispatching this event.
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

    /**
     * Stores the Brigadier context and initializes the event base with the client core; this does
     * not dispatch the event or execute the command. The reference is retained without copying or
     * validation. A {@code null} context is accepted, but methods that dereference it will fail.
     *
     * @param base the raw Brigadier context to retain; may be {@code null}
     */
    public CommandContextHelper(CommandContext<?> base) {
        super(JsMacrosClient.clientCore);
        this.base = base;
    }

    /**
     * Returns the underlying Brigadier context, exposing its raw values rather than JsMacros
     * wrappers.
     *
     * @return the brigadier context this wraps.
     */
    public CommandContext<?> getRaw() {
        return base;
    }

    /**
     * Returns the hash code delegated to the underlying Brigadier context.
     * A {@code null} underlying context causes {@code NullPointerException}.
     *
     * @return the underlying context's hash code
     */
    @Override
    public int hashCode() {
        return base.hashCode();
    }

    /**
     * Delegates equality to the underlying context, unwrapping another {@code CommandContextHelper}
     * to its context first; any other object is passed directly to the underlying context's
     * {@code equals}. A {@code null} underlying context causes {@code NullPointerException}.
     *
     * @param obj another helper or an object to compare through the underlying context
     * @return the result of the underlying context's equality comparison
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CommandContextHelper) {
            return base.equals(((CommandContextHelper) obj).base);
        }
        return base.equals(obj);
    }

    /**
     * Returns a named argument, converting recognized Brigadier argument values to JsMacros
     * helpers or script-friendly values. The conversions are:
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
     * Item arguments produce a stack of one. Entity selectors use {@code findEntities}, so the
     * result may be empty or contain multiple entities; unlike {@code findSingleEntity}, this does
     * not fail for zero or multiple matches. Selector permission checks can still throw
     * {@link CommandSyntaxException}. Angles, coordinates, and selectors use a fake command source
     * when the context source is a client suggestion provider. In that provider context, every
     * successful name lookup constructs the fake source before conversion. A missing current
     * client player therefore causes {@link NullPointerException} regardless of argument type.
     * The fake source has no server or server level: selectors that require either can also
     * fail with {@link NullPointerException}, rather than returning an empty list. A non-client
     * provider context supplies no fake source, so conversions that dereference it can fail too.
     * Selector permission failures throw {@link CommandSyntaxException}; on Minecraft 26.1.2,
     * item-stack conversion can also throw it for an oversized or malformed stack. Other
     * unconverted argument types are returned as their raw Brigadier values.
     * A name absent from this context throws {@link IllegalArgumentException} before conversion.
     *
     * @param name the name the argument was declared with on the command
     * @return the argument's value, wrapped as described above
     * @throws IllegalArgumentException if no argument of that name exists in this context
     * @throws CommandSyntaxException if selector permission checks fail or, on Minecraft 26.1.2,
     *         item-stack size or component validation fails
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
     * Returns a wrapper for the child context. If none exists, the wrapper is still non-null but
     * {@link #getRaw()} returns {@code null}; other methods on that wrapper will fail.
     *
     * @return a wrapper around the context one level down, whose own {@link #getRaw()} is
     *         {@code null} when there is no child context.
     */
    public CommandContextHelper getChild() {
        return new CommandContextHelper(base.getChild());
    }

    /**
     * Returns the Brigadier range covered by this context. Its start and end offsets are relative
     * to the input returned by {@link #getInput()}.
     *
     * @return the range of this context within the command line.
     */
    public StringRange getRange() {
        return base.getRange();
    }

    /**
     * Returns the full input string stored in the underlying Brigadier context, including the
     * command and arguments rather than only the portion covered by this context's range.
     *
     * @return the full input string stored in this context.
     */
    public String getInput() {
        return base.getInput();
    }

}
