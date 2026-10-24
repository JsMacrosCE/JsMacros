package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.jsmacros.client.util.PacketBufferCompat;

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
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.PacketFlow;
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

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.ChunkHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.DirectionHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.HitResultHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.util.ChunkPosCompat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Reads and writes typed values in a packet buffer; callers must use the packet's field order
 * and matching read/write operations.
 * <p>
 * Packet-event buffers contain encoded packet data; {@code Client.createPacketByteBuffer()}
 * creates a fresh buffer. Changes to a packet-event buffer do not change the packet object.
 * {@link #reset()} restores the captured bytes and reader position (normally the start for a
 * packet-backed helper); use it when you need to reread or decode from that captured position.
 * <p>
 * Supported standalone packets can be decoded using their registered codecs. Packet-backed
 * helpers remember their network phase; overloads taking a protocol select a phase explicitly.
 * Conversion reads from the current reader position, so reset after inspecting a packet before
 * decoding its untouched bytes. {@link #receivePacket()} handles the original packet object,
 * whereas its named and class overloads decode the buffer first.
 * <pre class="language-typescript">
 * const payload = Client.createPacketByteBuffer();
 * payload.writeString("hello").writeVarInt(42);
 * Chat.log(`${payload.readString()} and ${payload.readVarInt()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class PacketByteBufferHelper extends BaseHelper<FriendlyByteBuf> {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * the lookup table that turns a buffer back into a packet.
     * <p>
     * Don't touch this here!
     * <br>
     * Populated by {@code init()} for packet classes with supported standalone codecs.
     * {@link #toPacket(Class)} uses it for play-phase decoding; explicit phase decoding selects
     * the matching codec directly.
     */
    public static final Map<Class<? extends Packet<?>>, Function<FriendlyByteBuf, ? extends Packet<?>>> BUFFER_TO_PACKET = new HashMap<>();
    private static final Object2BooleanMap<Class<? extends Packet<?>>> PACKET_SIDES = new Object2BooleanArrayMap<>();
    private static final Map<Class<? extends Packet<?>>, Map<ConnectionProtocol, Integer>> PACKET_IDS_BY_PROTOCOL = new HashMap<>();
    /**
     * These names are subject to change and only exist for convenience.
     */
    private static final Map<String, Class<? extends Packet<?>>> PACKETS = new HashMap<>();
    private static final Map<Class<? extends Packet<?>>, String> PACKET_NAMES = new HashMap<>();

    @Nullable
    private final Packet<?> packet;
    private final String protocol;
    private final ByteBuf original;
    @Nullable
    private final RegistryAccess originalRegistryAccess;

    /**
     * Creates a fresh buffer with no originating packet and the default {@code play} phase.
     * Captures its initial contents for {@code reset()}.
     */
    public PacketByteBufferHelper() {
        super(getBuffer(null));
        this.packet = null;
        this.protocol = "play";
        this.original = base.copy();
        this.originalRegistryAccess = base instanceof RegistryFriendlyByteBuf registryBuffer ? registryBuffer.registryAccess() : null;
    }

    /**
     * Uses the supplied buffer directly as its working buffer and captures a separate copy of its
     * initial contents for {@code reset()}. This helper has no originating packet and uses the default {@code play}
     * phase.
     *
     * @param base the non-null buffer to wrap
     */
    public PacketByteBufferHelper(FriendlyByteBuf base) {
        super(base);
        this.packet = null;
        this.protocol = "play";
        this.original = base.copy();
        this.originalRegistryAccess = base instanceof RegistryFriendlyByteBuf registryBuffer ? registryBuffer.registryAccess() : null;
    }

    /**
     * Encodes a packet using its selected phase's standalone codec and retains the packet as the
     * helper's origin; this does not send or handle the packet. The encoded contents are captured
     * for {@code reset()}. The current connection phase is preferred when registered for the
     * packet; otherwise its sole registered phase is selected. Unknown packets or packets with
     * ambiguous phases throw {@code IllegalArgumentException}; a missing standalone codec also
     * throws {@code IllegalArgumentException}.
     *
     * @param packet the non-null packet to encode
     */
    public PacketByteBufferHelper(Packet<?> packet) {
        this(packet, preferredProtocol(packet, mc.getConnection() == null ? null : mc.getConnection().protocol()));
    }

    /**
     * Encodes a packet using the standalone codec for the specified network phase.
     *
     * @param packet the packet to encode
     * @param protocol the network phase, such as {@code play} or {@code configuration}
     * @throws IllegalArgumentException if the protocol is unknown or no standalone codec is available
     */
    public PacketByteBufferHelper(Packet<?> packet, String protocol) {
        super(getBuffer(packet));
        this.packet = packet;
        this.protocol = protocol;
        base.markReaderIndex();
        base.markWriterIndex();

        // Bundles do not have standalone codecs and cannot be serialized here.
        try {
            Class<?> packetClass = packet.getClass();
            Field codecField = findCodecField(packetClass, protocol);

            if (codecField == null) throw new IllegalArgumentException("Packet has no standalone codec: " + packetClass);
            StreamCodec<FriendlyByteBuf, Packet<?>> codec =
                    (StreamCodec<FriendlyByteBuf, Packet<?>>) codecField.get(null);
            codec.encode(base, packet);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.original = base.copy();
        this.originalRegistryAccess = base instanceof RegistryFriendlyByteBuf registryBuffer ? registryBuffer.registryAccess() : null;
    }

    private static FriendlyByteBuf getBuffer(Packet<?> packet) {
        ByteBuf buffer = Unpooled.buffer();
        return mc.getConnection() == null ? new FriendlyByteBuf(buffer) : new RegistryFriendlyByteBuf(buffer, mc.getConnection().registryAccess());
    }

    /**
     * the packet this buffer came from, rebuilt from the buffer.
     * <p>
     * A fresh helper without an original packet returns {@code null}. A packet-backed helper
     * decodes the original packet class using its saved network phase and the current reader
     * position. This returns a decoded packet, not the original object carried by the event.
     *
     * @return the packet for this buffer, or {@code null} if no packet was used to create this
     * helper
     * @throws IllegalArgumentException if the packet class is not registered in the saved phase
     * or has no supported codec
     * @since 1.8.4
     */
    @Nullable
    public Packet<?> toPacket() {
        return packet == null ? null : toPacket(packet.getClass(), protocol);
    }

    /**
     * the packet named, built from this buffer.
     * <p>
     * Looks up the convenience name and decodes at the current reader position. Packet-backed
     * helpers use their saved phase; fresh helpers default to play.
     * <br>
     * The names are the ones {@link #getPacketNames()} lists.
     *
     * @param packetName the name of the packet's class that should be returned
     * @return the packet for this buffer.
     * @throws IllegalArgumentException if the name is unknown or no supported codec is available
     * @since 1.8.4
     */
    @DocletReplaceParams("packetName: PacketName")
    public Packet<?> toPacket(String packetName) {
        Class<? extends Packet<?>> clazz = PACKETS.get(packetName);
        if (clazz == null) throw new IllegalArgumentException("Unknown packet: " + packetName);
        return toPacket((Class<? extends Packet>) clazz);
    }

    /**
     * Decodes a named packet at the current reader position using an explicit network phase.
     *
     * @param packetName a convenience name from {@link #getPacketNames()}
     * @param protocol the network phase
     * @return the decoded packet
     * @throws IllegalArgumentException if the name or phase is unknown, the packet is not
     * registered in that phase, or no supported codec is available
     */
    public Packet<?> toPacket(String packetName, String protocol) {
        Class<? extends Packet<?>> clazz = PACKETS.get(packetName);
        if (clazz == null) throw new IllegalArgumentException("Unknown packet: " + packetName);
        return toPacket((Class<? extends Packet>) clazz, protocol);
    }

    /**
     * the packet of the given class, built from this buffer.
     * <p>
     * Decodes at the current reader position. Non-play helpers use their saved phase; play
     * helpers use the registered decoder lookup. The bytes must match the requested codec.
     *
     * @param clazz the class of the packet to return
     * @return the packet for this buffer.
     * @throws IllegalArgumentException if no supported codec is available
     * @since 1.8.4
     */
    public Packet<?> toPacket(Class<? extends Packet> clazz) {
        if (!protocol.equals("play")) return toPacket(clazz, protocol);
        Function<FriendlyByteBuf, ? extends Packet<?>> decoder = BUFFER_TO_PACKET.get(clazz);
        if (decoder == null) throw new IllegalArgumentException("Packet has no supported codec: " + clazz);
        return decoder.apply(base);
    }

    /**
     * Decodes at the current reader position using a phase-specific codec, including configuration
     * custom payloads. The bytes must match the requested codec.
     *
     * @param clazz the packet class
     * @param protocol the network phase
     * @return the decoded packet
     * @throws IllegalArgumentException if the phase is unknown, the class is not registered in
     * that phase, or no supported codec is available
     */
    public Packet<?> toPacket(Class<? extends Packet> clazz, String protocol) {
        getPacketId((Class<? extends Packet<?>>) clazz, protocol);
        Field field = findCodecField(clazz, protocol);
        if (field == null) throw new IllegalArgumentException("Packet has no supported codec: " + clazz);
        try {
            StreamCodec<FriendlyByteBuf, ? extends Packet<?>> codec =
                    (StreamCodec<FriendlyByteBuf, ? extends Packet<?>>) field.get(null);
            return codec.decode(base);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read packet codec for " + clazz, e);
        }
    }

    /**
     * Returns the packet ID registered for the packet class. The ID is specific to the
     * protocol and direction, and may change across Minecraft versions; if the class is
     * registered in multiple protocols, use {@link #getPacketId(Class, String)}.
     *
     * @param packetClass the class of the packet to get the id for
     * @return the ID registered for the packet class
     * @throws IllegalArgumentException if the class is unknown or registered in multiple protocols
     * @since 1.8.4
     */
    public int getPacketId(Class<? extends Packet<?>> packetClass) {
        Map<ConnectionProtocol, Integer> ids = PACKET_IDS_BY_PROTOCOL.get(packetClass);
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Unknown packet: " + packetClass);
        if (ids.size() != 1) throw new IllegalArgumentException("Packet occurs in multiple protocols; use getPacketId(packetClass, protocol): " + packetClass);
        return ids.values().iterator().next();
    }

    /** Returns the packet ID registered for the requested protocol (for example, "play" or "configuration").
     * Throws {@link IllegalArgumentException} if the protocol is unknown or the packet is not registered in it.
     *
     * @param packetClass the packet class
     * @param protocol the protocol identifier or name
     * @return the registered packet ID
     */
    public int getPacketId(Class<? extends Packet<?>> packetClass, String protocol) {
        Map<ConnectionProtocol, Integer> ids = PACKET_IDS_BY_PROTOCOL.get(packetClass);
        ConnectionProtocol phase = resolveProtocol(protocol);
        if (ids == null || !ids.containsKey(phase)) throw new IllegalArgumentException("Packet is not registered in " + protocol + ": " + packetClass);
        return ids.get(phase);
    }

    /**
     * which network state a packet class belongs to, in the game's own numbering.
     * <p>
     * This overload requires the packet class to occur in exactly one protocol. Use
     * {@link #getNetworkStateId(Class, String)} for a class shared between phases.
     * <br>
     * The state is which part of the connection a packet belongs to — logging in, playing, or
     * configuring — and it is why a packet cannot simply be read on the wrong one.
     *
     * @param packetClass the class of the packet to get the id for
     * @return the ordinal of the registered connection protocol
     * @throws IllegalArgumentException if the class is unknown or belongs to multiple protocols
     * @since 1.8.4
     */
    public int getNetworkStateId(Class<? extends Packet<?>> packetClass) {
        Map<ConnectionProtocol, Integer> ids = PACKET_IDS_BY_PROTOCOL.get(packetClass);
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Unknown packet: " + packetClass);
        if (ids.size() != 1) throw new IllegalArgumentException("Packet occurs in multiple protocols; use getNetworkStateId(packetClass, protocol): " + packetClass);
        return ids.keySet().iterator().next().ordinal();
    }

    /** Returns the ordinal of the requested protocol for a packet registered in it.
     * Throws {@link IllegalArgumentException} if the protocol is unknown or the packet is not registered in it.
     *
     * @param packetClass the packet class
     * @param protocol the protocol identifier or name
     * @return the requested protocol's ordinal
     */
    public int getNetworkStateId(Class<? extends Packet<?>> packetClass, String protocol) {
        Map<ConnectionProtocol, Integer> ids = PACKET_IDS_BY_PROTOCOL.get(packetClass);
        ConnectionProtocol phase = resolveProtocol(protocol);
        if (ids == null || !ids.containsKey(phase)) throw new IllegalArgumentException("Packet is not registered in " + protocol + ": " + packetClass);
        return phase.ordinal();
    }

    /**
     * whether a packet class is one the server sends to the client.
     * <p>
     * Reads the registered packet direction rather than inferring it from a convenience name.
     * <br>
     * A name is the practical test for this: {@link #getPacketNames()} ends almost every
     * clientbound one with {@code S2C} and every serverbound one with {@code C2S}, and the
     * event's {@code type} is that same name.
     *
     * @param packetClass the class to get the side for
     * @return {@code true} if the packet is clientbound, {@code false} if it is serverbound
     * @throws IllegalArgumentException if the class is unknown
     * @since 1.8.4
     */
    public boolean isClientbound(Class<? extends Packet<?>> packetClass) {
        if (!PACKET_SIDES.containsKey(packetClass)) throw new IllegalArgumentException("Unknown packet: " + packetClass);
        return PACKET_SIDES.getBoolean(packetClass);
    }

    /**
     * whether a packet class is one the client sends to the server.
     * <p>
     * This is the exact opposite of {@link #isClientbound(Class)} for a registered packet class.
     *
     * @param packetClass the class to get the id for
     * @return {@code true} if the packet is serverbound, {@code false} if it is clientbound
     * @throws IllegalArgumentException if the class is unknown
     * @since 1.8.4
     */
    public boolean isServerbound(Class<? extends Packet<?>> packetClass) {
        return !isClientbound(packetClass);
    }

    /**
     * Send a packet of the given type, created from this buffer, to the server.
     * <p>
     * Decodes through {@link #toPacket()} at the current reader position. A helper without an
     * original packet is a no-op. Sending requires a connection and a suitable serverbound packet.
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
     * builds a packet of the named type from this buffer and sends it to the server.
     * <p>
     * Decodes through the class overload at the current reader position. Requires a connection,
     * a supported codec and bytes matching a suitable serverbound packet.
     *
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
     * handles a packet as though the client had received it, for testing a listener.
     * <p>
     * Nothing is converted here: this handles the original packet object, not modified buffer
     * bytes. The named and class overloads decode instead. A helper without an original packet
     * is a no-op in all three receive overloads.
     * <br>
     * Nothing is sent anywhere either, so this is the opposite direction from
     * {@link #sendPacket()} and the send event's {@code replacePacket} has no bearing on it.
     * <br>
     * The listener type is not checked before handling; the packet's handler receives the current
     * client game connection, so use a packet compatible with that listener.
     *
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
     * handles a packet of the named type as though the client had received it.
     * <p>
     * If this helper has an original packet, decodes through {@link #toPacket(String)} and
     * handles the result with the client game connection. Otherwise this is a no-op. The bytes
     * must be correctly positioned and describe a compatible client game packet.
     *
     * @param packetName the name of the packet's class that should be received
     * @return self for chaining.
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
     * Decodes the requested packet at the current reader position and handles it with the
     * client game connection. A helper without an original packet is a no-op; the listener type
     * is not checked before handling.
     *
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
     * Returns the convenience names registered for packet classes.
     * <p>
     * Names are also used as packet-event {@code type} values and by filterer
     * {@code setType}. Most encode direction in an {@code S2C} or {@code C2S} suffix, but
     * this is a naming convention, not authoritative direction metadata; use
     * {@link #isClientbound(Class)} or {@link #isServerbound(Class)} when available. Names
     * are convenience aliases and may change. {@link #getPacketName(Packet)} can return a
     * class's simple name when that class has no registered alias.
     *
     * @return a list of all packet names.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PacketName>")
    public List<String> getPacketNames() {
        return ImmutableList.copyOf(PACKETS.keySet());
    }

    /**
     * Restores the buffer contents and reader position captured when this helper was created.
     * <p>
     * For a packet-backed helper, this is the encoded packet data, not an empty buffer. The
     * helper's buffer is separate from the packet object; resetting it does not change that
     * object. Registry access is retained when restoring a registry-backed buffer.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper reset() {
        base = originalRegistryAccess != null
                ? new RegistryFriendlyByteBuf(original.copy(), originalRegistryAccess)
                : new FriendlyByteBuf(original.copy());
        return this;
    }

    /**
     * Writes a registry key to the buffer.
     *
     * @param key the key to write
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeRegistryKey(ResourceKey<?> key) {
        base.writeResourceKey(key);
        return this;
    }

    /**
     * Reads a registry key from the buffer.
     *
     * @param registry the registry the key belongs to
     * @param <T> the type of values identified by the registry key
     * @return the decoded key
     * @since 1.8.4
     */
    public <T> ResourceKey<T> readRegistryKey(ResourceKey<? extends Registry<T>> registry) {
        return base.readResourceKey(registry);
    }

    /**
     * Writes a collection and its elements to the buffer.
     *
     * @param collection the collection to write
     * @param writer     writes each element
     * @param <T> the type of elements in the collection
     * @return self for chaining.
     * @since 1.8.4
     */
    public <T> PacketByteBufferHelper writeCollection(Collection<T> collection, MethodWrapper<FriendlyByteBuf, T, ?, ?> writer) {
        PacketBufferCompat.writeCollection(base, collection, writer::accept);
        return this;
    }

    /**
     * Reads a list of elements from the buffer.
     *
     * @param reader reads each element
     * @param <T> the type of elements read
     * @return the decoded elements
     * @since 1.8.4
     */
    public <T> List<T> readList(MethodWrapper<FriendlyByteBuf, ?, T, ?> reader) {
        return PacketBufferCompat.readList(base, reader::apply);
    }

    /**
     * Writes an integer list to the buffer.
     *
     * @param list the list to write
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeIntList(Collection<Integer> list) {
        PacketBufferCompat.writeIntList(base, new IntArrayList(list));
        return this;
    }

    /**
     * Reads an integer list from the buffer.
     *
     * @return the decoded list
     * @since 1.8.4
     */
    public IntList readIntList() {
        return PacketBufferCompat.readIntList(base);
    }

    /**
     * Writes a map and its keys and values to the buffer.
     *
     * @param map         the map to write
     * @param keyWriter   writes each key
     * @param valueWriter writes each value
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return self for chaining.
     * @since 1.8.4
     */
    public <K, V> PacketByteBufferHelper writeMap(Map<K, V> map, MethodWrapper<FriendlyByteBuf, K, ?, ?> keyWriter, MethodWrapper<FriendlyByteBuf, V, ?, ?> valueWriter) {
        PacketBufferCompat.writeMap(base, map, keyWriter::accept, valueWriter::accept);
        return this;
    }

    /**
     * Reads a map's keys and values from the buffer.
     *
     * @param keyReader   reads each map key
     * @param valueReader reads each map value
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return the read map.
     * @since 1.8.4
     */
    public <K, V> Map<K, V> readMap(MethodWrapper<FriendlyByteBuf, ?, K, ?> keyReader, MethodWrapper<FriendlyByteBuf, ?, V, ?> valueReader) {
        return PacketBufferCompat.readMap(base, keyReader::apply, valueReader::apply);
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
     * @param <T> the value type
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
     * @param <T> the value type
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
     * @param <T> the value type
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
     * Reads and discards an int array, then returns this helper. The reader rejects an encoded
     * array length greater than {@code maxSize}.
     *
     * @param maxSize the maximum size of the int array to read
     * @return this helper
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
     * Reads and returns a long array. The {@code maxSize} argument is ignored; the underlying
     * reader instead rejects a length greater than the number of complete longs remaining in
     * the buffer.
     *
     * @param maxSize the maximum size of the long array to read
     * @return the read long array
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
     * @return the chunk coordinates, with x at index 0 and z at index 1.
     * @since 1.8.4
     */
    public int[] readChunkPos() {
        ChunkPos pos = base.readChunkPos();
        return new int[]{ChunkPosCompat.x(pos), ChunkPosCompat.z(pos)};
    }

    /**
     * Reads a chunk position and looks up that chunk in the current client level.
     *
     * @return its {@link ChunkHelper}, or {@code null} if the lookup returns no chunk.
     * @since 1.8.4
     */
    @Nullable
    public ChunkHelper readChunkHelper() {
        ChunkPos pos = base.readChunkPos();
        assert Minecraft.getInstance().level != null;
        ChunkAccess chunk = Minecraft.getInstance().level.getChunk(ChunkPosCompat.x(pos), ChunkPosCompat.z(pos));
        return chunk == null ? null : new ChunkHelper(chunk);
    }

    /**
     * @param chunkX the section's x coordinate
     * @param y      the section's y coordinate
     * @param chunkZ the section's z coordinate
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkSectionPos(int chunkX, int y, int chunkZ) {
        //? if <=1.21.8 {
        base.writeSectionPos(SectionPos.of(chunkX, y, chunkZ));
        //? } else {
        /*SectionPos.STREAM_CODEC.encode(base, SectionPos.of(chunkX, y, chunkZ));
         *///? }
        return this;
    }

    /**
     * @param chunk the chunk whose x and z coordinates define the section position
     * @param y     the section's y coordinate
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeChunkSectionPos(ChunkHelper chunk, int y) {
        //? if <=1.21.8 {
        base.writeSectionPos(SectionPos.of(chunk.getRaw().getPos(), y));
        //? } else {
        /*SectionPos.STREAM_CODEC.encode(base, SectionPos.of(chunk.getRaw().getPos(), y));
         *///? }
        return this;
    }

    /**
     * @return the read section coordinates, wrapped as a {@link BlockPosHelper}.
     * @since 1.8.4
     */
    public BlockPosHelper readChunkSectionPos() {
        //? if <=1.21.8 {
        SectionPos pos = base.readSectionPos();
        //? } else {
        /*SectionPos pos = SectionPos.STREAM_CODEC.decode(base);
         *///? }
        return new BlockPosHelper(pos.x(), pos.y(), pos.z());
    }

    /**
     * @param dimension the dimension identifier; vanilla dimensions include {@code overworld},
     *                  {@code the_nether}, and {@code the_end}
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
     * @param dimension the dimension identifier; vanilla dimensions include {@code overworld},
     *                  {@code the_nether}, and {@code the_end}
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
     * @param constant the enum constant to encode by ordinal
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeEnumConstant(Enum<?> constant) {
        base.writeEnum(constant);
        return this;
    }

    /**
     * @param enumClass the enum class whose constant is selected by the encoded ordinal
     * @param <T> the enum type to read
     * @return the enum constant at the encoded ordinal
     * @since 1.8.4
     */
    public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
        return base.readEnum(enumClass);
    }

    /**
     * @param i the integer to encode as a variable-length integer
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
        //? if >=1.21.11 {
        /*base.writeLong(date.getTime());
        *///? } else {
        base.writeDate(date);
        //? }
        return this;
    }

    /**
     * @return the read date.
     * @since 1.8.4
     */
    public Date readDate() {
        //? if >=1.21.11 {
        /*return new Date(base.readLong());
        *///? } else {
        return base.readDate();
        //? }
    }

    /**
     * @param instant the instant to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeInstant(Instant instant) {
        base.writeLong(instant.toEpochMilli());
        return this;
    }

    /**
     * @return the read instant.
     * @since 1.8.4
     */
    public Instant readInstant() {
        return Instant.ofEpochMilli(base.readLong());
    }

    /**
     * @param key the public key to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writePublicKey(PublicKey key) {
        base.writeByteArray(key.getEncoded());
        return this;
    }

    /**
     * @return the read public key.
     * @since 1.8.4
     */
    public PublicKey readPublicKey() {
        return PacketBufferCompat.readPublicKey(base);
    }

    /**
     * @param hitResult the hit result to store
     * @return self for chaining.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#writeBlockHitResult(HitResultHelper.Block hitResult)} instead.
     */
    @Deprecated
    @DocletIgnore
    public PacketByteBufferHelper writeBlockHitResult(BlockHitResult hitResult) {
        PacketBufferCompat.writeBlockHitResult(base, hitResult);
        return this;
    }

    /**
     * @param hitResult the hit result to store
     * @return self for chaining.
     * @since 1.9.1
     */
    public PacketByteBufferHelper writeBlockHitResult(HitResultHelper.Block hitResult) {
        PacketBufferCompat.writeBlockHitResult(base, hitResult.getRaw());
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
        PacketBufferCompat.writeBlockHitResult(base, result);
        return this;
    }

    /**
     * @return the read block hit result.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#readBlockHitResultHelper()} instead.
     */
    @Deprecated
    @DocletIgnore
    public BlockHitResult readBlockHitResult() {
        return PacketBufferCompat.readBlockHitResult(base);
    }

    /**
     * @return a map of the block hit result's data and their values.
     * @since 1.8.4
     * @deprecated use {@link PacketByteBufferHelper#readBlockHitResultHelper()} instead.
     */
    @Deprecated
    @DocletIgnore
    public Map<String, Object> readBlockHitResultMap() {
        BlockHitResult hitResult = PacketBufferCompat.readBlockHitResult(base);
        return ImmutableMap.of("side", new DirectionHelper(hitResult.getDirection()), "blockPos", new BlockPosHelper(hitResult.getBlockPos()), "missed", hitResult.getType() == HitResult.Type.MISS, "inside", hitResult.isInside());
    }

    /**
     * @return the read block hit result as a helper.
     * @since 1.9.1
     */
    public HitResultHelper.Block readBlockHitResultHelper() {
        return new HitResultHelper.Block(PacketBufferCompat.readBlockHitResult(base));
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
     * Marks the current writer index for later use by {@link #resetWriterIndex()}.
     *
     * @return self for chaining.
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
     * Reads a byte at the given index without changing the reader index.
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
     * Reads an unsigned byte at the given index without changing the reader index.
     * @param index the index to read from
     * @return the char at the given index.
     * @since 1.8.4
     */
    public char getChar(int index) {
        return base.getChar(index);
    }

    /**
     * Writes a short at the current writer index.
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeByte(int value) {
        base.writeByte(value);
        return this;
    }

    /**
     * Writes a short at the given index without changing the writer index.
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
     * Reads a short from the current reader index.
     * @return the read short value.
     * @since 1.8.4
     */
    public short readShort() {
        return base.readShort();
    }

    /**
     * Reads an unsigned short from the current reader index.
     * @return the read unsigned short value, represented as an int.
     * @since 1.8.4
     */
    public int readUnsignedShort() {
        return base.readUnsignedShort();
    }

    /**
     * Reads a short at the given index without changing the reader index.
     * @param index the index to read from
     * @return the short at the given index.
     * @since 1.8.4
     */
    public short getShort(int index) {
        return base.getShort(index);
    }

    /**
     * Reads an unsigned short at the given index without changing the reader index.
     * @param index the index to read from
     * @return the unsigned short at the given index, represented as an int.
     * @since 1.8.4
     */
    public int getUnsignedShort(int index) {
        return base.getUnsignedShort(index);
    }

    /**
     * Writes a medium at the current writer index.
     * @param value the value to store
     * @return self for chaining.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeMedium(int value) {
        base.writeMedium(value);
        return this;
    }

    /**
     * Writes a medium at the given index without changing the writer index.
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
     * Reads a signed medium from the current reader index.
     * @return the value read.
     * @since 1.8.4
     */
    public int readMedium() {
        return base.readMedium();
    }

    /**
     * Reads an unsigned medium from the current reader index.
     * @return the value read.
     * @since 1.8.4
     */
    public int readUnsignedMedium() {
        return base.readUnsignedMedium();
    }

    /**
     * @param index the byte index to read from
     * @return the signed medium at that index.
     * @since 1.8.4
     */
    public int getMedium(int index) {
        return base.getMedium(index);
    }

    /**
     * @param index the byte index to read from
     * @return the unsigned medium at that index.
     * @since 1.8.4
     */
    public int getUnsignedMedium(int index) {
        return base.getUnsignedMedium(index);
    }

    /**
     * Writes an integer at the current writer index.
     * @param value the value to write
     * @return this buffer helper.
     * @since 1.8.4
     */
    public PacketByteBufferHelper writeInt(int value) {
        base.writeInt(value);
        return this;
    }

    /**
     * Writes an integer at the given byte index without changing the writer index.
     * @param index the byte index to write to
     * @param value the value to write
     * @return this buffer helper.
     * @since 1.8.4
     */
    public PacketByteBufferHelper setInt(int index, int value) {
        base.setInt(index, value);
        return this;
    }

    /**
     * Reads an integer from the current reader index.
     * @return the value read.
     * @since 1.8.4
     */
    public int readInt() {
        return base.readInt();
    }

    /**
     * Reads an unsigned integer from the current reader index.
     * @return the value read, represented as a long.
     * @since 1.8.4
     */
    public long readUnsignedInt() {
        return base.readUnsignedInt();
    }

    /**
     * @param index the byte index to read from
     * @return the integer at that index.
     * @since 1.8.4
     */
    public int getInt(int index) {
        return base.getInt(index);
    }

    /**
     * @param index the byte index to read from
     * @return the unsigned integer at that index, represented as a long.
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
     * @return the bytes at the specified index; the reader index is unchanged
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

    /**
     * Returns a diagnostic string containing the underlying buffer's string representation.
     *
     * @return a diagnostic string describing the underlying buffer
     */
    @Override
    public String toString() {
        return String.format("PacketByteBufferHelper:{\"base\": %s}", base);
    }

    /**
     * Returns the shortcut name for a packet, or its simple class name when no shortcut is registered.
     * <p>
     * The returned name is also used as packet-event {@code type} and accepted by packet filterers.
     *
     * @param packet the packet to name
     * @return the packet's shortcut name, or its simple class name if none is registered
     * @since 1.8.4
     */
    public static String getPacketName(Packet<?> packet) {
        return PACKET_NAMES.getOrDefault(packet.getClass(), packet.getClass().getSimpleName());
    }

    /**
     * Populates convenience packet-name mappings and registers packet IDs, directions, and
     * supported standalone play-phase decoders using reflection. Reflective lookup failures may
     * cause an {@code IllegalStateException}.
     */
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
        //? if >=1.21.11 {
        /*PACKETS.put("ClientboundMountScreenOpenPacket", net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket.class);
        *///? } else {
        PACKETS.put("OpenHorseScreenS2CPacket", net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket.class);
        //? }
        PACKETS.put("CommandExecutionC2SPacket", net.minecraft.network.protocol.game.ServerboundChatCommandPacket.class);
        PACKETS.put("CraftRequestC2SPacket", net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket.class);
        //? if >=26.3 {
        /*PACKETS.put("HandSwingC2SPacket", net.minecraft.network.protocol.game.ServerboundPunchPacket.class);
        *///?} else {
        PACKETS.put("HandSwingC2SPacket", net.minecraft.network.protocol.game.ServerboundSwingPacket.class);
        //?}
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
        registerPacketCodecsAndMetadata();
    }

    private static void registerPacketCodecsAndMetadata() {
        String root = "net.minecraft.network.protocol.";
        String[] phases = {"handshake", "status", "login", "configuration", "game"};
        Map<PacketType<?>, Class<? extends Packet<?>>> classes = new HashMap<>();
        String[] typePackages = {"handshake", "status", "login", "configuration", "game", "common", "cookie", "ping"};
        for (String phase : typePackages) {
            try {
                Class<?> types = Class.forName(root + phase + "." + switch (phase) {
                    case "game" -> "GamePacketTypes";
                    default -> Character.toUpperCase(phase.charAt(0)) + phase.substring(1) + "PacketTypes";
                });
                for (Field field : types.getFields()) {
                    if (!(field.getGenericType() instanceof ParameterizedType generic) ||
                            !(generic.getActualTypeArguments()[0] instanceof Class<?> packetClass) ||
                            !Packet.class.isAssignableFrom(packetClass)) continue;
                    classes.put((PacketType<?>) field.get(null), (Class<? extends Packet<?>>) packetClass);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read packet types for " + phase, e);
            }
        }
        for (String phase : phases) {
            try {
                Class<?> protocols = Class.forName(root + phase + "." + switch (phase) {
                    case "game" -> "GameProtocols";
                    default -> Character.toUpperCase(phase.charAt(0)) + phase.substring(1) + "Protocols";
                });
                for (Field field : protocols.getFields()) {
                    if (!field.getName().endsWith("_TEMPLATE")) continue;
                    ProtocolInfo.Details details = ((ProtocolInfo.DetailsProvider) field.get(null)).details();
                    details.listPackets((type, index) -> {
                        Class<? extends Packet<?>> packetClass = classes.get(type);
                        if (packetClass == null) return;
                        PACKET_IDS_BY_PROTOCOL.computeIfAbsent(packetClass, key -> new EnumMap<>(ConnectionProtocol.class))
                                .put(details.id(), index);
                        PACKET_SIDES.put(packetClass, details.flow() == PacketFlow.CLIENTBOUND);
                    });
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read packet protocols for " + phase, e);
            }
        }
        for (Class<? extends Packet<?>> clazz : classes.values()) {
            try {
                Field codecField = findCodecField(clazz, "play");
                if (codecField == null) continue;
                StreamCodec<FriendlyByteBuf, ? extends Packet<?>> codec = (StreamCodec<FriendlyByteBuf, ? extends Packet<?>>) codecField.get(null);
                BUFFER_TO_PACKET.put(clazz, codec::decode);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read codec for " + clazz, e);
            }
        }
    }

    @Nullable
    private static Field findCodecField(Class<?> clazz, String protocol) {
        String preferred = null;
        if (protocol.equalsIgnoreCase("configuration")) {
            if (clazz == ClientboundCustomPayloadPacket.class) preferred = "CONFIG_STREAM_CODEC";
            else if (clazz.getSimpleName().equals("ClientboundShowDialogPacket")) preferred = "CONTEXT_FREE_STREAM_CODEC";
        } else if (clazz == ClientboundCustomPayloadPacket.class) {
            preferred = "GAMEPLAY_STREAM_CODEC";
        }
        String codecName = preferred;
        return Arrays.stream(clazz.getFields())
                .filter(field -> StreamCodec.class.isAssignableFrom(field.getType()))
                .filter(field -> field.getName().equals("STREAM_CODEC") || field.getName().equals(codecName))
                .sorted(Comparator.comparingInt(field -> field.getName().equals(codecName) ? 0 : 1))
                .findFirst().orElse(null);
    }

    private static ConnectionProtocol resolveProtocol(String protocol) {
        for (ConnectionProtocol phase : ConnectionProtocol.values()) {
            if (phase.id().equalsIgnoreCase(protocol) || phase.name().equalsIgnoreCase(protocol)) return phase;
        }
        throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }

    /**
     * Checks whether the packet has a standalone codec for the phase selected as in
     * {@link #PacketByteBufferHelper(Packet)}. Returns {@code false} for {@code null}, unknown
     * packets, ambiguous phase selection, or missing codecs; it does not test whether encoding
     * the packet's contents will succeed.
     *
     * @param packet the packet to check, or {@code null}
     * @return whether the packet has a supported standalone codec
     * @since 2.0.0
     */
    public static boolean canSerialize(@Nullable Packet<?> packet) {
        if (packet == null) return false;
        try {
            String protocol = preferredProtocol(packet, mc.getConnection() == null ? null : mc.getConnection().protocol());
            return findCodecField(packet.getClass(), protocol) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Selects a registered phase for the packet, preferring {@code current} when registered.
     * If no current phase applies, selects the sole registered phase; throws
     * {@link IllegalArgumentException} for an unknown packet or when multiple phases remain.
     *
     * @param packet the packet whose registered phase to select
     * @param current the current connection phase, or {@code null}
     * @return the selected phase identifier
     */
    public static String preferredProtocol(Packet<?> packet, @Nullable ConnectionProtocol current) {
        Map<ConnectionProtocol, Integer> ids = PACKET_IDS_BY_PROTOCOL.get(packet.getClass());
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Unknown packet: " + packet.getClass());
        if (current != null && ids.containsKey(current)) return current.id();
        if (ids.size() == 1) return ids.keySet().iterator().next().id();
        throw new IllegalArgumentException("Packet occurs in multiple protocols; specify its phase: " + packet.getClass());
    }

    /**
     * Runs the packet-mapping generator: initializes mappings, scans legacy packet classes on the
     * class path (including declared nested classes), and prints missing {@code PACKETS.put} lines
     * to standard output. Class-path scanning can fail with {@code IOException}; arguments are
     * ignored.
     *
     * @param args ignored command-line arguments
     */
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
