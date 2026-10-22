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
 * A client-side tab-list entry for a player. Obtain entries with {@code World.getPlayers()} or
 * look one up by account name with {@code World.getPlayerEntry(name)}. The entry exposes profile,
 * latency, game-mode, skin, chat-session, and team data; it does not require the player to be
 * loaded. Skin texture methods return texture records, while URL methods return download addresses.
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

    /** Wraps the supplied player-list entry, retaining its reference.
     * @param p player-list entry to wrap
     */
    public PlayerListEntryHelper(PlayerInfo p) {
        super(p);
    }

    /**
     * The account UUID from this entry's profile, formatted as a UUID string.
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
     * The account name from this entry's profile; this is the name accepted by
     * {@code World.getPlayerEntry(name)}.
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
     * The latency value stored for this entry, in milliseconds.
     *
     * @return the player's ping in milliseconds.
     * @since 1.6.5
     */
    public int getPing() {
        return base.getLatency();
    }

    /**
     * The entry's game mode name. Minecraft initializes entries with the default game mode and
     * updates it when a mode is set.
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
     * The optional tab-list display name, including any formatting supplied in its text component.
     *
     * @return the name shown for this player in the tab list, or {@code null} if the server did
     * not set one.
     * @since 1.1.9
     */
    public TextHelper getDisplayText() {
        return TextHelper.wrap(base.getTabListDisplayName());
    }

    /**
     * The encoded public key from this entry's remote chat session, or {@code null} if it has no
     * session.
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
     * Whether the entry's skin has a cape texture. This indicates cape presence, not whether a cape
     * download URL is available.
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
     * Whether the entry's skin model is the slim model.
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
     * The body's skin texture as the texture record's string form; unlike {@link #getSkinUrl()},
     * this is not just its download address. The default skin also has a texture record.
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
     * The body's skin download URL, or {@code null} when no URL is available.
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
     * The cape texture's string form, or {@code null} if the skin has no cape texture.
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
     * The cape texture's download URL, or {@code null} when unavailable. This method is only
     * available on Minecraft versions newer than 1.21.8.
     *
     * @return the cape texture URL, or {@code null} when unavailable
     * @since 2.0.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getCapeUrl() {
        return base.getSkin().cape() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
     * The elytra texture's string form, or {@code null} if the skin has no elytra texture.
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
     * The elytra texture's download URL, or {@code null} when unavailable. This method is only
     * available on Minecraft versions newer than 1.21.8.
     *
     * @return the url to the elytra texture, or {@code null} if the client has not downloaded one.
     * @since 2.0.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getElytraUrl() {
        return base.getSkin().elytra() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
     * The player's team in the client's loaded scoreboard, or {@code null} if the player is not on
     * a team. A world must be loaded to query the scoreboard.
     *
     * @return the team of the player or {@code null} if the player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    public TeamHelper getTeam() {
        return base.getTeam() == null ? null : new TeamHelper(base.getTeam());
    }

    /** Formats a diagnostic string from this entry's UUID and account name as
     * {@code PlayerListEntryHelper:{"uuid": "%s", "name": "%s"}}. The quoted values are
     * inserted with {@code String.format} and are not JSON-escaped.
     * @return diagnostic string with the entry UUID and account name
     */
    @Override
    public String toString() {
        return String.format("PlayerListEntryHelper:{\"uuid\": \"%s\", \"name\": \"%s\"}", this.getUUID(), this.getName());
    }

}
