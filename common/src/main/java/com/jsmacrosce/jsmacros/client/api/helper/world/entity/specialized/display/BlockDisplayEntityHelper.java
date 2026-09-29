package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;

/**
 * a display entity that shows a block, which is what the {@code block_display} entity is for
 * and what most hologram builds are made of.
 * <p>
 * The block is the only thing this class adds on top of the positioning, facing, light and
 * shadow that every display entity shares, and it is reached through
 * {@link #getBlockState() getBlockState()}, which can be {@code null}.
 * example:
 * <pre>
 * const BlockDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.BlockDisplayEntityHelper");
 * const blocks = World.getEntities(32, "block_display");
 * if (blocks !== null) {
 *   for (const entity of blocks) {
 *     const display = BlockDisplayEntityHelper.class.cast(entity);
 *     const state = display.getBlockState();
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

    public BlockDisplayEntityHelper(Display.BlockDisplay base) {
        super(base);
    }

    /**
     * the block the display is showing, as a block state rather than as a block id.
     * <p>
     * A state carries more than the id does, and for a block such as a chest or a banner the
     * two are genuinely different things: the id says what the block is and the state says
     * which way it is facing and which half is open. This is the client's render state, so it
     * is {@code null} until the client has built it once, which can be a tick or two after the
     * entity itself appears.
     * example:
     * <pre>
     * const BlockDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.BlockDisplayEntityHelper");
     * const blocks = World.getEntities(32, "block_display");
     * if (blocks !== null) {
     *   for (const entity of blocks) {
     *     const display = BlockDisplayEntityHelper.class.cast(entity);
     *     const state = display.getBlockState();
     *     if (state === null) {
     *       continue;
     *     }
          *     // the state, not just the id, so the block's own properties come with it
     *     if (state.hasBlockEntity()) {
     *       Chat.log(`${state.getId()} carries a block entity, and gives off light ${state.getLuminance()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the block state being displayed, or {@code null} if the client has not built it
     * @since 1.9.1
     */
    @Nullable
    public BlockStateHelper getBlockState() {
        Display.BlockDisplay.BlockRenderState data = base.blockRenderState();
        if (data == null) return null;
        return new BlockStateHelper(data.blockState());
    }

}
