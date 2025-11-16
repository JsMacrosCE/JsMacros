package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AnvilScreen.class)
public interface MixinAnvilScreen {

    @Accessor("name")
    EditBox getNameField();

}
