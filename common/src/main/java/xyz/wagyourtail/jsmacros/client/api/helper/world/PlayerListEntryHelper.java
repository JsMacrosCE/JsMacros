package xyz.wagyourtail.jsmacros.client.api.helper.world;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.api.helper.TextHelper;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

//? if >1.21.8 {
/*import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
*///?} else {
import net.minecraft.client.resources.PlayerSkin;
//?}

/**
 * @author Wagyourtail
 * @since 1.0.2
 */
@SuppressWarnings("unused")
public class PlayerListEntryHelper extends BaseHelper<PlayerInfo> {

    public PlayerListEntryHelper(PlayerInfo p) {
        super(p);
    }

    /**
     * @return
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
     * @return
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
     * @return
     * @since 1.6.5
     */
    public int getPing() {
        return base.getLatency();
    }

    /**
     * @return null if unknown
     * @since 1.6.5
     */
    @DocletReplaceReturn("Gamemode")
    @Nullable
    public String getGamemode() {
        GameType gm = base.getGameMode();
        return gm.getName();
    }

    /**
     * @return
     * @since 1.1.9
     */
    public TextHelper getDisplayText() {
        return TextHelper.wrap(base.getTabListDisplayName());
    }

    /**
     * @return
     * @since 1.8.2
     */
    public byte[] getPublicKey() {
        return base.getChatSession().profilePublicKey().data().key().getEncoded();
    }

    /**
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
     * A slim skin is an Alex skin, while the default one is Steve.
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
     * @return the identifier of the player's skin texture or {@code null} if it's unknown.
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
     * @return The url to the skin texture in this format: {@code http://textures.minecraft.net/texture/<hash>} or {@code null} if the entry does not have a ClientAsset.DownloadedTexture
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
     * @return The identifier of the player's cape texture or {@code null} if it's unknown.
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
     * @return The url to the cape texture in this format: {@code http://textures.minecraft.net/texture/<hash>} or {@code null} if the entry does not have a ClientAsset.DownloadedTexture
     * @since 2.1.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getCapeUrl() {
        return base.getSkin().body() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
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
     * @return The url to the cape texture in this format: {@code http://textures.minecraft.net/texture/<hash>} or {@code null} if the entry does not have a ClientAsset.DownloadedTexture
     * @since 2.1.0
     */
    //? if >1.21.8 {
    /*@Nullable
    public String getElytraUrl() {
        return base.getSkin().elytra() instanceof ClientAsset.DownloadedTexture downloadedTexture ? downloadedTexture.url() : null;
    }
    *///?}

    /**
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
