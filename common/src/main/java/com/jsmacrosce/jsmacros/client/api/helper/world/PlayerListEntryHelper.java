package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

//? if >1.21.8 {
/*import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
*///?} else {
import net.minecraft.client.resources.PlayerSkin;
//?}

/**
 * One line of the tab list: one player as the server has told the client about them. A script gets
 * one from {@code World.getPlayers()}, which is every entry the client holds, or from
 * {@code World.getPlayerEntry(name)}, which is one by account name.<br>
 * What is here is the server's account of a player rather than the player: the name and uuid from
 * the profile, the ping and game mode the server last reported, the skin and cape the client
 * downloaded, and the team they are on. Nothing here asks the world, so it answers the same whether
 * the player is loaded or not, and unlike {@code Player.getPlayer()} it is not about the local
 * player at all.<br>
 * The skin calls are worth reading together. {@link #getSkinTexture()} and the two cape and elytra
 * texture calls name the texture records themselves,
 * {@link #getSkinUrl()} and {@link #getElytraUrl()} give the download address of the ones that were
 * actually downloaded, and {@link #hasCape()} is the test for whether there is a cape at all.
 * example:
 * <pre>
 * const players = World.getPlayers();
 * if (players !== null) {
 *   for (let i = 0; i !== players.size(); i += 1) {
 *     const entry = players.get(i);
 *     Chat.log(`${entry.getName()} ${entry.getUUID()} ping ${entry.getPing()}ms`);
 *   }
 * }
 *
 * // one player by name, and what is known about them
 * const notch = World.getPlayerEntry("Notch");
 * if (notch !== null) {
 *   Chat.log(`${notch.getDisplayText()} is on ${notch.getGamemode()}`);
 *   Chat.log(`slim model: ${notch.hasSlimModel()}, cape: ${notch.hasCape()}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.2
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class PlayerListEntryHelper extends BaseHelper<PlayerInfo> {

    public PlayerListEntryHelper(PlayerInfo p) {
        super(p);
    }

    /**
     * The player's account uuid as the string form, with the dashes in it. This comes from the
     * profile the server sent rather than from anything the client worked out, so it is the same for
     * a player whether or not they are loaded.
     * example:
     * <pre>
     * // the account uuid of everybody in the tab list
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     Chat.log(`${players.get(i).getName()}: ${players.get(i).getUUID()}`);
     *   }
     * }
     * </pre>
     *
     * @return the player's account uuid in its string form.
     * @since 1.1.9
     */
    @Nullable
    public String getUUID() {
        GameProfile prof = base.getProfile();
        //? if >1.21.8 {
        /*return prof.id().toString();
        *///?} else {
        return prof.getId().toString();
        //?}
    }

    /**
     * The player's account name, which is the name the server knows them by and the same string
     * {@code World.getPlayerEntry(name)} takes.
     * example:
     * <pre>
     * // the account names, which is what getPlayerEntry looks up
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     Chat.log(players.get(i).getName());
     *   }
     * }
     * </pre>
     *
     * @return the player's account name.
     * @since 1.0.2
     */
    @Nullable
    public String getName() {
        GameProfile prof = base.getProfile();
        //? if >1.21.8 {
        /*return prof.name();
        *///?} else {
        return prof.getName();
        //?}
    }

    /**
     * How long the server reported this player's ping as, in milliseconds. This is the number the
     * server measures, which is not the same as the round trip time the client sees, and it is
     * whatever the last packet carried rather than a fresh measurement.
     * example:
     * <pre>
     * // who on the server has the worst ping
     * const players = World.getPlayers();
     * if (players !== null) {
     *   const sorted = [];
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     sorted.push({ name: players.get(i).getName(), ping: players.get(i).getPing() });
     *   }
     *   sorted.sort(function (a, b) { return b.ping - a.ping; });
     *   for (const row of sorted.slice(0, 5)) {
     *     Chat.log(`${row.name}: ${row.ping}ms`);
     *   }
     * }
     * </pre>
     *
     * @return the player's ping in milliseconds.
     * @since 1.6.5
     */
    public int getPing() {
        return base.getLatency();
    }

    /**
     * The game mode the server last reported for this player, as the word a command uses for it.
     * The client starts every entry off on the default mode and overwrites it when the server says
     * otherwise, so a player the server has not reported a mode for yet reads as that default rather
     * than as nothing.<br>
     * There is therefore always a mode here, one of survival, creative, adventure or spectator, and
     * never an absent one. The call is nonetheless marked as one that can give {@code null}, which is
     * further on the cautious side than the game itself goes, so it is worth writing a script as
     * though the mode were there even where the annotation says it might not be.
     * example:
     * <pre>
     * // who is in creative, and who in survival
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     Chat.log(`${entry.getName()} is on ${entry.getGamemode()}`);
     *   }
     * }
     * </pre>
     *
     * @return the player's game mode, as the word the game uses for it.
     * @since 1.6.5
     */
    @DocletReplaceReturn("Gamemode")
    @Nullable
    public String getGamemode() {
        GameType gm = base.getGameMode();
        return gm.getName();
    }

    /**
     * The name the tab list shows for this player, which is whatever the server chose and can carry
     * formatting and a prefix from their team. A server that shows nothing special for a player
     * gives {@code null} here, since the tab list entry has no name of its own in that case.
     * example:
     * <pre>
     * // the tab list as the server writes it, rather than the account names
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     const shown = entry.getDisplayText();
     *     if (shown !== null) {
     *       Chat.log(shown.getString());
     *     } else {
     *       Chat.log(entry.getName());
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the name shown for this player in the tab list, or {@code null} if the server did
     * not set one.
     * @since 1.1.9
     */
    public TextHelper getDisplayText() {
        return TextHelper.wrap(base.getTabListDisplayName());
    }

    /**
     * The public key this player's chat messages are signed with, as the encoded bytes of the key.
     * A player on a server that is not enforcing signed chat has no session here, and those give
     * {@code null} rather than an empty array, so this is the test for whether a player is signing.
     * example:
     * <pre>
     * // who is signing their chat
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     const key = entry.getPublicKey();
     *     if (key === null) {
     *       Chat.log(`${entry.getName()} has no chat session`);
     *     } else {
     *       Chat.log(`${entry.getName()} signs with a ${key.length} byte key`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the encoded public key, or {@code null} if this player has no chat session.
     * @since 1.8.2
     */
    @Nullable
    public byte[] getPublicKey() {
        RemoteChatSession session = base.getChatSession();
        return session == null ? null : session.profilePublicKey().data().key().getEncoded();
    }

    /**
     * Whether this player has a cape, which is the test for whether the cape texture has anything to
     * answer. A player with no cape gives {@code null} from {@link #getCapeTexture()}, since of a
     * skin's three assets the cape and the elytra are the two the game records as possibly missing
     * while the body texture is always there.<br>
     * {@link #getCapeUrl()} is deliberately not one of the calls this gates, because it reads the
     * body texture rather than the cape's and so answers exactly as {@link #getSkinUrl()} does. A
     * player wearing no cape but with a downloaded skin still gets the skin's address out of it, so
     * a {@code false} here says nothing about whether that call gives {@code null}.
     * example:
     * <pre>
     * // who is wearing a cape
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     if (entry.hasCape()) {
     *       Chat.log(`${entry.getName()}: ${entry.getCapeTexture()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the player has a cape enabled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCape() {
        //? if >1.21.8 {
        /*return base.getSkin().cape() != null;
         *///?} else {
        return base.getSkin().capeTexture() != null;
        //?}
    }

    /**
     * A slim skin is an Alex skin, while the default one is Steve. This asks about the model the
     * server chose, not about the texture, so a player with the slim model and the default one
     * would answer true here.
     * example:
     * <pre>
     * // the two player models, counted
     * const players = World.getPlayers();
     * let slim = 0;
     * let wide = 0;
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     if (players.get(i).hasSlimModel()) {
     *       slim += 1;
     *     } else {
     *       wide += 1;
     *     }
     *   }
     * }
     * Chat.log(`${slim} slim and ${wide} default`);
     * </pre>
     *
     * @return {@code true} if the player has a slim skin, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasSlimModel() {
        //? if >1.21.8 {
        /*return base.getSkin().model().equals(PlayerModelType.SLIM);
        *///?} else {
        return base.getSkin().model().equals(PlayerSkin.Model.SLIM);
        //?}
    }

    /**
     * The skin texture record as the game prints it, which is the record's own text form rather
     * than a bare id: the texture path and, for a downloaded texture, the address it came from are
     * both in it. {@link #getSkinUrl()} is the call for the address on its own.<br>
     * This is always the body texture, so a player with no skin of their own still has one here
     * rather than {@code null}.
     * example:
     * <pre>
     * // the raw skin record, and the address on its own
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   Chat.log(entry.getSkinTexture());
     *   const url = entry.getSkinUrl();
     *   if (url !== null) {
     *     Chat.log(`downloaded from ${url}`);
     *   }
     * }
     * </pre>
     *
     * @return the skin texture as the game prints it.
     * @since 1.8.4
     */


    public String getSkinTexture() {
        //? if >1.21.8 {
        /*return base.getSkin().body().toString();
        *///?} else {
        return base.getSkin().texture().toString();
        //?}
    }

    /**
     * The address the client's skin texture was downloaded from, or {@code null} if it was not
     * downloaded. The shape is the texture host plus the texture path, and the client only has an
     * address for a texture it has actually fetched, so a skin the server named but the client never
     * downloaded gives {@code null} here.
     * example:
     * <pre>
     * // which players have a downloaded skin rather than a named one
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const url = players.get(i).getSkinUrl();
     *     if (url !== null) {
     *       Chat.log(`${players.get(i).getName()}: ${url}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the url to the skin texture, or {@code null} if the client has not downloaded it.
     * @since 1.9.0
     */
    @Nullable
    public String getSkinUrl() {
        //? if >1.21.8 {
        /*return base.getSkin().body() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
        *///?} else {
        return base.getSkin().textureUrl();
        //?}
    }

    /**
     * The cape texture as the game prints it, which is the record's own text form rather than a
     * bare id, the same shape {@link #getSkinTexture()} gives. A player with no cape gives
     * {@code null} here rather than an empty string.
     * example:
     * <pre>
     * // capes, with hasCape as the test for there being one
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     if (entry.hasCape()) {
     *       Chat.log(`${entry.getName()}: ${entry.getCapeTexture()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the cape texture as the game prints it, or {@code null} if the player has no cape.
     * @since 1.8.4
     */
    @Nullable
    public String getCapeTexture() {
        //? if >1.21.8 {
        /*return base.getSkin().cape() == null ? null : base.getSkin().cape().toString();
        *///?} else {
        return base.getSkin().capeTexture() == null ? null : base.getSkin().capeTexture().toString();
        //?}
    }

    /**
     * The URL of the cape texture when it is a downloaded texture, or {@code null} if there is
     * no cape or it is not a downloaded texture. This is the cape's own URL, not the body skin URL.
     * example:
     * <pre>
     * // the url this gives, beside the skin's own
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   Chat.log(`skin url ${entry.getSkinUrl()}`);
     *   Chat.log(`cape url ${entry.getCapeUrl()}`);
     * }
     * </pre>
     *
     * @return the cape texture URL, or {@code null} when unavailable
     * @since 2.1.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getCapeUrl() {
        return base.getSkin().cape() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
     * The elytra texture as the game prints it, which is the record's own text form rather than a
     * bare id. A player whose skin has no elytra layer gives {@code null} here.
     * example:
     * <pre>
     * // who has elytra wings
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const elytra = players.get(i).getElytraTexture();
     *     if (elytra !== null) {
     *       Chat.log(`${players.get(i).getName()}: ${elytra}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the identifier of the player's elytra texture or {@code null} if it's unknown.
     * @since 1.8.4
     */
    @Nullable
    public String getElytraTexture() {
        //? if >1.21.8 {
        /*return base.getSkin().elytra() == null ? null : base.getSkin().elytra().toString();
         *///?} else {
        return base.getSkin().elytraTexture() == null ? null : base.getSkin().elytraTexture().toString();
        //?}
    }

    /**
     * The url to the elytra texture, or {@code null} if the client has not downloaded one. This is
     * the elytra layer's own address, so it differs from {@link #getSkinUrl()} for a player who has
     * both.
     * example:
     * <pre>
     * // the three addresses a skin can have
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   Chat.log(`body   ${entry.getSkinUrl()}`);
     *   Chat.log(`cape   ${entry.getCapeUrl()}`);
     *   Chat.log(`elytra ${entry.getElytraUrl()}`);
     * }
     * </pre>
     *
     * @return the url to the elytra texture, or {@code null} if the client has not downloaded one.
     * @since 2.1.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getElytraUrl() {
        return base.getSkin().elytra() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
     * The team this player is on, or {@code null} if the server has them on none. This reads the
     * scoreboard that is currently loaded, so it is a question about the client rather than about
     * the entry, and it needs a world to have been joined.
     * example:
     * <pre>
     * // group the tab list by team
     * const players = World.getPlayers();
     * if (players !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const entry = players.get(i);
     *     const team = entry.getTeam();
     *     if (team === null) {
     *       Chat.log(`${entry.getName()}: no team`);
     *     } else {
     *       Chat.log(`${entry.getName()}: ${team.getName()} in ${team.getColorName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the team of the player or {@code null} if the player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    public TeamHelper getTeam() {
        return base.getTeam() == null ? null : new TeamHelper(base.getTeam());
    }

    @Override
    public String toString() {
        return String.format("PlayerListEntryHelper:{\"uuid\": \"%s\", \"name\": \"%s\"}", this.getUUID(), this.getName());
    }

}
