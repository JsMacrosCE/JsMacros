package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class ServerInfoHelper extends BaseHelper<ServerData> {

    public ServerInfoHelper(ServerData base) {
        super(base);
    }

    public String getName() {
        return base.name;
    }

    public String getAddress() {
        return base.ip;
    }

    public TextHelper getPlayerCountLabel() {
        return TextHelper.wrap(base.status);
    }

    public TextHelper getLabel() {
        return TextHelper.wrap(base.motd);
    }

    public long getPing() {
        return base.ping;
    }

    public int getProtocolVersion() {
        return base.protocol;
    }

    public TextHelper getVersion() {
        return TextHelper.wrap(base.version);
    }

    public List<TextHelper> getPlayerListSummary() {
        return base.playerList.stream().map(TextHelper::wrap).collect(Collectors.toList());
    }

    public String resourcePackPolicy() {
        return ((TranslatableContents) base.getResourcePackStatus().getName().getContents()).getKey();
    }

    public byte[] getIcon() {
        return base.getIconBytes();
    }

    public boolean isOnline() {
        return !base.isLan();
    }

    public boolean isLocal() {
        return base.isLan();
    }

    public NBTElementHelper.NBTCompoundHelper getNbt() {
        return NBTElementHelper.wrapCompound(base.write());
    }

    @Override
    public String toString() {
        return "ServerInfoHelper:{" + getNbt().asString() + "}";
    }

}
