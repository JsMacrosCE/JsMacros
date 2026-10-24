package com.jsmacrosce.jsmacros.client.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jsmacrosce.doclet.DocletIgnore;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// These script wire formats outlive FriendlyByteBuf's convenience methods.
@DocletIgnore
public final class PacketBufferCompat {
    private PacketBufferCompat() {
    }

    public static <T> void writeCollection(FriendlyByteBuf buf, Collection<T> values,
                                           StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        buf.writeVarInt(values.size());
        for (T value : values) encoder.encode(buf, value);
    }

    public static <T> List<T> readList(FriendlyByteBuf buf, StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        int count = buf.readVarInt();
        List<T> values = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) values.add(decoder.decode(buf));
        return values;
    }

    public static void writeIntList(FriendlyByteBuf buf, IntList values) {
        buf.writeVarInt(values.size());
        values.forEach(buf::writeVarInt);
    }

    public static IntList readIntList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        IntList values = new IntArrayList();
        for (int i = 0; i < count; i++) values.add(buf.readVarInt());
        return values;
    }

    public static <K, V> void writeMap(FriendlyByteBuf buf, Map<K, V> values,
                                      StreamEncoder<? super FriendlyByteBuf, K> keyEncoder,
                                      StreamEncoder<? super FriendlyByteBuf, V> valueEncoder) {
        buf.writeVarInt(values.size());
        values.forEach((key, value) -> {
            keyEncoder.encode(buf, key);
            valueEncoder.encode(buf, value);
        });
    }

    public static <K, V> Map<K, V> readMap(FriendlyByteBuf buf,
                                         StreamDecoder<? super FriendlyByteBuf, K> keyDecoder,
                                         StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
        int count = buf.readVarInt();
        Map<K, V> values = Maps.newHashMapWithExpectedSize(count);
        for (int i = 0; i < count; i++) values.put(keyDecoder.decode(buf), valueDecoder.decode(buf));
        return values;
    }

    public static PublicKey readPublicKey(FriendlyByteBuf buf) {
        try {
            return Crypt.byteToPublicKey(buf.readByteArray(512));
        } catch (CryptException e) {
            throw new DecoderException("Malformed public key bytes", e);
        }
    }

    public static void writeBlockHitResult(FriendlyByteBuf buf, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        buf.writeBlockPos(pos);
        buf.writeEnum(hit.getDirection());
        Vec3 location = hit.getLocation();
        buf.writeFloat((float) (location.x - pos.getX()));
        buf.writeFloat((float) (location.y - pos.getY()));
        buf.writeFloat((float) (location.z - pos.getZ()));
        buf.writeBoolean(hit.isInside());
        buf.writeBoolean(hit.isWorldBorderHit());
    }

    public static BlockHitResult readBlockHitResult(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Direction face = buf.readEnum(Direction.class);
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        return new BlockHitResult(new Vec3(pos.getX() + (double) x, pos.getY() + (double) y, pos.getZ() + (double) z),
                face, pos, buf.readBoolean(), buf.readBoolean());
    }
}
