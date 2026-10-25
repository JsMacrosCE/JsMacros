package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;

/**
 * Provides access to the block state displayed by a block display entity. The state is
 * {@code null} until the client creates its render state.
 * <pre>
 * const blocks = World.getEntities(32, "block_display");
 * if (blocks !== null) {
 *   for (const entity of blocks) {
 *     if (!(entity instanceof EntityHelpers.BlockDisplay)) continue;
 *     const state = entity.getBlockState();
 *     if (state === null) {
 *       continue;
 *     }
 *     Chat.log(`showing ${state.getId()} at ${entity.getPos()}`);
 *   }
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class BlockDisplayEntityHelper extends DisplayEntityHelper<Display.BlockDisplay> {

    /** Wraps the supplied block display entity without copying it.
     * @param base block display entity to wrap
     */
    public BlockDisplayEntityHelper(Display.BlockDisplay base) {
        super(base);
    }

    /**
     * Returns the block state held by the entity's client render state.
     *
     * @return the displayed block state, or {@code null} before the client creates its render state
     * @since 1.9.1
     */
    @Nullable
    public BlockStateHelper getBlockState() {
        Display.BlockDisplay.BlockRenderState data = base.blockRenderState();
        if (data == null) return null;
        return new BlockStateHelper(data.blockState());
    }

}
