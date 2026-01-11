package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class HitResultHelper<T extends HitResult> extends BaseHelper<T> {

    @Nullable
    public static HitResultHelper<?> resolve(@Nullable HitResult hr) {
        if (hr == null) return null;
        return switch (hr.getType()) {
            case MISS, BLOCK -> new Block((BlockHitResult) hr);
            case ENTITY -> new Entity((EntityHitResult) hr);
            //noinspection UnnecessaryDefault
            default -> new HitResultHelper<>(hr);
        };
    }

    protected HitResultHelper(T base) {
        super(base);
    }

    public Pos3D getPos() {
        var pos = base.getLocation();
        return new Pos3D(pos.x, pos.y, pos.z);
    }

    @Nullable
    public Block asBlock() {
        return null;
    }

    @Nullable
    public Entity asEntity() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("HitResultHelper:{\"pos\": %s}", getPos());
    }

    public static class Block extends HitResultHelper<BlockHitResult> {

        public Block(BlockHitResult base) {
            super(base);
        }

        @Nullable
        public DirectionHelper getSide() {
            Direction dir = base.getDirection();
            return dir == null ? null : new DirectionHelper(dir);
        }

        @Nullable
        public BlockPosHelper getBlockPos() {
            BlockPos pos = base.getBlockPos();
            return pos == null ? null : new BlockPosHelper(pos);
        }

        public boolean isMissed() {
            return base.getType() == HitResult.Type.MISS;
        }

        public boolean isInsideBlock() {
            return base.isInside();
        }

        @Override
        public Block asBlock() {
            return this;
        }

        @Override
        public String toString() {
            return String.format("HitResultHelper$Block:{\"pos\": %s, \"side\": %s, \"blockPos\": %s, \"missed\": %s, \"insideBlock\": %s}", getPos(), base.getDirection(), getBlockPos(), isMissed(), isInsideBlock());
        }

    }

    public static class Entity extends HitResultHelper<EntityHitResult> {

        public Entity(EntityHitResult base) {
            super(base);
        }

        public EntityHelper<?> getEntity() {
            return EntityHelper.create(base.getEntity());
        }

        @Override
        public Entity asEntity() {
            return this;
        }

        @Override
        public String toString() {
            return String.format("HitResultHelper:{\"pos\": %s, \"entity\": %s}", getPos(), getEntity().getType());
        }

    }

}
