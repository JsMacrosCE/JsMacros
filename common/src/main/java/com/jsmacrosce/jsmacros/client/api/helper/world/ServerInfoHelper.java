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
 * One entry from the server list, and everything the client knows about a server from pinging it.
 * That covers the two things that are not the same: the name and address, which were typed in when
 * the entry was made, and the rest, which is whatever came back the last time the client looked the
 * server up.<br>
 * A script gets one from {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#ping(java.lang.String)},
 * which pings a server without joining it. An entry that has never been pinged still answers every
 * call here, though several of the answers are only the client's own defaults rather than anything
 * the server said: {@link #getProtocolVersion()} starts out as the running client's own version
 * and {@link #getPing()} at zero, and {@link #getPlayerCountLabel()} and
 * {@link #getPlayerListSummary()} have nothing in them to give.
 * example:
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

    public ServerInfoHelper(ServerData base) {
        super(base);
    }

    /**
     * The name the entry is listed under in the server list, which is whatever was typed into the
     * name box and has nothing to do with what the server calls itself.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(info.getName());
     * </pre>
     *
     * @return the entry's name.
     * @since 1.6.5
     */
    public String getName() {
        return base.name;
    }

    /**
     * The address the client connects to, which is the host with a port appended when the pinged
     * address named one.
     * example:
     * <pre>
     * // what to type to join what was just pinged
     * const info = Client.ping("localhost:25566");
     * Chat.log(`join with ${info.getAddress()}`);
     * </pre>
     *
     * @return the entry's address.
     * @since 1.6.5
     */
    public String getAddress() {
        return base.ip;
    }

    /**
     * The player count line the client built from the last ping, which is the two numbers of the
     * online and maximum counts put together and translated. A server that answered without a
     * player section gives a placeholder here rather than a count.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(`players: ${info.getPlayerCountLabel()}`);
     *
     * // compare two servers
     * for (const host of ["mc.hypixel.net", "play.cubecraft.net"]) {
     *   const entry = Client.ping(host);
     *   Chat.log(`${entry.getAddress()} -- ${entry.getPlayerCountLabel()}`);
     * }
     * </pre>
     *
     * @return the player count line, as text.
     * @since 1.6.5
     */
    public TextHelper getPlayerCountLabel() {
        return TextHelper.wrap(base.status);
    }

    /**
     * The description the server sent, which is the multi-line text a server list shows under the
     * name. The client strips the formatting it does not allow out of it before storing it, so what
     * comes back has been through that pass.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * const label = info.getLabel();
     * Chat.log(label.getString());
     * // and the same text with the colour codes taken out
     * Chat.log(label.getStringStripFormatting());
     * </pre>
     *
     * @return the server's description, as text.
     * @since 1.6.5
     */
    public TextHelper getLabel() {
        return TextHelper.wrap(base.motd);
    }

    /**
     * How long the last ping took, in milliseconds, as the client measured it. An entry that has
     * never been pinged reports zero rather than anything meaningful.
     * example:
     * <pre>
     * // rank a few servers by how close they are
     * const hosts = ["mc.hypixel.net", "play.cubecraft.net", "localhost"];
     * const measured = [];
     * for (const host of hosts) {
     *   const entry = Client.ping(host);
     *   measured.push({ host: host, ping: entry.getPing() });
     * }
     * measured.sort(function (a, b) { return a.ping - b.ping; });
     * for (const m of measured) {
     *   Chat.log(`${m.host}: ${m.ping}ms`);
     * }
     * </pre>
     *
     * @return the measured ping in milliseconds.
     * @since 1.6.5
     */
    public long getPing() {
        return base.ping;
    }

    /**
     * The protocol version the last ping reported. A server too old to name a version reports
     * {@code 0} here, and an entry that has never been pinged reports the running client's own
     * protocol version, since that is the value the field starts out holding.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(`server protocol ${info.getProtocolVersion()}`);
     *
     * // zero means the server did not report a version at all
     * if (info.getProtocolVersion() === 0) {
     *   Chat.log("that server did not report a protocol version");
     * }
     * </pre>
     *
     * @return the protocol version recorded for this entry.
     * @since 1.6.5
     */
    public int getProtocolVersion() {
        return base.protocol;
    }

    /**
     * The version name the server reported, which is the game version it says it is running. A
     * server that named no version gets a translated placeholder rather than an empty string.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(`that server says it is ${info.getVersion()}`);
     * </pre>
     *
     * @return the reported version, as text.
     * @since 1.6.5
     */
    public TextHelper getVersion() {
        return TextHelper.wrap(base.version);
    }

    /**
     * The names the server put in its ping as a sample of who is online. A server is free to send
     * an empty sample, and many do, so an empty list here says nothing about whether anybody is
     * connected. What is on the list is each entry's own text, including any formatting the
     * server chose to send.
     * example:
     * <pre>
     * const info = Client.ping("mc.hypixel.net");
     * const sample = info.getPlayerListSummary();
     * Chat.log(`the server listed ${sample.size()} names`);
     * for (let i = 0; i !== sample.size(); i += 1) {
     *   Chat.log(`  ${sample.get(i).getString()}`);
     * }
     * </pre>
     *
     * @return the sample of online names the server sent, as a list of text.
     * @since 1.6.5
     */
    public List<TextHelper> getPlayerListSummary() {
        return base.playerList.stream().map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * The resource pack setting for this entry as a translation key rather than as text, so there
     * are three of them: {@code manageServer.resourcePack.enabled},
     * {@code manageServer.resourcePack.disabled} and
     * {@code manageServer.resourcePack.prompt}. The key is what comes back here because that is
     * what the setting is stored as, and the game is the thing that turns it into a word in the
     * right language.
     * example:
     * <pre>
     * // a resource pack prompt is the one worth warning about
     * const info = Client.ping("mc.hypixel.net");
     * const policy = info.resourcePackPolicy();
     * if (policy.endsWith("prompt")) {
     *   Chat.log("this server will ask before sending a resource pack");
     * }
     * Chat.log(policy);
     * </pre>
     *
     * @return the translation key for this entry's resource pack setting.
     * @since 1.6.5
     */
    public String resourcePackPolicy() {
        return ((TranslatableContents) base.getResourcePackStatus().getName().getContents()).getKey();
    }

    /**
     * The icon the client downloaded for this entry, as raw bytes, or {@code null} when there is
     * none. An entry that has never been pinged, or one whose server sent no icon, gives
     * {@code null} here, so this is the test rather than a length check.
     * example:
     * <pre>
     * // how big the icon is, if the entry has one
     * const info = Client.ping("mc.hypixel.net");
     * const icon = info.getIcon();
     * if (icon === null) {
     *   Chat.log("no icon for this entry");
     * } else {
     *   Chat.log(`icon is ${icon.length} bytes`);
     * }
     * </pre>
     *
     * @return the downloaded icon bytes, or {@code null} if there is no icon.
     * @since 1.6.5
     */
    public byte[] getIcon() {
        return base.getIconBytes();
    }

    /**
     * Whether this entry is one that is joined by address rather than opened on the local network.
     * A realm entry is not a local one, so this answers true for a realm even though it is not an
     * address at all; the other half of the question is {@link #isLocal()}.
     * example:
     * <pre>
     * const info = Client.ping("localhost:25566");
     * if (info.isLocal()) {
     *   Chat.log("that is a server on this network");
     * } else {
     *   Chat.log("that one is out on the internet");
     * }
     * </pre>
     *
     * @return {@code true} if this entry is not a local network entry.
     * @since 1.6.5
     */
    public boolean isOnline() {
        return !base.isLan();
    }

    /**
     * Whether this entry is one that was opened from the local network tab of the server list,
     * rather than typed in as an address.
     * example:
     * <pre>
     * const info = Client.ping("localhost:25566");
     * if (info.isLocal()) {
     *   Chat.log(`${info.getName()} came from the local network list`);
     * }
     * </pre>
     *
     * @return {@code true} if this entry is a local network entry.
     * @since 1.6.5
     */
    public boolean isLocal() {
        return base.isLan();
    }

    /**
     * The entry as the game writes it out to the server list file: the name, the address, the icon
     * as base64, the resource pack setting and the code of conduct flag. This is the stored form of
     * the entry rather than everything the client learned from pinging it, so the ping, the version
     * and the player count are not in it.
     * example:
     * <pre>
     * // read back what would be written to the server list file
     * const info = Client.ping("mc.hypixel.net");
     * const nbt = info.getNbt();
     * for (const key of nbt.getKeys()) {
     *   Chat.log(`${key} is ${nbt.asString(key)}`);
     * }
     * </pre>
     *
     * @return the entry as it is written to the server list file.
     * @since 1.6.5
     */
    public NBTElementHelper.NBTCompoundHelper getNbt() {
        return NBTElementHelper.wrapCompound(base.write());
    }

    @Override
    public String toString() {
        return "ServerInfoHelper:{" + getNbt().asString() + "}";
    }

}
