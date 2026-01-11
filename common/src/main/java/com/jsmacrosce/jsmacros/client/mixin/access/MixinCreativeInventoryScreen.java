package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(CreativeModeInventoryScreen.class)
public interface MixinCreativeInventoryScreen {

    // Don't make this static, it will disable the compile and reload feature!
    @Accessor
    CreativeModeTab getSelectedTab();

    @Accessor("scrollOffs")
    float getScrollPosition();

    @Accessor
    EditBox getSearchBox();

    @Invoker("selectTab")
    void invokeSetSelectedTab(CreativeModeTab group);

    @Invoker("refreshSearchResults")
    void invokeSearch();

    @Invoker("canScroll")
    boolean invokeHasScrollbar();

}
