package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ItemFrameEntityHelper extends EntityHelper<ItemFrame> {

    public ItemFrameEntityHelper(ItemFrame base) {
        super(base);
    }

    /**
     * @return {@code true} if the item frame is glowing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isGlowingFrame() {
        return base instanceof GlowItemFrame;
    }

    /**
     * @return the rotation of the item inside this frame.
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getRotation();
    }

    /**
     * @return the item inside this item frame.
     * @since 1.8.4
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getItem());
    }

}
