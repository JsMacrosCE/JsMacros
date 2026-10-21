package com.jsmacrosce.jsmacros.client;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventQuitGame;
import com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.config.ClientProfile;
import com.jsmacrosce.jsmacros.client.event.EventRegistry;
import com.jsmacrosce.jsmacros.client.gui.screens.KeyMacrosScreen;
import com.jsmacrosce.jsmacros.client.movement.MovementQueue;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.wagyourgui.BaseScreen;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

//? if >1.21.8 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/** Client-side core, menu-key, screen-state, and screen-naming utilities; scripts can access this class through {@code Java.type}, not a global library alias.
 * <pre>
 * const ClientState = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient');
 * const screen = ClientState.prevScreen;
 * if (screen !== null) {
 *   Chat.log(ClientState.getScreenName(screen));
 * }
 * </pre>
 */
public class JsMacrosClient extends JsMacros {
    /** Menu-key category registered at class initialization as {@code jsmacrosce:jsmacrosce.title}; replacing this field does not update the existing menu binding's category. */
    //? if >1.21.8 {
    /*// TODO: This isn't properly getting the name
    public static KeyMapping.Category keyBindingCategory = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("jsmacrosce", "jsmacrosce.title"));
    *///?} else {
    /** Translated category string used when constructing the initial menu key binding; changing it later does not update that binding's category. */
    public static String keyBindingCategory = I18n.get("jsmacrosce.title");
    //?}
    /** Menu key mapping, initially bound to {@code K} in {@code keyBindingCategory}; assigning this field does not itself register a mapping. */
    public static KeyMapping keyBinding = new KeyMapping("jsmacrosce.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, keyBindingCategory);
    /** Client scripting core, with configuration rooted at the client config directory and scripts in its {@code Macros} child. */
    public static final Core<ClientProfile, EventRegistry> clientCore = new Core<>(EventRegistry::new, ClientProfile::new, configFolder.getAbsoluteFile(), new File(configFolder, "Macros"), LOGGER);

    /** Cached macro UI screen, initially {@code null}; it is reused when the menu key opens the UI. */
    public static BaseScreen prevScreen;

    /** Performs loader client setup: attempts configuration registration, clears the movement queue, optionally starts service reload listening, initializes packet-buffer helpers, then registers a quit-event shutdown hook.
     * Handled reflection and I/O failures during configuration registration are printed before continuing; later failures can abort the remaining steps. Every call that reaches shutdown-hook registration adds another hook, so this is not an idempotent reinitialization API.
     */
    public static void onInitializeClient() {
        try {
            clientCore.config.addOptions("com/jsmacrosce/jsmacros/client", ClientConfigV2.class);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IOException e) {
            e.printStackTrace();
        }

        // Init MovementQueue
        MovementQueue.clear();

        if (clientCore.config.getOptions(ClientConfigV2.class).serviceAutoReload) {
            clientCore.services.startReloadListener();
        }
        PacketByteBufferHelper.init();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new EventQuitGame().trigger()));
    }

    /** Resolves a native input-key name to its display component, falling back to a literal component containing the input on lookup or display failure.
     * @param translationKey native key name, such as {@code key.keyboard.k}
     * @return the key display component, or a literal component containing {@code translationKey} if resolution fails
     */
    static public Component getKeyText(String translationKey) {
        try {
            return InputConstants.getKey(translationKey).getDisplayName();
        } catch (Exception e) {
            return Component.literal(translationKey);
        }
    }

    // if any screen name is added or modified, check FHud#getOpenScreenName() and Inventory#is() annotation as well
    // i didn't put it here is because doclet won't check this class  -- aMelonRind
    /** Returns a screen label: recognized containers use fixed labels such as "Survival Inventory" or a row-count chest label, unrecognized containers use their fully qualified runtime class name, and chat uses "Chat".
     * Other screens use their nonempty title text, or "unknown" when the title is null or empty. The fixed labels are not localized, and the result is not a unique screen identifier.
     * @param s screen to name, or null
     * @return the selected label, or null when s is null
     */
    static public String getScreenName(Screen s) {
        return switch (s) {
            case null -> null;
            case AbstractContainerScreen<?> handledScreen -> //add more ?
                switch (handledScreen) {
                    case ContainerScreen genericContainerScreen ->
                        String.format("%d Row Chest", genericContainerScreen.getMenu().getRowCount());
                    case DispenserScreen ignored -> "3x3 Container";
                    case AnvilScreen ignored -> "Anvil";
                    case BeaconScreen ignored -> "Beacon";
                    case BlastFurnaceScreen ignored -> "Blast Furnace";
                    case BrewingStandScreen ignored -> "Brewing Stand";
                    case CraftingScreen ignored -> "Crafting Table";
                    case EnchantmentScreen ignored -> "Enchanting Table";
                    case FurnaceScreen ignored -> "Furnace";
                    case GrindstoneScreen ignored -> "Grindstone";
                    case HopperScreen ignored -> "Hopper";
                    case LoomScreen ignored -> "Loom";
                    case MerchantScreen ignored -> "Villager";
                    case ShulkerBoxScreen ignored -> "Shulker Box";
                    case SmithingScreen ignored -> "Smithing Table";
                    case SmokerScreen ignored -> "Smoker";
                    case CartographyTableScreen ignored -> "Cartography Table";
                    case StonecutterScreen ignored -> "Stonecutter";
                    case InventoryScreen ignored -> "Survival Inventory";
                    case HorseInventoryScreen ignored -> "Horse";
                    case CreativeModeInventoryScreen ignored -> "Creative Inventory";
                    default -> s.getClass().getName();
                };
            case ChatScreen ignored -> "Chat";
            default -> {
                Component t = s.getTitle();
                String ret = "";
                if (t != null) {
                    ret = t.getString();
                }
                if (ret.isEmpty()) {
                    ret = "unknown";
                }
                yield ret;
            }
        };
    }

    @DocletIgnore
    @Deprecated
    static public String getLocalizedName(InputConstants.Key keyCode) {
        return I18n.get(keyCode.getName());
    }

    @DocletIgnore
    @Deprecated
    static public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

}
