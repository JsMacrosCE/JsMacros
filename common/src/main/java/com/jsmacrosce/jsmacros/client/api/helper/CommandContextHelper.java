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
 * @since 1.4.2
 */
@Event("CommandContext")
@SuppressWarnings("unused")
public class CommandContextHelper extends BaseEvent {
    protected CommandContext<?> base;

    public CommandContextHelper(CommandContext<?> base) {
        super(JsMacrosClient.clientCore);
        this.base = base;
    }

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
     * @param name
     * @return
     * @throws CommandSyntaxException
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
            arg = new ItemStackHelper(((ItemInput) arg).createItemStack(1, false));
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

    public CommandContextHelper getChild() {
        return new CommandContextHelper(base.getChild());
    }

    public StringRange getRange() {
        return base.getRange();
    }

    public String getInput() {
        return base.getInput();
    }

}
