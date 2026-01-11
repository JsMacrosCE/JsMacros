package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.gui.components.LerpingBossEvent;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.BossBarHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Bossbar", oldName = "BOSSBAR_UPDATE")
public class EventBossbar extends BaseEvent {
    @Nullable
    public final BossBarHelper bossBar;
    public final String uuid;
    @DocletReplaceReturn("BossBarUpdateType")
    @DocletDeclareType(name = "BossBarUpdateType", type =
            """
            'ADD' | 'REMOVE' | 'UPDATE_PERCENT'
            | 'UPDATE_NAME' | 'UPDATE_STYLE' | 'UPDATE_PROPERTIES'
            """
    )
    public final String type;

    public EventBossbar(String type, UUID uuid, LerpingBossEvent bossBar) {
        super(JsMacrosClient.clientCore);
        if (bossBar != null) {
            this.bossBar = new BossBarHelper(bossBar);
        } else {
            this.bossBar = null;
        }
        this.uuid = uuid.toString();
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"bossBar\": %s}", this.getEventName(), bossBar != null ? bossBar.toString() : uuid);
    }

}
