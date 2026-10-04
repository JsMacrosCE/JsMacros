// Harness-only checks with host fixtures, not real Minecraft API validation.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, 'misc_api_regressions.js'), 'utf8');
const boundary = source.indexOf("run('Line and builder preserve bounds and direction in all orientations'");
assert.notEqual(boundary, -1, 'runner prelude boundary must exist');
const prelude = source.slice(0, boundary);

function runner({ joined = false, main = false, chatError = false, schedulerError = false } = {}) {
  const output = [];
  const errors = [];
  const chat = [];
  let onMainThread = main;
  const context = {
    Java: {
      type(name) {
        if (name === 'java.lang.System') return {
          out: { println: message => output.push(message) },
          err: { println: message => errors.push(message) }
        };
        if (name === 'net.minecraft.client.Minecraft') return {
          getInstance: () => ({ isSameThread: () => onMainThread })
        };
        if (name === 'com.jsmacrosce.jsmacros.client.JsMacrosClient') return {
          clientCore: { profile: { checkJoinedThreadStack: () => joined } }
        };
        return {}; // Other class handles are unused by the runner prelude.
      }
    },
    Chat: {
      log(message) {
        assert.equal(onMainThread, true, 'Chat.log must only run on the client thread');
        if (chatError) throw new Error('font upload failed');
        chat.push(message);
      }
    },
    JavaWrapper: { methodToJava: callback => callback },
    Client: {
      runOnMainThread(callback, awaitCompletion, timeout) {
        assert.equal(awaitCompletion, true, 'wait for reporting before script finishes');
        assert.equal(timeout, 5000);
        if (schedulerError) throw new Error('scheduler rejected callback');
        onMainThread = true;
        try { callback(); } finally { onMainThread = main; }
      }
    }
  };
  vm.createContext(context);
  return { context, output, errors, chat, initialize: () => vm.runInContext(prelude, context) };
}

let harness = runner();
harness.initialize();
harness.context.run('success', () => {});
harness.context.run('failure', () => { throw new Error('original assertion'); });
harness.context.skip('missing fixture');
assert.equal(harness.context.passes, 1);
assert.equal(harness.context.failures, 1);
assert.equal(harness.context.skips, 1);
assert.equal(harness.chat.length, 0, 'test execution must not render chat');
assert.equal(harness.output.length, 3, 'all results logged immediately');
assert.match(harness.output[1], /original assertion/);
harness.context.reportResults();
assert.deepEqual(harness.chat, [harness.output.join('\n')]);
assert.equal(harness.errors.length, 0);

for (const options of [{ chatError: true }, { schedulerError: true }]) {
  harness = runner(options);
  harness.initialize();
  harness.context.run('success', () => {});
  harness.context.run('failure', () => { throw new Error('original assertion'); });
  assert.doesNotThrow(() => harness.context.reportResults());
  assert.equal(harness.context.passes, 1, 'reporting failure must not invalidate passed test');
  assert.equal(harness.context.failures, 1, 'reporting failure must not add a test failure');
  assert.match(harness.output[1], /original assertion/, 'original failure remains visible');
  assert.match(harness.errors[0], /REPORT ERROR.*latest\.log/);
}

for (const options of [{ joined: true }, { main: true }]) {
  harness = runner(options);
  assert.throws(harness.initialize, /non-joined macro/);
  assert.match(harness.errors[0], /disable Joined/);
  assert.equal(harness.output.length, 0, 'reject unsafe invocation before running tests');
  assert.equal(harness.chat.length, 0, 'do not render rejection from the script thread');
}

