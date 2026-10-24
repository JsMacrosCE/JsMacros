package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.util.PacketBufferCompat;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public final class PacketBufferCompatTest {
    private static int checks;

    public static void main(String[] args) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PacketBufferCompat.writeCollection(buf, List.of(1, 127, 128), FriendlyByteBuf::writeVarInt);
            check(Arrays.equals(bytes(buf), new byte[]{3, 1, 127, (byte) 128, 1}), "collection wire format");
            check(PacketBufferCompat.readList(buf, FriendlyByteBuf::readVarInt).equals(List.of(1, 127, 128)), "collection roundtrip");
            buf.clear();
            LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
            map.put(2, 4);
            map.put(1, 3);
            PacketBufferCompat.writeMap(buf, map, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeVarInt);
            check(Arrays.equals(bytes(buf), new byte[]{2, 2, 4, 1, 3}), "map preserves iteration order on wire");
            check(PacketBufferCompat.readMap(buf, FriendlyByteBuf::readVarInt, FriendlyByteBuf::readVarInt).equals(map), "map roundtrip");
            buf.clear();
            PacketBufferCompat.writeIntList(buf, new IntArrayList(new int[]{-1, 0, 128}));
            check(PacketBufferCompat.readIntList(buf).equals(new IntArrayList(new int[]{-1, 0, 128})), "signed integer list");
            buf.clear();
            buf.writeVarInt(-1);
            check(PacketBufferCompat.readIntList(buf).isEmpty(), "legacy negative int-list count");
            buf.clear();
            buf.writeVarInt(-1);
            expect(IllegalArgumentException.class, () -> PacketBufferCompat.readList(buf, FriendlyByteBuf::readVarInt));
            buf.clear();
            buf.writeVarInt(2).writeVarInt(1).writeVarInt(3).writeVarInt(1).writeVarInt(4);
            check(PacketBufferCompat.readMap(buf, FriendlyByteBuf::readVarInt, FriendlyByteBuf::readVarInt).get(1) == 4, "duplicate map keys overwrite");
            buf.clear();
            BlockHitResult hit = new BlockHitResult(new Vec3(-10.25, 64.5, 21.75), Direction.WEST, new BlockPos(-11, 64, 21), true, true);
            PacketBufferCompat.writeBlockHitResult(buf, hit);
            BlockHitResult decoded = PacketBufferCompat.readBlockHitResult(buf);
            check(decoded.getBlockPos().equals(hit.getBlockPos()) && decoded.getDirection() == hit.getDirection(), "block hit position and face");
            check(decoded.getLocation().equals(hit.getLocation()) && decoded.isInside() && decoded.isWorldBorderHit(), "block hit offsets and flags");
            buf.clear();
            buf.writeBlockPos(BlockPos.ZERO).writeVarInt(99);
            expect(ArrayIndexOutOfBoundsException.class, () -> PacketBufferCompat.readBlockHitResult(buf));
        } finally {
            buf.release();
        }
        System.out.println("PacketBufferCompat: " + checks + " wire-format and error checks passed");
    }

    private static byte[] bytes(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return bytes;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(type.isInstance(thrown), "expected " + type.getName() + ", got " + thrown);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }
}
