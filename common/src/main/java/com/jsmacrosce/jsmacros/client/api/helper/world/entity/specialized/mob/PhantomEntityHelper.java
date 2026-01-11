package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Phantom;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class PhantomEntityHelper extends MobEntityHelper<Phantom> {

    public PhantomEntityHelper(Phantom base) {
        super(base);
    }

    /**
     * @return the size of this phantom.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPhantomSize();
    }

}
