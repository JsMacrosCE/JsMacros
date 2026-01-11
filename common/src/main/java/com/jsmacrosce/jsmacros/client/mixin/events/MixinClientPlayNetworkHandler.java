package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.jsmacrosce.jsmacros.client.access.BossBarConsumer;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventTitle;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventContainerUpdate;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemPickup;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventSlotUpdate;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDeath;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventStatusEffectUpdate;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.*;
import com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper;

import java.util.*;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonPacketListenerImpl {

    @Shadow
    private ClientLevel level;

    @Shadow
    @Final
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldShowDeathScreen()Z"), method = "handlePlayerCombatKill")
    private void onDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo info) {
        new EventDeath().trigger();
    }

    @Unique
    private final Set<UUID> newPlayerEntries = new HashSet<>();

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/social/PlayerSocialManager;addPlayer(Lnet/minecraft/client/multiplayer/PlayerInfo;)V"), method = "handlePlayerInfoUpdate", locals = LocalCapture.CAPTURE_FAILHARD)
    public void onPlayerList(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci, Iterator var2, Entry entry, PlayerInfo playerListEntry) {
        new EventPlayerJoin(entry.profileId(), playerListEntry).trigger();
    }

    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z", remap = false), method = "handlePlayerInfoRemove", locals = LocalCapture.CAPTURE_FAILHARD)
    public void onPlayerListEnd(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci, Iterator var2, UUID uUID, PlayerInfo playerListEntry) {
        new EventPlayerLeave(uUID, playerListEntry).trigger();
    }

    @ModifyArg(method = "setTitleText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setTitle(Lnet/minecraft/network/chat/Component;)V"))
    public Component onTitle(Component title) {
        EventTitle et = new EventTitle("TITLE", title);
        et.trigger();
        if (et.message == null || et.isCanceled()) {
            return null;
        } else {
            return et.message.getRaw();
        }
    }

    @ModifyArg(method = "setSubtitleText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setSubtitle(Lnet/minecraft/network/chat/Component;)V"))
    public Component onSubtitle(Component title) {
        EventTitle et = new EventTitle("SUBTITLE", title);
        et.trigger();
        if (et.message == null || et.isCanceled()) {
            return null;
        } else {
            return et.message.getRaw();
        }
    }

    @ModifyArg(method = "setActionBarText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public Component onOverlayMessage(Component title) {
        EventTitle et = new EventTitle("ACTIONBAR", title);
        et.trigger();
        if (et.message == null || et.isCanceled()) {
            return null;
        } else {
            return et.message.getRaw();
        }
    }

    @Inject(at = @At("TAIL"), method = "handleBossUpdate")
    public void onBossBar(ClientboundBossEventPacket packet, CallbackInfo info) {
        packet.dispatch(new BossBarConsumer());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"), method = "handleTakeItemEntity")
    public void onItemPickupAnimation(ClientboundTakeItemEntityPacket packet, CallbackInfo info) {
        assert minecraft.level != null;
        final Entity e = minecraft.level.getEntity(packet.getItemId());
        LivingEntity c = (LivingEntity) minecraft.level.getEntity(packet.getPlayerId());
        if (c == null) {
            c = minecraft.player;
        }
        assert c != null;
        if (c.equals(minecraft.player) && e instanceof ItemEntity) {
            ItemStack item = ((ItemEntity) e).getItem().copy();
            item.setCount(packet.getAmount());
            new EventItemPickup(item).trigger();
        }
    }

    @Inject(at = @At("TAIL"), method = "handleLogin")
    public void onGameJoin(ClientboundLoginPacket packet, CallbackInfo info) {
        new EventJoinServer(minecraft.player, connection.getRemoteAddress().toString()).trigger();
    }

    @Inject(at = @At("TAIL"), method = "handleLevelChunkWithLight")
    public void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo info) {
        new EventChunkLoad(packet.getX(), packet.getZ(), true).trigger();
    }

    @Inject(at = @At("TAIL"), method = "handleBlockUpdate")
    public void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo info) {
        new EventBlockUpdate(packet.getBlockState(), level.getBlockEntity(packet.getPos()), packet.getPos(), "STATE").trigger();
    }

    @Inject(at = @At("TAIL"), method = "handleChunkBlocksUpdate")
    public void onChunkDeltaUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo info) {
        packet.runUpdates((blockPos, blockState) -> new EventBlockUpdate(blockState, level.getBlockEntity(blockPos), new BlockPos(blockPos), "STATE").trigger());
    }

    @Inject(at = @At("TAIL"), method = "handleBlockEntityData")
    public void onBlockEntityUpdate(ClientboundBlockEntityDataPacket packet, CallbackInfo info) {
        new EventBlockUpdate(level.getBlockState(packet.getPos()), level.getBlockEntity(packet.getPos()), packet.getPos(), "ENTITY").trigger();
    }

    @Inject(at = @At("TAIL"), method = "handleForgetLevelChunk")
    public void onUnloadChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo info) {
        new EventChunkUnload(packet.pos().x, packet.pos().z).trigger();
    }

    @Inject(
            method = "handleUpdateMobEffect",
            at = @At(
                    value = "INVOKE",
                    //? if >1.21.8 {
                    /*target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    *///?} else {
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    //?}
                    shift = At.Shift.AFTER
            )
    )
    public void onEntityStatusEffect(ClientboundUpdateMobEffectPacket packet, CallbackInfo info) {
        assert minecraft.player != null;
        if (packet.getEntityId() == minecraft.player.getId()) {
            MobEffectInstance newEffect = new MobEffectInstance(packet.getEffect(), packet.getEffectDurationTicks(), packet.getEffectAmplifier(), packet.isEffectAmbient(), packet.isEffectVisible(), packet.effectShowsIcon(), null);
            MobEffectInstance oldEffect = minecraft.player.getEffect(packet.getEffect());
            new EventStatusEffectUpdate(oldEffect == null ? null : new StatusEffectHelper(oldEffect), new StatusEffectHelper(newEffect), true).trigger();
        }
    }

    @Inject(
            method = "handleRemoveMobEffect",
            at = @At(
                    value = "INVOKE",
                    //? if >1.21.8 {
                    /*target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    *///?} else {
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    //?}
                    shift = At.Shift.AFTER))
    public void onEntityStatusEffect(ClientboundRemoveMobEffectPacket packet, CallbackInfo info) {
        if (packet.getEntity(minecraft.level) == minecraft.player) {
            assert minecraft.player != null;
            new EventStatusEffectUpdate(new StatusEffectHelper(minecraft.player.getEffect(packet.effect())), null, false).trigger();
        }
    }

    @Inject(method = "handleSetCursorItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"))
    public void onHeldSlotUpdate(ClientboundSetCursorItemPacket packet, CallbackInfo ci) {
        AbstractContainerScreen<?> screen;
        if (this.minecraft.screen instanceof AbstractContainerScreen<?>) {
            screen = (AbstractContainerScreen<?>) this.minecraft.screen;
        } else {
            screen = new InventoryScreen(this.minecraft.player);
        }
        new EventSlotUpdate(screen, "HELD", -999, this.minecraft.player.containerMenu.getCarried(), packet.contents()).trigger();
    }

    @Inject(method = "handleSetPlayerInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    public void onInventorySlotUpdate(ClientboundSetPlayerInventoryPacket packet, CallbackInfo ci) {
        assert minecraft.player != null;
        new EventSlotUpdate(new InventoryScreen(minecraft.player), "INVENTORY", packet.slot(), this.minecraft.player.inventoryMenu.getSlot(packet.slot()).getItem(), packet.contents()).trigger();
    }

    @Inject(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/InventoryMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V"))
    public void onScreenSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        assert minecraft.player != null;
        new EventSlotUpdate(new InventoryScreen(minecraft.player), "INVENTORY", packet.getSlot(), this.minecraft.player.inventoryMenu.getSlot(packet.getSlot()).getItem(), packet.getItem()).trigger();
    }

    @Inject(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V"))
    public void onScreenSlotUpdate2(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        assert minecraft.player != null;
        if (packet.getContainerId() == 0) {
            new EventSlotUpdate(new InventoryScreen(this.minecraft.player), "INVENTORY", packet.getSlot(), this.minecraft.player.containerMenu.getSlot(packet.getSlot()).getItem(), packet.getItem()).trigger();
            return;
        } else if (this.minecraft.screen instanceof AbstractContainerScreen<?>) {
            if (packet.getContainerId() == ((AbstractContainerScreen<?>) this.minecraft.screen).getMenu().containerId) {
                new EventSlotUpdate((AbstractContainerScreen<?>) this.minecraft.screen, "CONTAINER", packet.getSlot(), this.minecraft.player.containerMenu.getSlot(packet.getSlot()).getItem(), packet.getItem()).trigger();
                return;
            }
        }
        new EventSlotUpdate(new InventoryScreen(this.minecraft.player), "UNKNOWN", packet.getSlot(), this.minecraft.player.containerMenu.getSlot(packet.getSlot()).getItem(), packet.getItem()).trigger();
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    public void onInventoryUpdate(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (packet.containerId() == 0) {
            assert minecraft.player != null;
            new EventContainerUpdate(new InventoryScreen(minecraft.player)).trigger();
        } else {
            if (this.minecraft.screen instanceof AbstractContainerScreen<?>) {
                new EventContainerUpdate((AbstractContainerScreen<?>) this.minecraft.screen).trigger();
            }
        }
    }



    protected MixinClientPlayNetworkHandler(Minecraft arg, Connection arg2, CommonListenerCookie arg3) {
        super(arg, arg2, arg3);
    }

}
