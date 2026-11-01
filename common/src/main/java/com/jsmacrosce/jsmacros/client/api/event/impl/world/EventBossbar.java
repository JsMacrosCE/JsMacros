package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.gui.components.LerpingBossEvent;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.BossBarHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * Reports each boss-bar packet action after the client handles it. {@link #type} identifies the
 * action; {@link #bossBar} is the full bar state when available.
 * <pre>
 * JsMacros.on("Bossbar", JavaWrapper.methodToJava(function (event) {
 *   if (event.bossBar === null) {
 *     Chat.log(`Bossbar ${event.uuid} (${event.type}) is not on screen`);
 *     return;
 *   }
 *   const bar = event.bossBar;
 *   Chat.log(`${event.type} ${bar.getName().getString()} ${(bar.getPercent() * 100).toFixed(1)}% (${bar.getColor()})`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Render/UI")
@Event(value = "Bossbar", oldName = "BOSSBAR_UPDATE")
public class EventBossbar extends BaseEvent {
    /**
     * The full boss bar state, or {@code null} if the overlay has no bar for this UUID. A
     * {@code 'REMOVE'} event always has {@code null}; other update types may also have {@code null}.
     * When non-null, it exposes the bar's
     * {@link BossBarHelper#getName() getName()},
     * {@link BossBarHelper#getPercent() getPercent()},
     * {@link BossBarHelper#getColor() getColor()} and
     * {@link BossBarHelper#getStyle() getStyle()}.
     */
    @Nullable
    public final BossBarHelper bossBar;
    /**
     * The bar UUID as a string, available even when {@link #bossBar} is {@code null}.
     */
    public final String uuid;
    /**
     * The boss-bar packet action.
     * {@code 'ADD'} for a new bar, {@code 'REMOVE'} when it is taken away, and
     * {@code 'UPDATE_PERCENT'}, {@code 'UPDATE_NAME'}, {@code 'UPDATE_STYLE'} or
     * {@code 'UPDATE_PROPERTIES'} for the respective changes to an existing bar.
     */
    @DocletReplaceReturn("BossBarUpdateType")
    @DocletDeclareType(name = "BossBarUpdateType", type =
            """
            'ADD' | 'REMOVE' | 'UPDATE_PERCENT'
            | 'UPDATE_NAME' | 'UPDATE_STYLE' | 'UPDATE_PROPERTIES'
            """
    )
    public final String type;

    /**
     * Initializes this event from the supplied action, bar identifier, and optional raw bar.
     * A non-null bar is wrapped in a {@link BossBarHelper}; a null bar leaves {@link #bossBar}
     * null. The supplied action is stored unchanged and is not validated against the bar or
     * identifier. A null identifier throws {@code NullPointerException} while converting it to a string.
     * @param type supplied action label; arbitrary values, including {@code null}, are stored unchanged
     * @param uuid identifier for the bar; must be non-null
     * @param bossBar raw bar to wrap, or {@code null} when no bar is available
     */
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

    /**
     * Formats a diagnostic string containing the event name and the current helper's string, or
     * the stored UUID string when no helper is present. The fallback is unquoted and is not a
     * standalone UUID field or a JSON serialization.
     * @return event name followed by {@code :{"bossBar": %s}}, with {@code %s} replaced by the helper string or UUID string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"bossBar\": %s}", this.getEventName(), bossBar != null ? bossBar.toString() : uuid);
    }

}
