package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class ItemDisplayEntityHelper extends DisplayEntityHelper<Display.ItemDisplay> {

    public ItemDisplayEntityHelper(Display.ItemDisplay base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getSlot(0).get());
    }

    /**
     * @return "none", "thirdperson_lefthand", "thirdperson_righthand", "firstperson_lefthand",
     *         "firstperson_righthand", "head", "gui", "ground" or "fixed"
     * @since 1.9.1
     */
    @SuppressWarnings("SpellCheckingInspection")
    @Nullable
    public String getTransform() {
        Display.ItemDisplay.ItemRenderState data = base.itemRenderState();
        if (data == null) return null;
        return data.itemTransform().getSerializedName();
    }

}
