package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A server-list entry containing its configured name and address and status information from a
 * server ping. Scripts can obtain one with {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#ping(java.lang.String)},
 * which looks up a server without joining it. Before a ping, protocol defaults to the client's
 * protocol and ping is zero before a ping; the player sample starts empty.
 * <pre>
 * const info = Client.ping("mc.hypixel.net");
 * Chat.log(`${info.getName()} at ${info.getAddress()}`);
 * Chat.log(`${info.getLabel()} -- ${info.getPlayerCountLabel()}`);
 * Chat.log(`version ${info.getVersion()}, protocol ${info.getProtocolVersion()}, ping ${info.getPing()}ms`);
 *
 * // the icon the client downloaded for this entry, if it got one
 * const icon = info.getIcon();
 * if (icon !== null) {
 *   Chat.log(`icon is ${icon.length} bytes`);
 * }
 * </pre>
 *
 * @since 1.6.5
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class ServerInfoHelper extends BaseHelper<ServerData> {

    /** Wraps the supplied server-list data without performing a ping or lookup.
     * @param base raw server data to wrap
     */
    public ServerInfoHelper(ServerData base) {
        super(base);
    }

    /**
     * The configured server-list name; this is not the name reported by the server.
     *
     * @return the entry's name.
     * @since 1.6.5
     */
    public String getName() {
        return base.name;
    }

    /**
     * The configured server address.
     *
     * @return the entry's address.
     * @since 1.6.5
     */
    public String getAddress() {
        return base.ip;
    }

    /**
     * The player-count status from the last ping; if no player section is supplied, the client
     * sets a translated placeholder.
     *
     * @return the player count line, as text.
     * @since 1.6.5
     */
    public TextHelper getPlayerCountLabel() {
        return TextHelper.wrap(base.status);
    }

    /**
     * The server's description (MOTD) returned by the last ping.
     *
     * @return the server's description, as text.
     * @since 1.6.5
     */
    public TextHelper getLabel() {
        return TextHelper.wrap(base.motd);
    }

    /**
     * The client's measured duration of the last ping, in milliseconds; zero before a ping.
     *
     * @return the measured ping in milliseconds.
     * @since 1.6.5
     */
    public long getPing() {
        return base.ping;
    }

    /**
     * The protocol version reported by the last ping, or {@code 0} if the server is too old to
     * report a version. Before a ping, this defaults to the running client's protocol version.
     *
     * @return the protocol version recorded for this entry.
     * @since 1.6.5
     */
    public int getProtocolVersion() {
        return base.protocol;
    }

    /**
     * The game version reported by the last ping, or a translated placeholder if the server is too
     * old to report a version.
     *
     * @return the reported version, as text.
     * @since 1.6.5
     */
    public TextHelper getVersion() {
        return TextHelper.wrap(base.version);
    }

    /**
     * The sample of player names supplied by the server. An empty list does not establish that no
     * players are online; entries retain the text and formatting supplied by the server.
     *
     * @return the sample of online names the server sent, as a list of text.
     * @since 1.6.5
     */
    public List<TextHelper> getPlayerListSummary() {
        return base.playerList.stream().map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * The translation key for this entry's resource-pack setting (enabled, disabled, or prompt).
     *
     * @return the translation key for this entry's resource pack setting.
     * @since 1.6.5
     */
    public String resourcePackPolicy() {
        return ((TranslatableContents) base.getResourcePackStatus().getName().getContents()).getKey();
    }

    /**
     * The server-list icon bytes, or {@code null} if no icon is stored.
     *
     * @return the downloaded icon bytes, or {@code null} if there is no icon.
     * @since 1.6.5
     */
    public byte[] getIcon() {
        return base.getIconBytes();
    }

    /**
     * Whether this entry is not a LAN entry. Realm and ordinary server entries both return true.
     *
     * @return {@code true} if this entry is not a local network entry.
     * @since 1.6.5
     */
    public boolean isOnline() {
        return !base.isLan();
    }

    /**
     * Whether this entry is a LAN entry.
     *
     * @return {@code true} if this entry is a local network entry.
     * @since 1.6.5
     */
    public boolean isLocal() {
        return base.isLan();
    }

    /**
     * The server-list compound written by the game, containing the name, address, optional base64 icon, and resource-pack setting.
     *
     * @return the entry as it is written to the server list file.
     * @since 1.6.5
     */
    public NBTElementHelper.NBTCompoundHelper getNbt() {
        return NBTElementHelper.wrapCompound(base.write());
    }

    /** Returns a diagnostic representation containing this entry's NBT representation.
     * @return diagnostic representation of the wrapped server entry
     */
    @Override
    public String toString() {
        return "ServerInfoHelper:{" + getNbt().asString() + "}";
    }

}
