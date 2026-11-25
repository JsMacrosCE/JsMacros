# Packet registry and packet events

At client startup the packet helpers read Minecraft's own protocol definitions to
learn each packet's names, ids per connection phase, direction and standalone
codec. Packet names are also what packet events report and what packet
filterers accept.

That registry must not depend on Mojang class or field names. Production Fabric
clients remap Minecraft classes and fields to intermediary names, while strings
written in the mod keep their original spelling, so a registry that looked up
names like `HandshakePacketTypes`, `_TEMPLATE` or `STREAM_CODEC` worked in dev
but crashed packet events in production. These tests cover the public view of
the registry: names, decoding and packet events.

`PACKET-NAMES-001` and `PACKET-DECODE-001` are `auto` mode and run under
`Run Automated`. `PACKET-EVENT-001` is `event` mode and completes once it has
seen the outgoing slot-change packet.

## Coverage

| Test | Area | Expected behavior |
| --- | --- | --- |
| `PACKET-NAMES-001` | Packet registry | Registry names are present, include the legacy event name and the modern alias for the same packet, cover both directions, and are unique. |
| `PACKET-DECODE-001` | Packet decoding | A play packet decodes by legacy name and by alias, consumes its body, rejects unknown names, and exposes phase ids and direction from the decoded class. A packet with separate play and configuration codecs decodes with the configuration codec. |
| `PACKET-EVENT-001` | Packet events | `SendPacket` fires for a hotbar slot change, keeps the legacy name as `type`, is matched by both a legacy-typed and an alias-typed filterer, and its buffer decodes back to the reported name. |

## Run steps

1. Run `[Run Automated]` (or the `Network` category) for `PACKET-NAMES-001`
   and `PACKET-DECODE-001`. They work in a world or from the main menu.
2. Run `PACKET-EVENT-001`, close chat, and press a hotbar key (`1`-`9`) for a
   slot other than the selected one.
3. Record the automatic-check count and the outcome for each test.
4. Repeat on a production Fabric client; that is where the registry previously
   failed to load.

## Expected results

- `PACKET-NAMES-001`: the list holds more than a hundred names, contains
  `ChatMessageS2CPacket` and `ClientboundPlayerChatPacket`,
  `UpdateSelectedSlotC2SPacket` and `ServerboundSetCarriedItemPacket`, and both
  `KeepAliveS2CPacket` and `KeepAliveC2SPacket`; no name appears twice.
- `PACKET-DECODE-001`: the crafted ping body decodes from both names and the
  reader reaches the end of its four bytes; an unknown name throws; the ping
  reports play and configuration ids, the phase-less `getPacketId` overload
  rejects it as ambiguous, and it is clientbound. A `minecraft:brand` custom
  payload decodes in the configuration phase, and a keep-alive reply is
  serverbound.
- `PACKET-EVENT-001`: `type` is `UpdateSelectedSlotC2SPacket`, the alias-filtered
  listener fires for the same packet, `canGetPacketBuffer()` is true, and
  decoding the event's buffer by that name succeeds.

## Exclusions / known issues

- Scripts cannot inspect which codec field a phase resolved to, only whether a
  decode with that phase succeeds. A packet whose play and configuration codecs
  accept the same bytes therefore looks the same in both phases.
- `PACKET-DECODE-001` asserts the ping body is four bytes because it is one
  `int`. If Mojang changes that wire format the byte count needs updating; the
  decode itself is the behavior under test.
- Running the automated batch in a world and from the main menu both matter: the
  menu gives a plain buffer, a connection gives a registry-backed one.
- `PACKET-EVENT-001` needs a server connection (single-player is enough) and a
  slot change. It only reads the packet and does not cancel or replace it.
- No test constructs packets or uses `replacePacket`; packet editing stays
  documented but out of scope here.
- `PACKET-EVENT-001` pins the current naming policy, where the event type is the
  legacy name even though modern aliases exist. Update the check when legacy
  names are removed in 3.0.0.
