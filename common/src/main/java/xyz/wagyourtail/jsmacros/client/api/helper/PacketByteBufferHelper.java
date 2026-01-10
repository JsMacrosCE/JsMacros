package xyz.wagyourtail.jsmacros.client.api.helper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.doclet.DocletReplaceParams;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.api.math.Pos3D;
import xyz.wagyourtail.jsmacros.client.api.classes.RegistryHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.BlockPosHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.ChunkHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.DirectionHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.HitResultHelper;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class PacketByteBufferHelper extends BaseHelper<FriendlyByteBuf> {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Don't touch this here!
     */
    public static final Map<Class<? extends Packet<?>>, Function<FriendlyByteBuf, ? extends Packet<?>>> BUFFER_TO_PACKET = new HashMap<>();
    private static final Object2IntMap<Class<? extends Packet<?>>> PACKET_IDS = new Object2IntArrayMap<>();
    private static final Object2IntMap<Class<? extends Packet<?>>> PACKET_STATES = new Object2IntArrayMap<>();
    private static final Object2BooleanMap<Class<? extends Packet<?>>> PACKET_SIDES = new Object2BooleanArrayMap<>();
    /**
     * These names are subject to change and only exist for convenience.
     */
    private static final Map<String, Class<? extends Packet<?>>> PACKETS = new HashMap<>();
    private static final Map<Class<? extends Packet<?>>, String> PACKET_NAMES = new HashMap<>();

    @Nullable
    private final Packet<?> packet;
    private final ByteBuf original;

    public PacketByteBufferHelper() {
        super(getBuffer(null));
        this.packet = null;
        this.original = base.copy();
    }

    public PacketByteBufferHelper(FriendlyByteBuf base) {
        super(base);
        this.packet = null;
        this.original = base.copy();
    }

    public PacketByteBufferHelper(Packet<?> packet) {
        super(getBuffer(packet));
        this.packet = packet;
        base.markReaderIndex();
        base.markWriterIndex();

        // get the PacketCodec static field and use it to write
        try {
            Class<?> packetClass = packet.getClass();
            Field f = Arrays.stream(packetClass.getFields()).filter(e -> e.getType().isAssignableFrom(StreamCodec.class)).findFirst().orElseThrow();
            StreamCodec<FriendlyByteBuf, Packet<?>> codec = (StreamCodec<FriendlyByteBuf, Packet<?>>) f.get(null);
            codec.encode(base, packet);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.original = base.copy();
    }

    private static FriendlyByteBuf getBuffer(Packet<?> packet) {
        ByteBuf buffer = Unpooled.buffer();
        return new FriendlyByteBuf(buffer);
    }

    /**
     * @return the packet for this buffer or {@code null} if no packet was used to create this
     * helper.
     * @since 1.8.4
     */
    @Nullable
    public Packet<?> toPacket() {
        return packet == null ? null : toPacket(packet.getClass());
    }

    /**
     * @param packetName the name of the packet's class that should be returned
     * @return the packet for this buffer.
     * @see #getPacketNames()
     * @since 1.8.4
     */
    @DocletReplaceParams("packetName: PacketName")
    public Packet<?> toPacket(String packetName) {
        return toPacket(PACKETS.get(packetName));
    }

    /**
     * @param clazz the class of the packet to return
     * @return the packet for this buffer.
     * @since 1.8.4
     */
    public Packet<?> toPacket(Class<? extends Packet> clazz) {
        return BUFFER_TO_PACKET.get(clazz).apply(base);
    }

    /**
     * @param packetClass the class of the packet to get the id for
     * @return the id of the packet.
     * @since 1.8.4
     */
    public int getPacketId(Class<? extends Packet<?>> packetClass) {
        return PACKET_IDS.getInt(packetClass);
    }

    /**
     * @param packetClass the class of the packet to get the id for
     * @return the id of the network state the packet belongs to.
     * @since 1.8.4
     */
    public int getNetworkStateId(Class<? extends Packet<?>> packetClass) {
        return PACKET_STATES.getInt(packetClass);
    }

    /**
     * @param packetClass the class to get the side for
     * @return {@code true} if the packet is clientbound, {@code false} if it is serverbound.
     * @since 1.8.4
     */
    public boolean isClientbound(Class<? extends Packet<?>> packetClass) {
        return PACKET_SIDES.getBoolean(packetClass);
    }

    /**
     * @param packetClass the class to get the id for
     * @return {@code true} if the packet is serverbound, {@code false} if it is clientbound.
     * @since 1.8.4
     */
    public boolean isServerbound(Class<? extends Packet<?>> packetClass) {
        return !PACKET_SIDES.getBoolean(packetClass);
    }

    /**
     * Send a packet of the given type, created from this buffer, to the server.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper sendPacket() {
        if (packet != null) {
            Minecraft.getInstance().getConnection().send(toPacket());
        }
        return this;
    }

    /**
     * @param packetName the name of the packet's class that should be sent
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper sendPacket(String packetName) {
        return sendPacket(PACKETS.get(packetName));
    }

    /**
     * Send a packet of the given type, created from this buffer, to the server.
     *
     * @param clazz the class of the packet to send
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper sendPacket(Class<? extends Packet<?>> clazz) {
        Minecraft.getInstance().getConnection().send(toPacket(clazz));
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper receivePacket() {
        if (packet != null) {
            ((Packet<ClientGamePacketListener>) packet).handle(Minecraft.getInstance().getConnection());
        }
        return this;
    }

    /**
     * @param packetName the name of the packet's class that should be received
     * @return self for chaining.
     * @see #getPacketNames()
     * @since 1.8.4
     */
    @DocletReplaceParams("packetName: PacketName")
    public PacketByteBufferHelper receivePacket(String packetName) {
        if (packet != null) {
            ((Packet<ClientGamePacketListener>) toPacket(packetName)).handle(Minecraft.getInstance().getConnection());
        }
        return this;
    }

    /**
     * @param clazz the class of the packet to receive
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper receivePacket(Class<? extends Packet> clazz) {
        if (packet != null) {
            ((Packet<ClientGamePacketListener>) toPacket(clazz)).handle(Minecraft.getInstance().getConnection());
        }
        return this;
    }

    /**
     * These names are subject to change and are only for an easier access. They will probably not
     * change in the future, but it is not guaranteed.
     *
     * @return a list of all packet names.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PacketName>")
    public List<String> getPacketNames() {
        return ImmutableList.copyOf(PACKETS.keySet());
    }

    /**
     * Resets the buffer to the state it was in when this helper was created.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper reset() {
        base = new FriendlyByteBuf(original.copy());
        return this;
    }

    /**
     * @param key the registry key to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeRegistryKey(ResourceKey<?> key) {
        base.writeResourceKey(key);
        return this;
    }

    /**
     * @param registry the registry the read key is from
     * @return the registry key.
     * @since 1.8.4
     */
    public <T> ResourceKey<T> readRegistryKey(ResourceKey<? extends Registry<T>> registry) {
        return base.readResourceKey(registry);
    }

    /**
     * @param collection the collection to store
     * @param writer     the function that writes the collection's elements to the buffer
     * @return self for chaining.
     * @since 1.8.4
     */
    public <T> PacketByteBufferHelper writeCollection(Collection<T> collection, MethodWrapper<FriendlyByteBuf, T, ?, ?> writer) {
        base.writeCollection(collection, writer::accept);
        return this;
    }

    /**
     * @param reader the function that reads the collection's elements from the buffer
     * @return the read list.
     * @since 1.8.4
     */
    public <T> List<T> readList(MethodWrapper<FriendlyByteBuf, ?, T, ?> reader) {
        return base.readList(reader::apply);
    }

    /**
     * @param list the integer list to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeIntList(Collection<Integer> list) {
        base.writeIntIdList(new IntArrayList(list));
        return this;
    }

    /**
     * @return the read integer list.
     * @since 1.8.4
     */
    public IntList readIntList() {
        return base.readIntIdList();
    }

    /**
     * @param map         the map to store
     * @param keyWriter   the function to write the map's keys to the buffer
     * @param valueWriter the function to write the map's values to the buffer
     * @return self for chaining.
     * @since 1.8.4
     */
    public <K, V> PacketByteBufferHelper writeMap(Map<K, V> map, MethodWrapper<FriendlyByteBuf, K, ?, ?> keyWriter, MethodWrapper<FriendlyByteBuf, V, ?, ?> valueWriter) {
        base.writeMap(map, keyWriter::accept, valueWriter::accept);
        return this;
    }

    /**
     * @param keyReader   the function to read the map's keys from the buffer
     * @param valueReader the function to read the map's values from the buffer
     * @return the read map.
     * @since 1.8.4
     */
    public <K, V> Map<K, V> readMap(MethodWrapper<FriendlyByteBuf, ?, K, ?> keyReader, MethodWrapper<FriendlyByteBuf, ?, V, ?> valueReader) {
        return base.readMap(Maps::newHashMapWithExpectedSize, keyReader::apply, valueReader::apply);
    }

    /**
     * @param reader the function to read the collection's elements from the buffer
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper forEachInCollection(MethodWrapper<FriendlyByteBuf, ?, Object, ?> reader) {
        base.readWithCount(reader);
        return this;
    }

    /**
     * @param value  the optional value to store
     * @param writer the function to write the optional value if present to the buffer
     * @return self for chaining.
     * @see #writeNullable(Object, MethodWrapper)
     * @since 1.8.4
     */
    public <T> PacketByteBufferHelper writeOptional(T value, MethodWrapper<FriendlyByteBuf, T, ?, ?> writer) {
        base.writeOptional(Optional.ofNullable(value), writer::accept);
        return this;
    }

    /**
     * @param reader the function to read the optional value from the buffer if present
     * @return the optional value.
     * @see #readNullable(MethodWrapper)
     * @since 1.8.4
     */
    public <T> Optional<T> readOptional(MethodWrapper<FriendlyByteBuf, ?, T, ?> reader) {
        return base.readOptional(reader::apply);
    }

    /**
     * @param value  the optional value to store
     * @param writer the function to write the optional value if it's not null to the buffer
     * @return self for chaining.
     * @see #writeOptional(Object, MethodWrapper)
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeNullable(Object value, MethodWrapper<FriendlyByteBuf, Object, ?, ?> writer) {
        base.writeNullable(value, writer::accept);
        return this;
    }

    /**
     * @param reader the function to read the value from the buffer if it's not null
     * @return the read value or {@code null} if it was null.
     * @see #readOptional(MethodWrapper)
     * @since 1.8.4
     */
    public <T> T readNullable(MethodWrapper<FriendlyByteBuf, ?, T, ?> reader) {
        return base.readNullable(reader::apply);
    }

    /**
     * @param bytes the bytes to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeByteArray(byte[] bytes) {
        base.writeByteArray(bytes);
        return this;
    }

    /**
     * @return the read byte array.
     * @since 1.8.4
     */
    public byte[] readByteArray() {
        return base.readByteArray();
    }

    /**
     * Will throw an exception if the byte array is bigger than the given maximum size.
     *
     * @param maxSize the maximum size of the byte array to read
     * @return the read byte array.
     * @since 1.8.4
     */
    public byte[] readByteArray(int maxSize) {
        return base.readByteArray(maxSize);
    }

    /**
     * @param ints the int array to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeIntArray(int[] ints) {
        base.writeVarIntArray(ints);
        return this;
    }

    /**
     * @return the read int array.
     * @since 1.8.4
     */
    public int[] readIntArray() {
        return base.readVarIntArray();
    }

    /**
     * Will throw an exception if the int array is bigger than the given maximum size.
     *
     * @param maxSize the maximum size of the int array to read
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper readIntArray(int maxSize) {
        base.readVarIntArray(maxSize);
        return this;
    }

    /**
     * @param longs the long array to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeLongArray(long[] longs) {
        base.writeLongArray(longs);
        return this;
    }

    /**
     * @return the read long array.
     * @since 1.8.4
     */
    public long[] readLongArray() {
        return base.readLongArray();
    }

    /**
     * Will throw an exception if the long array is bigger than the given maximum size.
     *
     * @param maxSize the maximum size of the long array to read
     * @return the read long array.
     * @since 1.8.4
     */
    public long[] readLongArray(int maxSize) {
        return base.readLongArray(); // TODO: does this exist?
    }

    /**
     * @param pos the block position to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBlockPos(BlockPosHelper pos) {
        base.writeBlockPos(pos.getRaw());
        return this;
    }

    /**
     * @param x the x coordinate of the block position to store
     * @param y the y coordinate of the block position to store
     * @param z the z coordinate of the block position to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBlockPos(int x, int y, int z) {
        base.writeBlockPos(new BlockPos(x, y, z));
        return this;
    }

    /**
     * @return the read block position.
     * @since 1.8.4
     */
    public BlockPosHelper readBlockPos() {
        return new BlockPosHelper(base.readBlockPos());
    }

    /**
     * @param x the x coordinate of the chunk to store
     * @param z the z coordinate of the chunk to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkPos(int x, int z) {
        base.writeChunkPos(new ChunkPos(x, z));
        return this;
    }

    /**
     * @param chunk the chunk to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkPos(ChunkHelper chunk) {
        base.writeChunkPos(chunk.getRaw().getPos());
        return this;
    }

    /**
     * @return the position of the read chunk, x at index 0, z at index 1.
     * @since 1.8.4
     */
    public int[] readChunkPos() {
        ChunkPos pos = base.readChunkPos();
        return new int[]{pos.x, pos.z};
    }

    /**
     * @return a {@link ChunkHelper} for the read chunk position.
     * @since 1.8.4
     */
    @Nullable
    public ChunkHelper readChunkHelper() {
        ChunkPos pos = base.readChunkPos();
        assert Minecraft.getInstance().level != null;
        ChunkAccess chunk = Minecraft.getInstance().level.getChunk(pos.x, pos.z);
        return chunk == null ? null : new ChunkHelper(chunk);
    }

    // TODO: These methods were removed in 1.21.9 or 1.21.10
    //? if <=1.21.8 {
    /*
    /**
     * @param chunkX the x coordinate of the chunk to store
     * @param y      the y coordinate to store
     * @param chunkZ the z coordinate of the chunk to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkSectionPos(int chunkX, int y, int chunkZ) {
        base.writeSectionPos(SectionPos.of(chunkX, y, chunkZ));
        return this;
    }

    /**
     * @param chunk the chunk whose position should be stored
     * @param y     the y to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkSectionPos(ChunkHelper chunk, int y) {
        base.writeSectionPos(SectionPos.of(chunk.getRaw().getPos(), y));
        return this;
    }

    /**
     * @return the read chunk section pos, as a {@link BlockPosHelper}.
     * @since 1.8.4
     */
    public BlockPosHelper readChunkSectionPos() {
        SectionPos pos = base.readSectionPos();
        return new BlockPosHelper(pos.x(), pos.y(), pos.z());
    }
    //?}

    /**
     * @param dimension the dimension, vanilla default are {@code overworld}, {@code the_nether},
     *                  {@code the_end}
     * @param pos       the position to store
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("dimension: CanOmitNamespace<Dimension>, pos: BlockPosHelper")
    public PacketByteBufferHelper writeGlobalPos(String dimension, BlockPosHelper pos) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
        base.writeGlobalPos(GlobalPos.of(key, pos.getRaw()));
        return this;
    }

    /**
     * @param dimension the dimension, vanilla default are {@code overworld}, {@code the_nether},
     *                  {@code the_end}
     * @param x         the x coordinate of the position to store
     * @param y         the y coordinate of the position to store
     * @param z         the z coordinate of the position to store
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("dimension: CanOmitNamespace<Dimension>, x: int, y: int, z: int")
    public PacketByteBufferHelper writeGlobalPos(String dimension, int x, int y, int z) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
        base.writeGlobalPos(GlobalPos.of(key, new BlockPos(x, y, z)));
        return this;
    }

    /**
     * @param constant the enum constant to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeEnumConstant(Enum<?> constant) {
        base.writeEnum(constant);
        return this;
    }

    /**
     * @param enumClass the class of the enum to read from
     * @return the read enum constant.
     * @since 1.8.4
     */
    public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
        return base.readEnum(enumClass);
    }

    /**
     * @param i the int to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeVarInt(int i) {
        base.writeVarInt(i);
        return this;
    }

    /**
     * @return the read int.
     * @since 1.8.4
     */
    public int readVarInt() {
        return base.readVarInt();
    }

    /**
     * @param l the long to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeVarLong(long l) {
        base.writeVarLong(l);
        return this;
    }

    /**
     * @return the read long.
     * @since 1.8.4
     */
    public long readVarLong() {
        return base.readVarLong();
    }

    /**
     * @param uuid the UUID to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeUuid(String uuid) {
        base.writeUUID(UUID.fromString(uuid));
        return this;
    }

    /**
     * @return the read UUID.
     * @since 1.8.4
     */
    public UUID readUuid() {
        return base.readUUID();
    }

    /**
     * @param nbt the nbt
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeNbt(NBTElementHelper.NBTCompoundHelper nbt) {
        base.writeNbt(nbt.getRaw());
        return this;
    }

    /**
     * @return the read nbt data.
     * @since 1.8.4
     */
    public NBTElementHelper<?> readNbt() {
        return NBTElementHelper.resolve(base.readNbt());
    }

    /**
     * @param string the string to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeString(String string) {
        base.writeUtf(string);
        return this;
    }

    /**
     * Throws an exception if the string is longer than the given length.
     *
     * @param string    the string to store
     * @param maxLength the maximum length of the string
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeString(String string, int maxLength) {
        base.writeUtf(string, maxLength);
        return this;
    }

    /**
     * @return the read string.
     * @since 1.8.4
     */
    public String readString() {
        return base.readUtf();
    }

    /**
     * Throws an exception if the read string is longer than the given length.
     *
     * @param maxLength the maximum length of the string to read
     * @return the read string.
     * @since 1.8.4
     */
    public String readString(int maxLength) {
        return base.readUtf(maxLength);
    }

    /**
     * @param id the identifier to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeIdentifier(String id) {
        base.writeResourceLocation(RegistryHelper.parseIdentifier(id));
        return this;
    }

    /**
     * @return the read identifier.
     * @since 1.8.4
     */
    public String readIdentifier() {
        return base.readResourceLocation().toString();
    }

    /**
     * @param date the date to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeDate(Date date) {
        base.writeDate(date);
        return this;
    }

    /**
     * @return the read date.
     * @since 1.8.4
     */
    public Date readDate() {
        return base.readDate();
    }

    /**
     * @param instant the instant to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeInstant(Instant instant) {
        base.writeInstant(instant);
        return this;
    }

    /**
     * @return the read instant.
     * @since 1.8.4
     */
    public Instant readInstant() {
        return base.readInstant();
    }

    /**
     * @param key the public key to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writePublicKey(PublicKey key) {
        base.writePublicKey(key);
        return this;
    }

    /**
     * @return the read public key.
     * @since 1.8.4
     */
    public PublicKey readPublicKey() {
        return base.readPublicKey();
    }

    /**
     * @param hitResult the hit result to store
     * @return self for chaining.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#writeBlockHitResult(HitResultHelper.Block hitResult)} instead.
     */
    @Deprecated
    public PacketByteBufferHelper writeBlockHitResult(BlockHitResult hitResult) {
        base.writeBlockHitResult(hitResult);
        return this;
    }

    /**
     * @param hitResult the hit result to store
     * @return self for chaining.
     * @since 1.9.1
     */
    public PacketByteBufferHelper writeBlockHitResult(HitResultHelper.Block hitResult) {
        base.writeBlockHitResult(hitResult.getRaw());
        return this;
    }

    /**
     * @param pos         the position of the BlockHitResult
     * @param direction   the direction of the BlockHitResult
     * @param blockPos    the block pos of the BlockHitResult
     * @param missed      whether the BlockHitResult missed
     * @param insideBlock whether the BlockHitResult is inside a block
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBlockHitResult(Pos3D pos, String direction, BlockPosHelper blockPos, boolean missed, boolean insideBlock) {
        BlockHitResult result;
        Vec3 vecPos = new Vec3(pos.x, pos.y, pos.z);
        if (missed) {
            result = BlockHitResult.miss(vecPos, Direction.valueOf(direction), blockPos.getRaw());
        } else {
            result = new BlockHitResult(vecPos, Direction.valueOf(direction), blockPos.getRaw(), insideBlock);
        }
        base.writeBlockHitResult(result);
        return this;
    }

    /**
     * @return the read block hit result.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#readBlockHitResultHelper()} instead.
     */
    @Deprecated
    public BlockHitResult readBlockHitResult() {
        return base.readBlockHitResult();
    }

    /**
     * @return a map of the block hit result's data and their values.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#readBlockHitResultHelper()} instead.
     */
    @Deprecated
    public Map<String, Object> readBlockHitResultMap() {
        BlockHitResult hitResult = base.readBlockHitResult();
        return ImmutableMap.of("side", new DirectionHelper(hitResult.getDirection()), "blockPos", new BlockPosHelper(hitResult.getBlockPos()), "missed", hitResult.getType() == HitResult.Type.MISS, "inside", hitResult.isInside());
    }

    /**
     * @return the read block hit result as a helper.
     * @since 1.9.1
     */
    public HitResultHelper.Block readBlockHitResultHelper() {
        return new HitResultHelper.Block(base.readBlockHitResult());
    }

    /**
     * @param bitSet the bit set to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBitSet(BitSet bitSet) {
        base.writeBitSet(bitSet);
        return this;
    }

    /**
     * @return the read bit set.
     * @since 1.8.4
     */
    public BitSet readBitSet() {
        return base.readBitSet();
    }

    /**
     * @return the readers current position.
     * @since 1.8.4
     */
    public int readerIndex() {
        return base.readerIndex();
    }

    /**
     * @param index the readers new index
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setReaderIndex(int index) {
        base.readerIndex(index);
        return this;
    }

    /**
     * @return the writers current position.
     * @since 1.8.4
     */
    public int writerIndex() {
        return base.writerIndex();
    }

    /**
     * @param index the writers new index
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setWriterIndex(int index) {
        base.writerIndex(index);
        return this;
    }

    /**
     * @param readerIndex the readers new index
     * @param writerIndex the writers new index
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setIndices(int readerIndex, int writerIndex) {
        base.setIndex(readerIndex, writerIndex);
        return this;
    }

    /**
     * Resets the readers and writers index to their respective last marked indices.
     *
     * @return self for chaining.
     * @see #markReaderIndex()
     * @see #markWriterIndex()
     * @since 1.8.4
     */
    public PacketByteBufferHelper resetIndices() {
        base.resetReaderIndex();
        base.resetWriterIndex();
        return this;
    }

    /**
     * Marks the readers current index for later use.
     *
     * @return self for chaining.
     * @see #resetReaderIndex()
     * @since 1.8.4
     */
    public PacketByteBufferHelper markReaderIndex() {
        base.markReaderIndex();
        return this;
    }

    /**
     * Resets the readers index to the last marked index.
     *
     * @return self for chaining.
     * @see #markReaderIndex()
     * @since 1.8.4
     */
    public PacketByteBufferHelper resetReaderIndex() {
        base.resetReaderIndex();
        return this;
    }

    /**
     * Marks the writers current index for later use.
     *
     * @return self for chaining.
     * @see #resetWriterIndex() ()
     * @since 1.8.4
     */
    public PacketByteBufferHelper markWriterIndex() {
        base.markWriterIndex();
        return this;
    }

    /**
     * Resets the writers index to the last marked index.
     *
     * @return self for chaining.
     * @see #markWriterIndex()
     * @since 1.8.4
     */
    public PacketByteBufferHelper resetWriterIndex() {
        base.resetWriterIndex();
        return this;
    }

    /**
     * Resets the writers and readers index to 0. This technically doesn't clear the buffer, but
     * rather makes it so that new operations will overwrite the old data.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper clear() {
        base.clear();
        return this;
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBoolean(boolean value) {
        base.writeBoolean(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setBoolean(int index, boolean value) {
        base.setBoolean(index, value);
        return this;
    }

    /**
     * @return the read boolean value.
     * @since 1.8.4
     */
    public boolean readBoolean() {
        return base.readBoolean();
    }

    /**
     * @param index the index to read from
     * @return the boolean value at the given index.
     * @since 1.8.4
     */
    public boolean getBoolean(int index) {
        return base.getBoolean(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChar(int value) {
        base.writeChar(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setChar(int index, char value) {
        base.setChar(index, value);
        return this;
    }

    /**
     * @return the read char value.
     * @since 1.8.4
     */
    public char readChar() {
        return base.readChar();
    }

    /**
     * @param index the index to read from
     * @return the char at the given index.
     * @since 1.8.4
     */
    public char getChar(int index) {
        return base.getChar(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeByte(int value) {
        base.writeByte(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setByte(int index, int value) {
        base.setByte(index, value);
        return this;
    }

    /**
     * @return the read byte value.
     * @since 1.8.4
     */
    public byte readByte() {
        return base.readByte();
    }

    /**
     * @return the read unsigned byte value, represented as a short.
     * @since 1.8.4
     */
    public short readUnsignedByte() {
        return base.readUnsignedByte();
    }

    /**
     * @param index the index to read from
     * @return the byte at the given index.
     * @since 1.8.4
     */
    public byte getByte(int index) {
        return base.getByte(index);
    }

    /**
     * @param index the index to read from
     * @return the unsigned byte at the given index, represented as a short.
     * @since 1.8.4
     */
    public short getUnsignedByte(int index) {
        return base.getUnsignedByte(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeShort(int value) {
        base.writeShort(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setShort(int index, int value) {
        base.setShort(index, value);
        return this;
    }

    /**
     * @return the read short value.
     * @since 1.8.4
     */
    public short readShort() {
        return base.readShort();
    }

    /**
     * @return the read unsigned short value, represented as an int.
     * @since 1.8.4
     */
    public int readUnsignedShort() {
        return base.readUnsignedShort();
    }

    /**
     * @param index the index to read from
     * @return the short at the given index.
     * @since 1.8.4
     */
    public short getShort(int index) {
        return base.getShort(index);
    }

    /**
     * @param index the index to read from
     * @return the unsigned short at the given index, represented as an int.
     * @since 1.8.4
     */
    public int getUnsignedShort(int index) {
        return base.getUnsignedShort(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeMedium(int value) {
        base.writeMedium(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setMedium(int index, int value) {
        base.setMedium(index, value);
        return this;
    }

    /**
     * @return the read medium value.
     * @since 1.8.4
     */
    public int readMedium() {
        return base.readMedium();
    }

    /**
     * @return the read unsigned medium value.
     * @since 1.8.4
     */
    public int readUnsignedMedium() {
        return base.readUnsignedMedium();
    }

    /**
     * @param index the index to read from
     * @return the medium at the given index.
     * @since 1.8.4
     */
    public int getMedium(int index) {
        return base.getMedium(index);
    }

    /**
     * @param index the index to read from
     * @return the unsigned medium at the given index.
     * @since 1.8.4
     */
    public int getUnsignedMedium(int index) {
        return base.getUnsignedMedium(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeInt(int value) {
        base.writeInt(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setInt(int index, int value) {
        base.setInt(index, value);
        return this;
    }

    /**
     * @return the read int value.
     * @since 1.8.4
     */
    public int readInt() {
        return base.readInt();
    }

    /**
     * @return the read unsigned int value, represented as a long.
     * @since 1.8.4
     */
    public long readUnsignedInt() {
        return base.readUnsignedInt();
    }

    /**
     * @param index the index to read from
     * @return the int at the given index.
     * @since 1.8.4
     */
    public int getInt(int index) {
        return base.getInt(index);
    }

    /**
     * @param index the index to read from
     * @return the unsigned int at the given index, represented as a long.
     * @since 1.8.4
     */
    public long getUnsignedInt(int index) {
        return base.getUnsignedInt(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeLong(long value) {
        base.writeLong(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setLong(int index, long value) {
        base.setLong(index, value);
        return this;
    }

    /**
     * @return the read long value.
     * @since 1.8.4
     */
    public long readLong() {
        return base.readLong();
    }

    /**
     * @param index the index to read from
     * @return the long at the given index.
     * @since 1.8.4
     */
    public long getLong(int index) {
        return base.getLong(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeFloat(double value) {
        base.writeFloat((float) value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setFloat(int index, double value) {
        base.setFloat(index, (float) value);
        return this;
    }

    /**
     * @return the read float value.
     * @since 1.8.4
     */
    public float readFloat() {
        return base.readFloat();
    }

    /**
     * @param index the index to read from
     * @return the float at the given index.
     * @since 1.8.4
     */
    public float getFloat(int index) {
        return base.getFloat(index);
    }

    /**
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeDouble(double value) {
        base.writeDouble(value);
        return this;
    }

    /**
     * @param index the index to write to
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setDouble(int index, double value) {
        base.setDouble(index, value);
        return this;
    }

    /**
     * @return the read double value.
     * @since 1.8.4
     */
    public double readDouble() {
        return base.readDouble();
    }

    /**
     * @param index the index to read from
     * @return the double at the given index.
     * @since 1.8.4
     */
    public double getDouble(int index) {
        return base.getDouble(index);
    }

    /**
     * @param length the amount of zeros to write
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeZero(int length) {
        base.writeZero(length);
        return this;
    }

    /**
     * @param index  the index to write to
     * @param length the amount of zeros to write
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setZero(int index, int length) {
        base.setZero(index, length);
        return this;
    }

    /**
     * @param bytes the bytes to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeBytes(byte[] bytes) {
        base.writeBytes(bytes);
        return this;
    }

    /**
     * @param index the index to write to
     * @param bytes the bytes to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setBytes(int index, byte[] bytes) {
        base.setBytes(index, bytes);
        return this;
    }

    /**
     * Starts reading from this buffer's readerIndex.
     *
     * @param length the length of the array to read
     * @return the read byte array.
     * @since 1.8.4
     */
    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        base.readBytes(bytes);
        return bytes;
    }

    /**
     * @param index  the index to start reading from
     * @param length the length of the array to read
     * @return the read byte array .
     * @since 1.8.4
     */
    public byte[] getBytes(int index, int length) {
        byte[] bytes = new byte[length];
        base.getBytes(index, bytes);
        return bytes;
    }

    /**
     * Moves the readerIndex of this buffer by the specified amount.
     *
     * @param length the amount of bytes to skip
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper skipBytes(int length) {
        base.skipBytes(length);
        return this;
    }

    @Override
    public String toString() {
        return String.format("PacketByteBufferHelper:{\"base\": %s}", base);
    }

    public static String getPacketName(Packet<?> packet) {
        return PACKET_NAMES.getOrDefault(packet.getClass(), packet.getClass().getSimpleName());
    }

    public static void init() {
//        for (NetworkState state : NetworkState.values()) {
//            for (NetworkSide side : NetworkSide.values()) {
//                state.getPacketIdToPacketMap(side).forEach((id, packet) -> {
//                    PACKET_IDS.put(packet, id);
//                    PACKET_STATES.put(packet, state.ordinal());
//                    PACKET_SIDES.put(packet, side == NetworkSide.CLIENTBOUND);
//                });
//            }
//        }

        // TODO: Update this with latest
        PACKETS.put("WorldBorderWarningTimeChangedS2CPacket", net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket.class);
        PACKETS.put("SelectMerchantTradeC2SPacket", net.minecraft.network.protocol.game.ServerboundSelectTradePacket.class);
        PACKETS.put("SelectAdvancementTabS2CPacket", net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket.class);
        PACKETS.put("ChunkBiomeDataS2CPacket", net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.class);
        PACKETS.put("ChunkDeltaUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket.class);
        PACKETS.put("EntityStatusEffectS2CPacket", net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket.class);
        PACKETS.put("AcknowledgeReconfigurationC2SPacket", net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket.class);
        PACKETS.put("GameJoinS2CPacket", net.minecraft.network.protocol.game.ClientboundLoginPacket.class);
        PACKETS.put("RemoveEntityStatusEffectS2CPacket", net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket.class);
        PACKETS.put("RemoveMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundDeleteChatPacket.class);
        PACKETS.put("EntityStatusS2CPacket", net.minecraft.network.protocol.game.ClientboundEntityEventPacket.class);
        PACKETS.put("OpenWrittenBookS2CPacket", net.minecraft.network.protocol.game.ClientboundOpenBookPacket.class);
        PACKETS.put("ClickSlotC2SPacket", net.minecraft.network.protocol.game.ServerboundContainerClickPacket.class);
        PACKETS.put("PingResultS2CPacket", net.minecraft.network.protocol.ping.ClientboundPongResponsePacket.class);
        PACKETS.put("TeamS2CPacket", net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.class);
        PACKETS.put("UpdateSelectedSlotS2CPacket", net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket.class);
        PACKETS.put("SubtitleS2CPacket", net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket.class);
        PACKETS.put("EntityAnimationS2CPacket", net.minecraft.network.protocol.game.ClientboundAnimatePacket.class);
        PACKETS.put("StartChunkSendS2CPacket", net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket.class);
        PACKETS.put("DamageTiltS2CPacket", net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket.class);
        PACKETS.put("UpdateCommandBlockMinecartC2SPacket", net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket.class);
        PACKETS.put("UnloadChunkS2CPacket", net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket.class);
        PACKETS.put("BlockEventS2CPacket", net.minecraft.network.protocol.game.ClientboundBlockEventPacket.class);
        PACKETS.put("ParticleS2CPacket", net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class);
        PACKETS.put("UpdateSelectedSlotC2SPacket", net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket.class);
        PACKETS.put("UpdateDifficultyLockC2SPacket", net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket.class);
        PACKETS.put("CloseScreenS2CPacket", net.minecraft.network.protocol.game.ClientboundContainerClosePacket.class);
        PACKETS.put("ClearTitleS2CPacket", net.minecraft.network.protocol.game.ClientboundClearTitlesPacket.class);
        PACKETS.put("AdvancementUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket.class);
        PACKETS.put("SetTradeOffersS2CPacket", net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket.class);
        PACKETS.put("RecipeBookDataC2SPacket", net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket.class);
        PACKETS.put("RequestCommandCompletionsC2SPacket", net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket.class);
        PACKETS.put("ResourcePackStatusC2SPacket", net.minecraft.network.protocol.common.ServerboundResourcePackPacket.class);
        PACKETS.put("PlaySoundFromEntityS2CPacket", net.minecraft.network.protocol.game.ClientboundSoundEntityPacket.class);
        PACKETS.put("BoatPaddleStateC2SPacket", net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket.class);
        PACKETS.put("KeepAliveS2CPacket", net.minecraft.network.protocol.common.ClientboundKeepAlivePacket.class);
        PACKETS.put("PlayerInteractBlockC2SPacket", net.minecraft.network.protocol.game.ServerboundUseItemOnPacket.class);
        PACKETS.put("WorldBorderInterpolateSizeS2CPacket", net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket.class);
        PACKETS.put("DynamicRegistriesS2CPacket", net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket.class);
        PACKETS.put("VehicleMoveS2CPacket", net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket.class);
        PACKETS.put("PlayerAbilitiesS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket.class);
        PACKETS.put("WorldEventS2CPacket", net.minecraft.network.protocol.game.ClientboundLevelEventPacket.class);
        PACKETS.put("CommonPingS2CPacket", net.minecraft.network.protocol.common.ClientboundPingPacket.class);
        PACKETS.put("ChatSuggestionsS2CPacket", net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.class);
        PACKETS.put("PlayerInteractItemC2SPacket", net.minecraft.network.protocol.game.ServerboundUseItemPacket.class);
        PACKETS.put("ChatMessageC2SPacket", net.minecraft.network.protocol.game.ServerboundChatPacket.class);
        PACKETS.put("LookAtS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket.class);
        PACKETS.put("LightUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundLightUpdatePacket.class);
        PACKETS.put("ScoreboardObjectiveUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetObjectivePacket.class);
        PACKETS.put("RecipeCategoryOptionsC2SPacket", net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket.class);
        PACKETS.put("PlayerRespawnS2CPacket", net.minecraft.network.protocol.game.ClientboundRespawnPacket.class);
        PACKETS.put("PlayerInteractEntityC2SPacket", net.minecraft.network.protocol.game.ServerboundInteractPacket.class);
        PACKETS.put("GameStateChangeS2CPacket", net.minecraft.network.protocol.game.ClientboundGameEventPacket.class);
        PACKETS.put("LoginHelloS2CPacket", net.minecraft.network.protocol.login.ClientboundHelloPacket.class);
        PACKETS.put("ClientOptionsC2SPacket", net.minecraft.network.protocol.common.ServerboundClientInformationPacket.class);
        PACKETS.put("EnterReconfigurationS2CPacket", net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket.class);
        PACKETS.put("PlaySoundS2CPacket", net.minecraft.network.protocol.game.ClientboundSoundPacket.class);
        PACKETS.put("OpenScreenS2CPacket", net.minecraft.network.protocol.game.ClientboundOpenScreenPacket.class);
        PACKETS.put("QueryRequestC2SPacket", net.minecraft.network.protocol.status.ServerboundStatusRequestPacket.class);
        PACKETS.put("ChatMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerChatPacket.class);
        PACKETS.put("PlayerPositionLookS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class);
        PACKETS.put("UpdateStructureBlockC2SPacket", net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket.class);
        PACKETS.put("RenameItemC2SPacket", net.minecraft.network.protocol.game.ServerboundRenameItemPacket.class);
        PACKETS.put("ChunkSentS2CPacket", net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket.class);
        PACKETS.put("EntityVelocityUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class);
        PACKETS.put("EntityPositionS2CPacket", net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket.class);
        PACKETS.put("EntityS2CPacket", net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.class);
        PACKETS.put("EntityS2CPacket$Rotate", net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot.class);
        PACKETS.put("EntityS2CPacket$MoveRelative", net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos.class);
        PACKETS.put("EntityS2CPacket$RotateAndMoveRelative", net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot.class);
        PACKETS.put("EntitiesDestroyS2CPacket", net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket.class);
        PACKETS.put("CommandSuggestionsS2CPacket", net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket.class);
        PACKETS.put("AdvancementTabC2SPacket", net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket.class);
        PACKETS.put("EntityEquipmentUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket.class);
        PACKETS.put("DisconnectS2CPacket", net.minecraft.network.protocol.common.ClientboundDisconnectPacket.class);
        PACKETS.put("SignEditorOpenS2CPacket", net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket.class);
        PACKETS.put("PlayerSpawnPositionS2CPacket", net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket.class);
        PACKETS.put("NbtQueryResponseS2CPacket", net.minecraft.network.protocol.game.ClientboundTagQueryPacket.class);
        PACKETS.put("EndCombatS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket.class);
        PACKETS.put("CustomPayloadS2CPacket", ClientboundCustomPayloadPacket.class);
        PACKETS.put("MapUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundMapItemDataPacket.class);
        PACKETS.put("CustomPayloadC2SPacket", ServerboundCustomPayloadPacket.class);
        PACKETS.put("ButtonClickC2SPacket", net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket.class);
        PACKETS.put("LoginSuccessS2CPacket", net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket.class);
        PACKETS.put("SynchronizeTagsS2CPacket", net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket.class);
        PACKETS.put("MessageAcknowledgmentC2SPacket", net.minecraft.network.protocol.game.ServerboundChatAckPacket.class);
        PACKETS.put("ChunkDataS2CPacket", net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket.class);
        PACKETS.put("EntityPassengersSetS2CPacket", net.minecraft.network.protocol.game.ClientboundSetPassengersPacket.class);
        PACKETS.put("TitleS2CPacket", net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket.class);
        PACKETS.put("BlockUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket.class);
        PACKETS.put("BlockBreakingProgressS2CPacket", net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket.class);
        PACKETS.put("ScreenHandlerPropertyUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket.class);
        PACKETS.put("PlayerActionC2SPacket", net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.class);
        PACKETS.put("LoginQueryRequestS2CPacket", net.minecraft.network.protocol.login.ClientboundCustomQueryPacket.class);
        PACKETS.put("ClientStatusC2SPacket", net.minecraft.network.protocol.game.ServerboundClientCommandPacket.class);
        PACKETS.put("DifficultyS2CPacket", net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket.class);
        PACKETS.put("TeleportConfirmC2SPacket", net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket.class);
        PACKETS.put("InventoryS2CPacket", net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket.class);
        PACKETS.put("FeaturesS2CPacket", net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket.class);
        PACKETS.put("BossBarS2CPacket", net.minecraft.network.protocol.game.ClientboundBossEventPacket.class);
        PACKETS.put("WorldBorderInitializeS2CPacket", net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket.class);
        PACKETS.put("EntityAttachS2CPacket", net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket.class);
        PACKETS.put("ExperienceBarUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetExperiencePacket.class);
        PACKETS.put("QueryBlockNbtC2SPacket", net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket.class);
        PACKETS.put("SpectatorTeleportC2SPacket", net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket.class);
        PACKETS.put("PlayerActionResponseS2CPacket", net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket.class);
        PACKETS.put("ProfilelessChatMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket.class);
        PACKETS.put("PlayerListS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.class);
        PACKETS.put("EnterCombatS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket.class);
        PACKETS.put("OpenHorseScreenS2CPacket", net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket.class);
        PACKETS.put("CommandExecutionC2SPacket", net.minecraft.network.protocol.game.ServerboundChatCommandPacket.class);
        PACKETS.put("CraftRequestC2SPacket", net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket.class);
        PACKETS.put("HandSwingC2SPacket", net.minecraft.network.protocol.game.ServerboundSwingPacket.class);
        PACKETS.put("HandshakeC2SPacket", net.minecraft.network.protocol.handshake.ClientIntentionPacket.class);
        PACKETS.put("ChunkRenderDistanceCenterS2CPacket", net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket.class);
        PACKETS.put("CommonPongC2SPacket", net.minecraft.network.protocol.common.ServerboundPongPacket.class);
        PACKETS.put("PlayerRemoveS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket.class);
        PACKETS.put("SetCameraEntityS2CPacket", net.minecraft.network.protocol.game.ClientboundSetCameraPacket.class);
        PACKETS.put("VehicleMoveC2SPacket", net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.class);
        PACKETS.put("UpdateSignC2SPacket", net.minecraft.network.protocol.game.ServerboundSignUpdatePacket.class);
        PACKETS.put("ServerMetadataS2CPacket", net.minecraft.network.protocol.game.ClientboundServerDataPacket.class);
        PACKETS.put("ResourcePackSendS2CPacket", net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket.class);
        PACKETS.put("ReadyC2SPacket", net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.class);
        PACKETS.put("BlockEntityUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class);
        PACKETS.put("ScreenHandlerSlotUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.class);
        PACKETS.put("ClientCommandC2SPacket", net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.class);
        PACKETS.put("EntityTrackerUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.class);
        PACKETS.put("EnterConfigurationC2SPacket", net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket.class);
        PACKETS.put("QueryResponseS2CPacket", net.minecraft.network.protocol.status.ClientboundStatusResponsePacket.class);
        PACKETS.put("UpdateCommandBlockC2SPacket", net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket.class);
        PACKETS.put("QueryEntityNbtC2SPacket", net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket.class);
        PACKETS.put("LoginHelloC2SPacket", net.minecraft.network.protocol.login.ServerboundHelloPacket.class);
        PACKETS.put("BookUpdateC2SPacket", net.minecraft.network.protocol.game.ServerboundEditBookPacket.class);
        PACKETS.put("ExplosionS2CPacket", net.minecraft.network.protocol.game.ClientboundExplodePacket.class);
        PACKETS.put("PlayerMoveC2SPacket", net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.class);
        PACKETS.put("PlayerMoveC2SPacket$OnGroundOnly", net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly.class);
        PACKETS.put("PlayerMoveC2SPacket$LookAndOnGround", net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot.class);
        PACKETS.put("PlayerMoveC2SPacket$PositionAndOnGround", net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos.class);
        PACKETS.put("PlayerMoveC2SPacket$Full", net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot.class);
        PACKETS.put("JigsawGeneratingC2SPacket", net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket.class);
        PACKETS.put("WorldBorderCenterChangedS2CPacket", net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket.class);
        PACKETS.put("HealthUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetHealthPacket.class);
        PACKETS.put("ItemPickupAnimationS2CPacket", net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket.class);
        PACKETS.put("EntityDamageS2CPacket", net.minecraft.network.protocol.game.ClientboundDamageEventPacket.class);
        PACKETS.put("UpdateJigsawC2SPacket", net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket.class);
        PACKETS.put("WorldTimeUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetTimePacket.class);
        PACKETS.put("CooldownUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundCooldownPacket.class);
        PACKETS.put("KeepAliveC2SPacket", net.minecraft.network.protocol.common.ServerboundKeepAlivePacket.class);
        PACKETS.put("ChunkLoadDistanceS2CPacket", net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket.class);
        PACKETS.put("EntitySetHeadYawS2CPacket", net.minecraft.network.protocol.game.ClientboundRotateHeadPacket.class);
        PACKETS.put("DeathMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket.class);
        PACKETS.put("SimulationDistanceS2CPacket", net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket.class);
        PACKETS.put("WorldBorderSizeChangedS2CPacket", net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket.class);
        PACKETS.put("LoginCompressionS2CPacket", net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket.class);
        PACKETS.put("CraftFailedResponseS2CPacket", net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket.class);
        PACKETS.put("QueryPingC2SPacket", net.minecraft.network.protocol.ping.ServerboundPingRequestPacket.class);
        PACKETS.put("UpdateDifficultyC2SPacket", net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket.class);
        PACKETS.put("OverlayMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket.class);
        PACKETS.put("ScoreboardDisplayS2CPacket", net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket.class);
        PACKETS.put("CloseHandledScreenC2SPacket", net.minecraft.network.protocol.game.ServerboundContainerClosePacket.class);
        PACKETS.put("PlayerListHeaderS2CPacket", net.minecraft.network.protocol.game.ClientboundTabListPacket.class);
        PACKETS.put("WorldBorderWarningBlocksChangedS2CPacket", net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket.class);
        PACKETS.put("CreativeInventoryActionC2SPacket", net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket.class);
        PACKETS.put("EntitySpawnS2CPacket", net.minecraft.network.protocol.game.ClientboundAddEntityPacket.class);
        PACKETS.put("TitleFadeS2CPacket", net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket.class);
        PACKETS.put("ReadyS2CPacket", net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket.class);
        PACKETS.put("SynchronizeRecipesS2CPacket", net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket.class);
        PACKETS.put("LoginDisconnectS2CPacket", net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket.class);
        PACKETS.put("PlayerSessionC2SPacket", net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket.class);
        PACKETS.put("StopSoundS2CPacket", net.minecraft.network.protocol.game.ClientboundStopSoundPacket.class);
        PACKETS.put("UpdatePlayerAbilitiesC2SPacket", net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket.class);
        PACKETS.put("GameMessageS2CPacket", net.minecraft.network.protocol.game.ClientboundSystemChatPacket.class);
        PACKETS.put("LoginKeyC2SPacket", net.minecraft.network.protocol.login.ServerboundKeyPacket.class);
        PACKETS.put("EntityAttributesS2CPacket", net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.class);
        PACKETS.put("PlayerInputC2SPacket", net.minecraft.network.protocol.game.ServerboundPlayerInputPacket.class);
        PACKETS.put("AcknowledgeChunksC2SPacket", net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket.class);
        PACKETS.put("UpdateBeaconC2SPacket", net.minecraft.network.protocol.game.ServerboundSetBeaconPacket.class);
        PACKETS.put("BundleS2CPacket", net.minecraft.network.protocol.game.ClientboundBundlePacket.class);
        PACKETS.put("LoginQueryResponseC2SPacket", net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket.class);
        PACKETS.put("StatisticsS2CPacket", net.minecraft.network.protocol.game.ClientboundAwardStatsPacket.class);
        PACKETS.put("CommandTreeS2CPacket", net.minecraft.network.protocol.game.ClientboundCommandsPacket.class);
        PACKETS.put("ChatCommandSignedC2SPacket", net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket.class);
        PACKETS.put("SlotChangedStateC2SPacket", net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket.class);
        PACKETS.put("ResourcePackRemoveS2CPacket", net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket.class);
        PACKETS.put("ServerTransferS2CPacket", net.minecraft.network.protocol.common.ClientboundTransferPacket.class);
        PACKETS.put("SelectKnownPacksS2CPacket", net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks.class);
        PACKETS.put("StoreCookieS2CPacket", net.minecraft.network.protocol.common.ClientboundStoreCookiePacket.class);
        PACKETS.put("DebugSampleS2CPacket", net.minecraft.network.protocol.game.ClientboundDebugSamplePacket.class);
        PACKETS.put("ProjectilePowerS2CPacket", net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket.class);
        PACKETS.put("TickStepS2CPacket", net.minecraft.network.protocol.game.ClientboundTickingStepPacket.class);
        PACKETS.put("SelectKnownPacksC2SPacket", net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks.class);
        PACKETS.put("CookieRequestS2CPacket", net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket.class);
        PACKETS.put("ResetChatS2CPacket", net.minecraft.network.protocol.configuration.ClientboundResetChatPacket.class);
        PACKETS.put("ScoreboardScoreUpdateS2CPacket", net.minecraft.network.protocol.game.ClientboundSetScorePacket.class);
        //? if <1.21.8 {
        /*PACKETS.put("DebugSampleSubscriptionC2SPacket", net.minecraft.network.protocol.game.ServerboundDebugSampleSubscriptionPacket.class);
        *///?}
        PACKETS.put("ServerLinksS2CPacket", net.minecraft.network.protocol.common.ClientboundServerLinksPacket.class);
        PACKETS.put("CookieResponseC2SPacket", net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket.class);
        PACKETS.put("UpdateTickRateS2CPacket", net.minecraft.network.protocol.game.ClientboundTickingStatePacket.class);
        PACKETS.put("BundleDelimiterS2CPacket", net.minecraft.network.protocol.game.ClientboundBundleDelimiterPacket.class);
        PACKETS.put("CustomReportDetailsS2CPacket", net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket.class);
        PACKETS.put("ScoreboardScoreResetS2CPacket", net.minecraft.network.protocol.game.ClientboundResetScorePacket.class);

        PACKETS.forEach((name, clazz) -> PACKET_NAMES.put(clazz, name));
    }

    public static void main(String[] args) throws IOException {
        StringBuilder builder = new StringBuilder();
        PacketByteBufferHelper.init();
        ClassPath.from(PacketByteBufferHelper.class.getClassLoader())
                .getTopLevelClassesRecursive("net.minecraft.network.packet")
                .stream()
                .map(ClassPath.ClassInfo::load)
                .flatMap(c -> Stream.concat(Stream.of(c), Arrays.stream(c.getDeclaredClasses())))
                .filter(Packet.class::isAssignableFrom)
                .filter(c -> !PACKETS.containsValue(c))
                .filter(c -> !c.equals(Packet.class))
                .forEach(c -> {
                    String name;
                    if (c.getEnclosingClass() != null) {
                        name = c.getEnclosingClass().getSimpleName() + "$" + c.getSimpleName();
                    } else {
                        name = c.getSimpleName();
                    }
                    name = '"' + name + '"';
                    String classQualifier = c.getCanonicalName() + ".class";
                    builder.append("PACKETS.put(").append(name).append(", ").append(classQualifier).append(");").append(System.lineSeparator());
                });
        System.out.println(builder);
    }

}
