package com.jsmacrosce.jsmacros.client.config;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import com.jsmacrosce.jsmacros.access.CustomClickEvent;
import com.jsmacrosce.jsmacros.api.library.FJavaUtils;
import com.jsmacrosce.jsmacros.api.library.FUtils;
import com.jsmacrosce.jsmacros.client.access.IChatHud;
import com.jsmacrosce.jsmacros.client.api.event.impl.*;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.*;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.*;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.*;
import com.jsmacrosce.jsmacros.client.api.helper.*;
import com.jsmacrosce.jsmacros.client.api.library.impl.*;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;
import com.jsmacrosce.jsmacros.client.gui.screens.MacroScreen;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;

import java.util.Arrays;

public class ClientProfile extends BaseProfile {
    private static final Minecraft mc = Minecraft.getInstance();

    public ClientProfile(Core<ClientProfile, ?> runner, Logger logger) {
        super(runner, logger);
    }

    @Override
    protected boolean loadProfile(String profileName) {
        boolean val = super.loadProfile(profileName);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MacroScreen) {
            mc.execute(() -> ((MacroScreen) mc.screen).reload());
        }
        return val;
    }

    public static Class<? extends Throwable>[] ignoredErrors = new Class[]{
            InterruptedException.class,
            BaseScriptContext.ScriptAssertionError.class,
    };

    @Override
    public void logError(Throwable ex) {
        ex.printStackTrace();
        Throwable finalEx = ex;
        if (Arrays.stream(ignoredErrors).anyMatch(e -> e.isAssignableFrom(finalEx.getClass()))) {
            return;
        }
        if (ex instanceof RuntimeException) {
            if (ex.getCause() != null) {
                Throwable cause = ex.getCause();
                if (Arrays.stream(ignoredErrors).anyMatch(e -> e.isAssignableFrom(cause.getClass()))) {
                    return;
                }
                // un-wrap exceptions
                if (ex.getMessage().equals(ex.getCause().toString())) {
                    ex = ex.getCause();
                }
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            BaseWrappedException<?> e;
            try {
                e = runner.wrapException(ex);
            } catch (Throwable t) {
                t.printStackTrace();
                mc.execute(() -> ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(Component.translatable("jsmacrosce.errorerror").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED))));
                return;
            }
            Component text = compileError(e);
            mc.execute(() -> {
                try {
                    ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(text);
                } catch (Throwable t) {
                    ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(Component.translatable("jsmacrosce.errorerror").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                    t.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean checkJoinedThreadStack() {
        return mc.isSameThread() || joinedThreadStack.contains(Thread.currentThread());
    }

    private Component compileError(BaseWrappedException<?> ex) {
        if (ex == null) {
            return null;
        }
        BaseWrappedException<?> head = ex;
        MutableComponent text = Component.literal("");
        do {
            String message = head.message;
            MutableComponent line = Component.literal(message).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            if (head.location != null) {
                Style locationStyle = Style.EMPTY.withColor(ChatFormatting.GOLD);
                if (head.location instanceof BaseWrappedException.GuestLocation) {
                    BaseWrappedException.GuestLocation loc = (BaseWrappedException.GuestLocation) head.location;
                    if (loc.file != null) {
                        locationStyle = locationStyle.withHoverEvent(
                                new HoverEvent.ShowText(Component.translatable("jsmacrosce.clicktoview"))
                        ).withClickEvent(new CustomClickEvent(() -> {
                            if (loc.startIndex > -1) {
                                EditorScreen.openAndScrollToIndex(loc.file, loc.startIndex, loc.endIndex);
                            } else if (loc.line > -1) {
                                EditorScreen.openAndScrollToLine(loc.file, loc.line, loc.column, -1);
                            } else {
                                EditorScreen.openAndScrollToIndex(loc.file, 0, 0);
                            }
                        }));
                    }
                }
                line.append(Component.literal(" (" + head.location + ")").setStyle(locationStyle));
            }
            if ((head = head.next) != null) {
                line.append("\n");
            }
            text.append(line);
        } while (head != null);
        return text;
    }

    @Override
    public void initRegistries() {
        super.initRegistries();

        runner.eventRegistry.addEvent(EventAirChange.class);
        runner.eventRegistry.addEvent(EventArmorChange.class);
        runner.eventRegistry.addEvent(EventAttackBlock.class);
        runner.eventRegistry.addEvent(EventAttackEntity.class);
        runner.eventRegistry.addEvent(EventBlockUpdate.class);
        runner.eventRegistry.addEvent(EventBossbar.class);
        runner.eventRegistry.addEvent(EventChunkLoad.class);
        runner.eventRegistry.addEvent(EventChunkUnload.class);
        runner.eventRegistry.addEvent(EventContainerUpdate.class);
        runner.eventRegistry.addEvent(EventClickSlot.class);
        runner.eventRegistry.addEvent(EventDamage.class);
        runner.eventRegistry.addEvent(EventHeal.class);
        runner.eventRegistry.addEvent(EventDeath.class);
        runner.eventRegistry.addEvent(EventDimensionChange.class);
        runner.eventRegistry.addEvent(EventDisconnect.class);
        runner.eventRegistry.addEvent(EventDropSlot.class);
        runner.eventRegistry.addEvent(EventEntityDamaged.class);
        runner.eventRegistry.addEvent(EventEntityHealed.class);
        runner.eventRegistry.addEvent(EventEntityLoad.class);
        runner.eventRegistry.addEvent(EventEntityUnload.class);
        runner.eventRegistry.addEvent(EventEXPChange.class);
        runner.eventRegistry.addEvent(EventFallFlying.class);
        runner.eventRegistry.addEvent(EventHealthChange.class);
        runner.eventRegistry.addEvent(EventHeldItemChange.class);
        runner.eventRegistry.addEvent(EventHungerChange.class);
        runner.eventRegistry.addEvent(EventInteractBlock.class);
        runner.eventRegistry.addEvent(EventInteractEntity.class);
        runner.eventRegistry.addEvent(EventItemDamage.class);
        runner.eventRegistry.addEvent(EventItemPickup.class);
        runner.eventRegistry.addEvent(EventRecvPacket.class);
        runner.eventRegistry.addEvent(EventSendPacket.class);
        runner.eventRegistry.addEvent(EventJoinServer.class);
        runner.eventRegistry.addEvent(EventKey.class);
        runner.eventRegistry.addEvent(EventLaunchGame.class);
        runner.eventRegistry.addEvent(EventMouseScroll.class);
        runner.eventRegistry.addEvent(EventNameChange.class);
        runner.eventRegistry.addEvent(EventOpenContainer.class);
        runner.eventRegistry.addEvent(EventOpenScreen.class);
        runner.eventRegistry.addEvent(EventRecvPacket.class);
        runner.eventRegistry.addEvent(EventSendPacket.class);
        runner.eventRegistry.addEvent(EventPlayerJoin.class);
        runner.eventRegistry.addEvent(EventPlayerLeave.class);
        runner.eventRegistry.addEvent(EventQuitGame.class);
        runner.eventRegistry.addEvent(EventRecvMessage.class);
        runner.eventRegistry.addEvent(EventRiding.class);
        runner.eventRegistry.addEvent(EventResourcePackLoaded.class);
        runner.eventRegistry.addEvent(EventSendMessage.class);
        runner.eventRegistry.addEvent(EventSignEdit.class);
        runner.eventRegistry.addEvent(EventSlotUpdate.class);
        runner.eventRegistry.addEvent(EventSound.class);
        runner.eventRegistry.addEvent(EventStatusEffectUpdate.class);
        runner.eventRegistry.addEvent(EventTick.class);
        runner.eventRegistry.addEvent(EventTitle.class);

        runner.libraryRegistry.addLibrary(FChat.class);
        runner.libraryRegistry.addLibrary(FHud.class);
        runner.libraryRegistry.addLibrary(FClient.class);
        runner.libraryRegistry.addLibrary(FKeyBind.class);
        runner.libraryRegistry.addLibrary(FPlayer.class);
        runner.libraryRegistry.addLibrary(FPositionCommon.class);
        runner.libraryRegistry.addLibrary(FJavaUtils.class);
        runner.libraryRegistry.addLibrary(FUtils.class);
        runner.libraryRegistry.addLibrary(FWorld.class);

        runner.registerHelper(AdvancementNode.class, AdvancementHelper.class);
        runner.registerHelper(AdvancementTree.class, AdvancementManagerHelper.class);
        runner.registerHelper(AdvancementProgress.class, AdvancementProgressHelper.class);
        runner.registerHelper(BlockPredicate.class, BlockPredicateHelper.class);
//        runner.registerHelper(CommandContext.class, CommandContextHelper.class);
        runner.registerHelper(CommandNode.class, CommandNodeHelper.class);
        runner.registerHelper(DyeColor.class, DyeColorHelper.class);
        runner.registerHelper(ChatFormatting.class, FormattingHelper.class);
        runner.registerHelper(MultiPlayerGameMode.class, InteractionManagerHelper.class);
        runner.helperRegistry.registerType(Tag.class, NBTElementHelper::wrap);
        runner.registerHelper(NumericTag.class, NBTElementHelper.NBTNumberHelper.class);
        runner.registerHelper(CompoundTag.class, NBTElementHelper.NBTCompoundHelper.class);
        runner.registerHelper(CollectionTag.class, (Class) NBTElementHelper.NBTListHelper.class);
        runner.registerHelper(NbtPredicate.class, NbtPredicateHelper.class);
        runner.registerHelper(Options.class, OptionsHelper.class);
        runner.registerHelper(FriendlyByteBuf.class, PacketByteBufferHelper.class);
        runner.registerHelper(StatePropertiesPredicate.class, StatePredicateHelper.class);
        // TODO: complete list

    }

}