// Execute the real entity enumeration block: top-level var assignments can overwrite helpers.
const entityStart = source.indexOf('var entities = World.getEntities();');
const entityEnd = source.indexOf("run('Vec3D pitch and non-aliasing to3D copy'");
assert.ok(entityStart !== -1 && entityEnd > entityStart, 'entity enumeration boundaries');
const entityEnumeration = source.slice(entityStart, entityEnd);
const boatIds = ['minecraft:oak_boat', 'minecraft:bamboo_raft', 'minecraft:oak_chest_boat', 'minecraft:bamboo_chest_raft'];
for (const ids of [null, [], ['minecraft:chicken'], boatIds, ['minecraft:chicken', ...boatIds]]) {
  harness = runner();
  harness.initialize();
  const lookup = harness.context.type;
  const entities = ids == null ? null : ids.map(id => ({
    getType: () => id,
    isChestBoat: () => id.includes('chest_')
  }));
  harness.context.World = {
    getEntities: () => entities == null ? null : {
      size: () => entities.length,
      get: index => entities[index]
    }
  };
  harness.context.BoatHelper = { class: { isInstance: entity => boatIds.includes(entity.getType()) } };
  vm.runInContext(entityEnumeration, harness.context);
  assert.equal(harness.context.type, lookup, 'entity IDs must not overwrite class lookup helper');
  assert.doesNotThrow(() => harness.context.type('api.math.Vec3D'), 'later tests can still resolve classes');
  assert.equal(harness.context.failures, 0);
  assert.equal(harness.context.passes, ids == null ? 0 : ids.filter(id => boatIds.includes(id)).length);
}

// Execute the actual beginning of the packet test with a deliberately non-constructible singleton.
const packetStart = source.indexOf("run('Packet phase metadata, shared ambiguity, named decoding and unknown errors'");
const packetEnd = source.indexOf("run('Scoreboard nullable team/objective handling on a local empty scoreboard'");
assert.ok(packetStart !== -1 && packetEnd > packetStart, 'packet test boundaries');
harness = runner();
harness.initialize();
const singleton = {};
class StatusRequest {
  constructor() { throw new Error('status request constructor is private'); }
}
StatusRequest.class = {};
StatusRequest.INSTANCE = singleton;
class Ping {
  constructor(time) { this.time = time; }
}
class PacketBufferFixture {
  constructor(packet) { this.packet = packet; }
  toPacket() { return { getTime: () => this.packet.time }; }
  getPacketId(clazz, protocol) {
    assert.equal(clazz, StatusRequest.class);
    assert.equal(protocol, 'STATUS');
    return 1;
  }
  static preferredProtocol(packet, current) {
    assert.equal(packet, singleton, 'status fixture must use the public singleton');
    assert.equal(current, null);
    return 'status';
  }
  getNetworkStateId(clazz) {
    assert.equal(clazz, StatusRequest.class);
    // End the probe after checking the singleton; remaining packet assertions need real Minecraft.
    throw new Error('status singleton probe completed');
  }
}
const hostType = harness.context.Java.type;
harness.context.Java.type = name => {
  if (name.endsWith('.ServerboundStatusRequestPacket')) return StatusRequest;
  if (name.endsWith('.ServerboundPingRequestPacket')) return Ping;
  return hostType(name);
};
harness.context.PacketBuffer = PacketBufferFixture;
let packetTest;
harness.context.run = (name, callback) => { packetTest = callback; };
vm.runInContext(source.slice(packetStart, packetEnd), harness.context);
assert.equal(typeof packetTest, 'function');
assert.throws(packetTest, /status singleton probe completed/);

// Explicit-phase bundle decoding can fail registration before reaching codec selection.
// The play-only legacy overload independently checks the missing decoder path.
const bundleChecks = source.split('\n').filter(line => line.includes('assertThrows') && line.includes('toPacket(BundlePacket.class'));
assert.equal(bundleChecks.length, 2, 'test both bundle decode paths');
for (const phaseError of ['Packet is not registered in play', 'Packet has no supported codec']) {
  harness = runner();
  harness.initialize();
  harness.context.helper = { toPacket: () => { throw new Error('java.lang.IllegalArgumentException: ' + phaseError); } };
  harness.context.legacy = { toPacket: () => { throw new Error('java.lang.IllegalArgumentException: Packet has no supported codec'); } };
  assert.doesNotThrow(() => vm.runInContext(bundleChecks.join('\n'), harness.context));
  harness.context.helper.toPacket = () => null;
  assert.throws(() => vm.runInContext(bundleChecks[0], harness.context), /Expected exception/);
  harness.context.legacy.toPacket = () => null;
  assert.throws(() => vm.runInContext(bundleChecks[1], harness.context), /Expected exception/);
}

console.log('PASS runner: logging/thread guards, class lookup after entity enumeration, status singleton and bundle rejection fixtures');
