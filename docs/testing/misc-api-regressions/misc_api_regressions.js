/*
 * Run as a JavaScript macro on any supported Minecraft version.
 * Does not register drawings, send packets, or modify the world.
 * Join a world for registry-backed packet tests. Place a normal boat, raft,
 * chest boat and chest raft nearby for complete entity-dispatch coverage.
 * Missing prerequisites are reported as SKIP, not PASS. Run as NON-JOINED.
 * Covers safe local cases from the whole PR; see the coverage map and manual
 * scenarios in misc_api_regressions.md for UI, lifecycle and live-network cases.
 * Watchdog quarantine requires the separate manual check in
 * misc_api_regressions.md; this script deliberately does not stall the client.
 */

var System = Java.type('java.lang.System');
var Minecraft = Java.type('net.minecraft.client.Minecraft');
var core = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient').clientCore;
if (core.profile.checkJoinedThreadStack() || Minecraft.getInstance().isSameThread()) {
  var message = '[Misc API] Run this suite as a non-joined macro (disable Joined on the trigger), not on the client/render thread.';
  System.err.println(message);
  throw new Error(message);
}

var PacketBuffer = Java.type('com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper');
var RecvPacket = Java.type('com.jsmacrosce.jsmacros.client.api.event.impl.EventRecvPacket');
var SendPacket = Java.type('com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket');
var BundlePacket = Java.type('net.minecraft.network.protocol.game.ClientboundBundlePacket');
var SlotPacket = Java.type('net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket');
var FriendlyByteBuf = Java.type('net.minecraft.network.FriendlyByteBuf');
var RegistryFriendlyByteBuf = Java.type('net.minecraft.network.RegistryFriendlyByteBuf');
var Unpooled = Java.type('io.netty.buffer.Unpooled');
var Collections = Java.type('java.util.Collections');
var ItemStack = Java.type('net.minecraft.world.item.ItemStack');
var BoatHelper = Java.type('com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper');
var failures = 0;
var passes = 0;
var skips = 0;
var testOutput = [];
// Opt in only in a disposable profile. HTTP talks exclusively to the local fixture.
var TEST_FILES = false;
var TEST_HTTP = false;

function type(name) {
  return Java.type('com.jsmacrosce.jsmacros.' + name);
}

function assertThrows(action, text) {
  try {
    action();
  } catch (e) {
    if (String(e).indexOf(text) !== -1) return;
    throw new Error('Wrong exception, expected ' + text + ': ' + e);
  }
  throw new Error('Expected exception containing ' + text);
}

function near(actual, expected, message) {
  if (Math.abs(actual - expected) > 0.00001) {
    throw new Error(message + ': expected=' + expected + ', actual=' + actual);
  }
}

function field(object, name) {
  var member = object.getClass().getDeclaredField(name);
  member.setAccessible(true);
  return member.get(object);
}

function assertEq(actual, expected, message) {
  if (actual !== expected) {
    throw new Error(message + ': expected=' + expected + ', actual=' + actual);
  }
}

function run(name, test) {
  try {
    test();
  } catch (e) {
    failures++;
    logResult('[Misc API FAIL] ' + name + ': ' + e);
    return;
  }
  passes++;
  logResult('[Misc API PASS] ' + name);
}

function skip(name) {
  skips++;
  logResult('[Misc API SKIP] ' + name);
}

function logResult(message) {
  testOutput.push(message);
  // No GUI/font work on the script thread. Keep the original failure in latest.log.
  System.out.println(message);
}

function reportResults() {
  var reportError = null;
  try {
    Client.runOnMainThread(JavaWrapper.methodToJava(function () {
      // FClient logs callback exceptions instead of propagating them to its caller.
      try {
        if (!Minecraft.getInstance().isSameThread()) throw new Error('Chat reporting is not on the client thread');
        Chat.log(testOutput.join('\n'));
      } catch (e) {
        reportError = String(e);
      }
    }), true, 5000);
  } catch (e) {
    reportError = String(e);
  }
  if (reportError != null) {
    System.err.println('[Misc API REPORT ERROR] ' + reportError + '; results are preserved in latest.log');
  }
}

run('Line and builder preserve bounds and direction in all orientations', function () {
  var parent = Hud.createDraw2D();
  [10, 20].forEach(function (x1) {
    [10, 20].forEach(function (x2) {
      [10, 30].forEach(function (y1) {
        [10, 30].forEach(function (y2) {
          var builder = parent.lineBuilder(x1, y1, x2, y2);
          [builder, builder.build()].forEach(function (line) {
            line.moveTo(100, 100);
            assertEq(line.getScaledLeft(), 100, 'left');
            assertEq(line.getScaledTop(), 100, 'top');
            assertEq(line.getX2() - line.getX1(), x2 - x1, 'x direction');
            assertEq(line.getY2() - line.getY1(), y2 - y1, 'y direction');
            line.moveToX(200);
            assertEq(line.getScaledLeft(), 200, 'moveToX left');
            assertEq(line.getScaledTop(), 100, 'moveToX retains top');
            line.moveToY(300);
            assertEq(line.getScaledLeft(), 200, 'moveToY retains left');
            assertEq(line.getScaledTop(), 300, 'moveToY top');
          });
        });
      });
    });
  });
});

run('Child Draw2D dimensions follow its wrapper', function () {
  var parent = Hud.createDraw2D();
  var child = Hud.createDraw2D();
  var element = parent.draw2DBuilder(child).size(100, 50).build();
  assertEq(child.getWidth(), 100, 'configured width');
  assertEq(child.getHeight(), 50, 'configured height');
  element.setSize(120, 60);
  assertEq(child.getWidth(), 120, 'changed width');
  assertEq(child.getHeight(), 60, 'changed height');
});

run('Unsupported packet buffers can be checked before construction', function () {
  assertEq(PacketBuffer.canSerialize(null), false, 'null packet');
  var bundle = new BundlePacket(Collections.emptyList());
  assertEq(PacketBuffer.canSerialize(bundle), false, 'bundle packet');
  [new RecvPacket(bundle), new SendPacket(bundle)].forEach(function (event) {
    assertEq(event.canGetPacketBuffer(), false, 'event bundle buffer');
    var threw = false;
    try {
      event.getPacketBuffer();
    } catch (e) {
      threw = String(e).indexOf('IllegalArgumentException') !== -1;
    }
    assertEq(threw, true, 'documented unsupported-packet exception');
    event.packet = null;
    assertEq(event.canGetPacketBuffer(), false, 'event null packet');
  });
});

run('Plain caller-supplied buffers stay plain after reset', function () {
  var helper = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
  helper.writeInt(42).reset();
  assertEq(RegistryFriendlyByteBuf.class.isInstance(helper.getRaw()), false, 'plain subtype');
  assertEq(helper.getRaw().readableBytes(), 0, 'original empty contents');
});

var connection = Minecraft.getInstance().getConnection();
if (connection == null) {
  skip('Registry-backed packet reset: join a world first');
} else {
  run('Registry-backed packets decode after repeated reset', function () {
    var packet = new SlotPacket(0, 7, 2, ItemStack.EMPTY);
    assertEq(PacketBuffer.canSerialize(packet), true, 'slot packet support');
    [new RecvPacket(packet), new SendPacket(packet)].forEach(function (event) {
      assertEq(event.canGetPacketBuffer(), true, 'event slot buffer');
      var helper = event.getPacketBuffer();
      for (var i = 0; i < 3; i++) {
        helper.reset();
        assertEq(RegistryFriendlyByteBuf.class.isInstance(helper.getRaw()), true, 'registry subtype');
        assertEq(helper.getRaw().registryAccess(), connection.registryAccess(), 'registry access');
        var decoded = helper.toPacket();
        assertEq(decoded.getStateId(), 7, 'state id');
        assertEq(decoded.getSlot(), 2, 'slot');
      }
    });
  });
}

var scoreboards = World.getScoreboards();
if (scoreboards == null) {
  skip('Scoreboard index bounds: join a world first');
} else {
  run('Scoreboard index bounds reject overflow', function () {
    [-1, 16, 2147483645, 2147483646, 2147483647].forEach(function (index) {
      assertEq(scoreboards.getObjectiveForTeamColorIndex(index), null, 'invalid index ' + index);
    });
    scoreboards.getObjectiveForTeamColorIndex(0);
    scoreboards.getObjectiveForTeamColorIndex(15);
  });
}

var entities = World.getEntities();
var kinds = { boat: false, raft: false, chest_boat: false, chest_raft: false };
if (entities != null) {
  for (var i = 0; i < entities.size(); i++) {
    var entity = entities.get(i);
    var entityTypeId = String(entity.getType());
    var match = entityTypeId.match(/_(chest_raft|chest_boat|raft|boat)$/);
    if (match == null || entityTypeId.indexOf('minecraft:') !== 0) continue;
    var kind = match[1];
    kinds[kind] = true;
    run('Entity dispatch: ' + entityTypeId, function () {
      assertEq(BoatHelper.class.isInstance(entity), true, 'specialized helper');
      assertEq(entity.isChestBoat(), kind.indexOf('chest_') === 0, 'chest detection');
    });
  }
}
Object.keys(kinds).forEach(function (kind) {
  if (!kinds[kind]) skip('Entity dispatch: place a ' + kind + ' nearby');
});

run('Vec3D pitch and non-aliasing to3D copy', function () {
  var Vec = type('api.math.Vec3D');
  [[0, 1, 0, -90], [0, -1, 0, 90], [1, 0, 0, 0],
    [0, 0, 1, 0], [1, 1, 0, -45], [1, -1, 0, 45], [0, 0, 0, 0]]
    .forEach(function (v) {
      var original = new Vec(2, 3, 4, 2 + v[0], 3 + v[1], 4 + v[2]);
      near(original.getPitch(), v[3], 'pitch');
      var copy = original.to3D();
      assertEq(copy.z1, 4, 'copy start z');
      assertEq(copy.z2, original.z2, 'copy end z');
      copy.z2 = 99;
      assertEq(original.z2, 4 + v[2], 'independent copy');
    });
});

run('Draw2D addItem overloads return and retain elements', function () {
  var draw = Hud.createDraw2D();
  var stack = new (type('client.api.helper.inventory.ItemStackHelper'))(ItemStack.EMPTY);
  [draw.addItem(1, 2, 7, 'minecraft:stone'), draw.addItem(3, 4, 9, stack)]
    .forEach(function (item, i) {
      assertEq(item != null, true, 'non-null item');
      assertEq(item.getZIndex(), i === 0 ? 7 : 9, 'z index');
      assertEq(draw.getItems().contains(item), true, 'retained item');
    });
});

run('Draw2D init retains static elements and initializes nested children', function () {
  var parent = Hud.createDraw2D();
  var child = Hud.createDraw2D();
  var calls = 0;
  child.setOnInit(JavaWrapper.methodToJava(function (draw) {
    calls++;
    draw.addLine(0, 0, 5, 5, 0xffffff);
  }));
  var wrapper = parent.draw2DBuilder(child).size(10, 10).buildAndAdd();
  var staticLine = parent.addLine(0, 0, 10, 10, 0xffffff);
  parent.init();
  parent.init();
  assertEq(calls, 2, 'nested init without parent callback');
  assertEq(parent.getDraw2Ds().contains(wrapper), true, 'static child retained');
  assertEq(parent.getLines().contains(staticLine), true, 'static line retained');
  assertEq(child.getLines().size(), 1, 'callback clears its own old elements');
  parent.setOnInit(JavaWrapper.methodToJava(function (draw) {
    draw.addLine(1, 1, 2, 2, 0xffffff);
  }));
  parent.init();
  assertEq(parent.getLines().contains(staticLine), false, 'parent callback clears old elements');
  assertEq(parent.getLines().size(), 1, 'parent rebuilt');
});

run('Alignable percentages use both sides and reject malformed separators', function () {
  var draw = Hud.createDraw2D();
  var target = draw.lineBuilder(10, 20, 210, 120).build();
  [draw.lineBuilder(0, 0, 40, 20), draw.lineBuilder(40, 20, 0, 0).build()]
    .forEach(function (subject) {
      subject.alignHorizontally(target, '25%on75%', 3);
      assertEq(subject.getScaledLeft(), 153, 'horizontal percentages + offset');
      subject.alignVertically(target, '50%on25%', -2);
      assertEq(subject.getScaledTop(), 33, 'vertical percentages + offset');
      ['left', 'lefton', 'onright', 'leftonrightonleft', 'garbageonright']
        .forEach(function (bad) {
          assertThrows(function () { subject.alignHorizontally(target, bad, 0); }, 'IllegalArgumentException');
        });
      ['top', 'topon', 'onbottom', 'toponbottomontop', 'garbageonbottom']
        .forEach(function (bad) {
          assertThrows(function () { subject.alignVertically(target, bad, 0); }, 'IllegalArgumentException');
        });
    });
});

run('Render component constructors and setters wrap rotation', function () {
  var draw = Hud.createDraw2D();
  var stack = new (type('client.api.helper.inventory.ItemStackHelper'))(ItemStack.EMPTY);
  var image = new (type('client.api.classes.render.components.Image'))(
    0, 0, 16, 16, 0, 0xffffff, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 450);
  var item = new (type('client.api.classes.render.components.Item'))(0, 0, 0, stack, true, 1, 450);
  var child = draw.draw2DBuilder(Hud.createDraw2D()).rotation(450).build();
  [image, item, child].forEach(function (element) {
    assertEq(element.getRotation(), 90, 'constructor rotation');
  });
  [image, item, child, draw.lineBuilder(0, 0, 10, 10).build()].forEach(function (element) {
    element.setRotation(-450);
    assertEq(element.getRotation(), -90, 'setter rotation');
    element.setRotation(180);
    assertEq(element.getRotation(), -180, 'half turn');
  });
});

run('Surface builds copy position independently of builder and siblings', function () {
  var builder = Hud.createDraw3D().surfaceBuilder().pos(1, 2, 3);
  var first = builder.build();
  var second = builder.build();
  first.pos.x = 99;
  assertEq(second.pos.x, 1, 'sibling position');
  assertEq(builder.getPos().x, 1, 'builder position');
  builder.pos(4, 5, 6);
  assertEq(first.pos.y, 2, 'built position survives builder reuse');
});

run('Formatting modifiers have sentinel color; colors retain RGB', function () {
  var Formatting = type('client.api.helper.FormattingHelper');
  var values = Java.type('net.minecraft.ChatFormatting').values();
  for (var i = 0; i < values.length; i++) {
    var raw = values[i];
    var helper = new Formatting(raw);
    assertEq(helper.getColorValue(), raw.isColor() ? Number(raw.getColor()) : -1, String(raw));
    helper.toString();
  }
});

run('Permanent status effects reflect infinite duration', function () {
  var Helper = type('client.api.helper.StatusEffectHelper');
  var Effect = Java.type('net.minecraft.world.effect.MobEffectInstance');
  var speed = Java.type('net.minecraft.world.effect.MobEffects').SPEED;
  assertEq(new Helper(new Effect(speed, -1)).isPermanent(), true, 'infinite');
  assertEq(new Helper(new Effect(speed, 200)).isPermanent(), false, 'finite');
  assertEq(new Helper(new Effect(speed, 0)).isPermanent(), false, 'expired');
});

run('Non-repairable items return false instead of throwing', function () {
  var Items = Java.type('net.minecraft.world.item.Items');
  var ItemHelper = type('client.api.helper.inventory.ItemHelper');
  var StackHelper = type('client.api.helper.inventory.ItemStackHelper');
  assertEq(new ItemHelper(Items.STONE).canBeRepairedWith(new StackHelper(new ItemStack(Items.STONE))), false, 'no repair component');
});

run('Tooltip visibility queries inspect the individual component', function () {
  var Items = Java.type('net.minecraft.world.item.Items');
  var Creative = type('client.api.helper.inventory.CreativeItemStackHelper');
  var methods = [['hideEnchantments', 'areEnchantmentsHidden'], ['hideModifiers', 'areModifiersHidden'],
    ['hideUnbreakable', 'isUnbreakableHidden']];
  methods.forEach(function (selected) {
    var stack = new Creative(new ItemStack(Items.DIAMOND_SWORD));
    stack[selected[0]](true);
    methods.forEach(function (query) {
      assertEq(stack[query[1]](), query === selected, query[1] + ' isolated');
    });
    stack[selected[0]](false);
    assertEq(stack[selected[1]](), false, 'visible again');
  });
});

if (connection == null) {
  skip('Enchantment removal: join a world first');
} else {
  run('Enchantment removal persists and preserves unrelated enchantments', function () {
    var Items = Java.type('net.minecraft.world.item.Items');
    var stack = new (type('client.api.helper.inventory.CreativeItemStackHelper'))(new ItemStack(Items.DIAMOND_SWORD));
    stack.addEnchantment('minecraft:sharpness', 3).addEnchantment('minecraft:unbreaking', 2);
    stack.removeEnchantment('minecraft:sharpness');
    assertEq(stack.getEnchantment('minecraft:sharpness'), null, 'removed');
    assertEq(stack.getEnchantment('minecraft:unbreaking').getLevel(), 2, 'unrelated preserved');
    stack.removeEnchantment('minecraft:sharpness');
    assertEq(stack.getEnchantments().size(), 1, 'idempotent removal');
    stack.removeEnchantment(stack.getEnchantment('minecraft:unbreaking'));
    assertEq(stack.getEnchantments().size(), 0, 'helper overload');
  });
}

run('Coordinate suggestions accept whitespace and consistent 2D/3D inputs', function () {
  var Builder = Java.type('com.mojang.brigadier.suggestion.SuggestionsBuilder');
  var Helper = type('client.api.helper.SuggestionsBuilderHelper');
  var Arrays = Java.type('java.util.Arrays');
  [[' 1\t 2 ', '1 2'], [' 1  2\t3 ', '1 2 3']].forEach(function (fixture) {
    var raw = new Builder('', 0);
    new Helper(raw).suggestPositions(Arrays.asList(Java.to([fixture[0]], 'java.lang.String[]')));
    assertEq(raw.build().getList().stream().anyMatch(function (suggestion) {
      return String(suggestion.getText()) === fixture[1];
    }), true, 'normalized full coordinate suggestion');
  });
  [['1'], ['1 2 3 4'], ['1 2', '1 2 3'], ['']].forEach(function (bad) {
    assertThrows(function () {
      new Helper(new Builder('', 0)).suggestPositions(Arrays.asList(Java.to(bad, 'java.lang.String[]')));
    }, 'IllegalArgumentException');
  });
  var empty = new Builder('', 0);
  new Helper(empty).suggestPositions(Collections.emptyList());
  assertEq(empty.build().isEmpty(), true, 'empty list');
});

run('NumberCompareFilter >= equality and diagnostic for every numeric type', function () {
  var Filter = type('client.api.classes.worldscanner.filter.compare.NumberCompareFilter');
  ['Integer', 'Long', 'Byte', 'Short', 'Float', 'Double'].forEach(function (name) {
    var Box = Java.type('java.lang.' + name);
    var value = Box.valueOf('2');
    assertEq(new Filter('>=', value).apply(value), true, name + ' valid >=');
    assertThrows(function () { new Filter('=>', value).apply(value); }, '< > <= >= == !=');
  });
});

run('ClassWrapperFilter inherits methods and diagnoses unknown names', function () {
  var Wrapper = type('client.api.classes.worldscanner.filter.ClassWrapperFilter');
  var lookup = Wrapper.class.getDeclaredMethod('getPublicNoParameterMethods', Java.to([Java.type('java.lang.Class').class], 'java.lang.Class[]'));
  lookup.setAccessible(true);
  var methods = lookup.invoke(null, Java.to([Java.type('java.util.LinkedList').class], 'java.lang.Object[]'));
  assertEq(methods.containsKey('toString'), true, 'inherited AbstractCollection method');
  assertEq(methods.get('toString').getDeclaringClass(), Java.type('java.util.AbstractCollection').class, 'selected inherited declaration');
  assertEq(methods.containsKey('getClass'), false, 'Object methods excluded');
  var Filter = type('client.api.classes.worldscanner.filter.impl.BlockFilter');
  assertThrows(function () { new Filter('missingRegressionMethod', Java.to([], 'java.lang.Object[]'), Java.to([], 'java.lang.Object[]')); }, 'Unknown filter method: missingRegressionMethod');
  // Also initialize the real state-helper lookup; bridge collisions have a JVM fixture.
  new (type('client.api.classes.worldscanner.filter.impl.BlockStateFilter'))(
    'isAir', Java.to([], 'java.lang.Object[]'), Java.to([true], 'java.lang.Object[]'));
});

run('Scanner builder XOR variants implement exclusive-or truth tables', function () {
  var Builder = type('client.api.classes.worldscanner.WorldScannerBuilder');
  var Blocks = Java.type('net.minecraft.world.level.block.Blocks');
  var State = type('client.api.helper.world.BlockStateHelper');
  var Block = type('client.api.helper.world.BlockHelper');
  ['Block', 'State', 'StringBlock', 'StringState'].forEach(function (category) {
    [false, true].forEach(function (a) {
      [false, true].forEach(function (b) {
        var builder = new Builder();
        if (category.indexOf('String') === 0) {
          builder['with' + category + 'Filter']().contains(a ? 'air' : 'stone');
          builder['xor' + category + 'Filter']().contains(b ? 'air' : 'stone');
        } else {
          var method = category === 'State' ? 'isAir' : 'hasDynamicBounds';
          builder['with' + category + 'Filter'](method).is(category === 'State' ? a : !a);
          builder['xor' + category + 'Filter'](method).is(category === 'State' ? b : !b);
        }
        var stateCategory = category.indexOf('State') !== -1;
        var predicate = field(builder, stateCategory ? 'stateFilter' : 'blockFilter');
        var input = stateCategory ? new State(Blocks.AIR.defaultBlockState()) : new Block(Blocks.AIR);
        assertEq(predicate.apply(input), a !== b, category + ' XOR ' + a + ',' + b);
      });
    });
  });
});

run('Scanner callback overloads force sequential execution and protect pending filters', function () {
  var Builder = type('client.api.classes.worldscanner.WorldScannerBuilder');
  var IFilter = type('client.api.classes.worldscanner.filter.api.IFilter');
  var FilterAdapter = Java.extend(IFilter);
  var callback = new FilterAdapter({ apply: function () { return true; } });
  ['Block', 'State'].forEach(function (category) {
    var builder = new Builder();
    builder['with' + category + 'Filter'](callback);
    assertEq(field(builder.build(), 'useParallelStream'), false, category + ' sequential');
    builder['with' + category + 'Filter']('isAir');
    assertThrows(function () { builder['with' + category + 'Filter'](callback); }, 'IllegalStateException');
  });
});

run('Packet phase metadata, shared ambiguity, named decoding and unknown errors', function () {
  var Ping = Java.type('net.minecraft.network.protocol.ping.ServerboundPingRequestPacket');
  var StatusRequest = Java.type('net.minecraft.network.protocol.status.ServerboundStatusRequestPacket');
  var KeepAlive = Java.type('net.minecraft.network.protocol.common.ServerboundKeepAlivePacket');
  var Protocol = Java.type('net.minecraft.network.ConnectionProtocol');
  var helper = new PacketBuffer(new Ping(123), 'status');
  assertEq(helper.toPacket().getTime(), 123, 'status decode without connection phase');
  assertEq(helper.getPacketId(StatusRequest.class, 'STATUS') >= 0, true, 'case-insensitive phase');
  assertEq(PacketBuffer.preferredProtocol(StatusRequest.INSTANCE, null), 'status', 'unique phase');
  assertEq(helper.getNetworkStateId(StatusRequest.class), Protocol.STATUS.ordinal(), 'state ordinal');
  assertEq(helper.isServerbound(Ping.class), true, 'serverbound');
  assertEq(helper.isClientbound(SlotPacket.class), true, 'clientbound');
  assertEq(helper.getPacketId(KeepAlive.class, 'play') >= 0, true, 'shared play ID');
  assertEq(helper.getPacketId(KeepAlive.class, 'configuration') >= 0, true, 'shared config ID');
  assertEq(helper.getNetworkStateId(KeepAlive.class, 'configuration'), Protocol.CONFIGURATION.ordinal(), 'explicit state ID');
  [function () { helper.getPacketId(KeepAlive.class); },
    function () { helper.getNetworkStateId(KeepAlive.class); }, function () { PacketBuffer.preferredProtocol(new KeepAlive(1), null); }]
    .forEach(function (action) { assertThrows(action, 'multiple protocols'); });
  assertEq(PacketBuffer.preferredProtocol(new KeepAlive(1), Protocol.CONFIGURATION), 'configuration', 'shared phase selection');
  var named = new PacketBuffer(new Ping(123), 'status');
  named.reset();
  assertEq(named.toPacket('QueryPingC2SPacket').getTime(), 123, 'named decode');
  named.reset();
  assertEq(named.toPacket('QueryPingC2SPacket', 'status').getTime(), 123, 'named phase decode');
  var legacy = new PacketBuffer(new KeepAlive(123), 'play');
  assertEq(legacy.toPacket(KeepAlive.class).getId(), 123, 'registered legacy decoder');
  assertThrows(function () { helper.toPacket('missingRegressionPacket'); }, 'Unknown packet');
  assertThrows(function () { helper.toPacket('missingRegressionPacket', 'status'); }, 'Unknown packet');
  assertThrows(function () { helper.getPacketId(StatusRequest.class, 'play'); }, 'not registered');
  assertThrows(function () { helper.getPacketId(Ping.class, 'missing'); }, 'Unknown protocol');
  assertThrows(function () { helper.isClientbound(Java.type('java.lang.Object').class); }, 'Unknown packet');
  // Explicit-phase decoding validates registration before checking codec availability.
  assertThrows(function () { helper.toPacket(BundlePacket.class, 'play'); }, 'IllegalArgumentException');
  // The legacy play decoder lookup directly exercises the missing-codec rejection.
  assertThrows(function () { legacy.toPacket(BundlePacket.class); }, 'Packet has no supported codec');
  assertEq(new PacketBuffer().toPacket(), null, 'no originating packet');
});

run('Scoreboard nullable team/objective handling on a local empty scoreboard', function () {
  var helper = new (type('client.api.helper.world.ScoreboardsHelper'))(new (Java.type('net.minecraft.world.scores.Scoreboard'))());
  [-1, 19, 2147483647].forEach(function (slot) {
    assertEq(helper.getObjectiveSlot(slot), null, 'invalid slot');
  });
  assertEq(helper.getObjectiveSlot(0), null, 'empty valid slot');
  assertEq(helper.getPlayerTeam(), null, 'no local team or no player');
  assertEq(helper.getTeamColorFormatting(), null, 'no team formatting');
  assertEq(helper.getTeamColor(), -1, 'no team color');
  assertEq(helper.getTeamColorName(), null, 'no team color name');
  if (Player.getPlayer() != null) assertEq(helper.getPlayerTeam(Player.getPlayer()), null, 'explicit unteamed player');
  assertEq(helper.getCurrentScoreboard(), null, 'no current objective');
  assertEq(String(helper.toString()).indexOf('null') !== -1, true, 'null-safe toString');
});

run('Unannotated BaseEvent defaults and event registration', function () {
  var core = type('client.JsMacrosClient').clientCore;
  var base = new (type('core.event.BaseEvent'))(core);
  assertEq(base.cancellable(), false, 'default cancellation');
  assertEq(base.joinable(), false, 'default joining');
  assertEq(base.getEventName(), 'BaseEvent', 'fallback name');
  assertEq(core.eventRegistry.events.contains('WrappedScript'), true, 'WrappedScript registered');
  assertEq(core.eventRegistry.events.contains('CommandContext'), true, 'CommandContext registered');
});

run('EventContainer null-thread diagnostics and repeated release', function () {
  var context = JavaWrapper.methodToJava(function () {}).getCtx();
  var container = new (type('core.language.EventContainer'))(context);
  container.toString();
  container.releaseLock();
  container.releaseLock();
  assertEq(container.isLocked(), false, 'released');
});

['on', 'once'].forEach(function (mode) {
  run('Cancellable custom event runs same-context ' + mode + ' listener inline', function () {
    var core = type('client.JsMacrosClient').clientCore;
    var name = 'MiscApiRegression_' + Java.type('java.util.UUID').randomUUID();
    var custom = JsMacros.createCustomEvent(name);
    custom.cancelable = true;
    custom.registerEvent();
    var calls = 0;
    var listener = null;
    try {
      listener = JsMacros[mode](name, false, JavaWrapper.methodToJava(function (event) {
        calls++;
        event.cancel();
      }));
      custom.trigger();
      assertEq(custom.isCanceled(), true, 'cancellation before dispatch returns');
      assertEq(calls, 1, 'one callback');
      custom.trigger();
      assertEq(calls, mode === 'once' ? 1 : 2, 'listener lifetime');
    } finally {
      if (listener != null) JsMacros.off(listener);
      core.eventRegistry.events.remove(name);
    }
  });
});

['java', 'guest', 'builder', 'callback'].forEach(function (mode) {
  run('ClassBuilder static initializer via ' + mode, function () {
    var Builder = type('core.library.impl.classes.ClassBuilder');
    var name = 'MiscApiClinit_' + String(Java.type('java.util.UUID').randomUUID()).replace(/-/g, '');
    var builder = new Builder(name, Java.type('java.lang.Object').class, Java.to([], 'java.lang.Class[]'));
    var calls = 0;
    var guest = JavaWrapper.methodToJava(function (receiver, args) {
      assertEq(receiver, null, 'static initializer has no receiver');
      assertEq(args.length, 0, 'no parameters');
      calls++;
      return null;
    });
    var wrapperKey = name + ';<clinit>()V' + (mode === 'builder' ? ': 0' : '');
    try {
      builder.addField('public static int marker;');
      var initializer = builder.addClinit();
      if (mode === 'java') initializer.body('{ marker = 42; }');
      if (mode === 'guest') initializer.guestBody(guest);
      if (mode === 'builder') initializer.buildBody().appendGuestCode(guest, '', null).appendJavaCode('marker = 42').finish();
      if (mode === 'callback') initializer.body(JavaWrapper.methodToJava(function (clazz, behavior) {
        assertEq(behavior.isClassInitializer(), true, 'class initializer behavior');
        behavior.setBody('{ marker = 42; }');
      }));
      var clazz = builder.finishBuildAndFreeze();
      Java.type('java.lang.Class').forName(clazz.getName(), true, clazz.getClassLoader());
      assertEq(mode === 'guest' || mode === 'builder' ? calls : clazz.getField('marker').getInt(null),
        mode === 'guest' || mode === 'builder' ? 1 : 42, 'initializer executed');
      if (mode === 'builder') assertEq(clazz.getField('marker').getInt(null), 42, 'Java + guest body');
    } finally {
      if (Builder.methodWrappers.get(wrapperKey) === guest) Builder.methodWrappers.remove(wrapperKey, guest);
    }
  });
});

if (!TEST_FILES) {
  skip('FileHandler full byte read: enable TEST_FILES for temporary file I/O');
} else {
  run('FileHandler readBytes returns complete empty and multi-megabyte files', function () {
    var Files = Java.type('java.nio.file.Files');
    var Arrays = Java.type('java.util.Arrays');
    var file = Files.createTempFile('jsmacros-misc-', '.bin');
    try {
      var helper = new (type('core.library.impl.classes.FileHandler'))(file.toFile());
      assertEq(helper.readBytes().length, 0, 'empty');
      var data = new (Java.type('byte[]'))(2 * 1024 * 1024 + 7);
      for (var i = 0; i < data.length; i++) data[i] = (i % 256) - 128;
      helper.write(data);
      assertEq(Arrays.equals(helper.readBytes(), data), true, 'all bytes preserved');
    } finally {
      Files.deleteIfExists(file);
      assertEq(Files.exists(file), false, 'temporary file removed');
    }
  });
}

if (!TEST_HTTP) {
  skip('HTTP timeout overload matrix: start misc_api_http_fixture.py and enable TEST_HTTP');
} else {
  var Request = type('core.library.impl.classes.HTTPRequest');
  var body = Java.to([65, 66, 67], 'byte[]');
  var overloads = [
    ['get', function (r) { return r.get(); }],
    ['post string', function (r) { return r.post('ABC'); }],
    ['post bytes', function (r) { return r.post(body); }],
    ['put string', function (r) { return r.put('ABC'); }],
    ['put bytes', function (r) { return r.put(body); }],
    ['send method', function (r) { return r.send('GET'); }],
    ['send string', function (r) { return r.send('POST', 'ABC'); }],
    ['send bytes', function (r) { return r.send('PUT', body); }]
  ];
  overloads.forEach(function (entry) {
    run('HTTP configured timeout and normal response: ' + entry[0], function () {
      var request = new Request('http://127.0.0.1:18729/ok').setConnectTimeout(400).setReadTimeout(500);
      var method = Request.class.getDeclaredMethod('openConnection', Java.to([], 'java.lang.Class[]'));
      method.setAccessible(true);
      var raw = method.invoke(request, Java.to([], 'java.lang.Object[]'));
      assertEq(raw.getConnectTimeout(), 400, 'connect timeout applied');
      assertEq(raw.getReadTimeout(), 500, 'read timeout applied');
      raw.disconnect();
      var response = entry[1](request);
      try {
        assertEq(response.responseCode, 200, 'normal response');
        assertEq(String(response.text()).indexOf('misc-api-fixture') !== -1, true, 'fixture response');
      } finally {
        field(response, 'raw').close();
      }
    });
    run('HTTP delayed response times out: ' + entry[0], function () {
      var control = entry[1](new Request('http://127.0.0.1:18729/slow').setConnectTimeout(2000).setReadTimeout(4000));
      try {
        assertEq(control.responseCode, 200, 'same delayed endpoint succeeds with longer read deadline');
      } finally {
        field(control, 'raw').close();
      }
      var request = new Request('http://127.0.0.1:18729/slow').setConnectTimeout(400).setReadTimeout(100);
      var start = Java.type('java.lang.System').nanoTime();
      assertThrows(function () { entry[1](request); }, 'Read timed out');
      var elapsed = Number(Java.type('java.lang.System').nanoTime() - start) / 1000000;
      assertEq(elapsed >= 50 && elapsed < 1500, true, 'configured read deadline elapsed=' + elapsed);
    });
  });
}

logResult('[Misc API] ' + passes + ' passed, ' + failures + ' failed, ' + skips + ' skipped; manual scenarios are separate');
reportResults();
if (failures !== 0) throw new Error('Misc API regressions: ' + failures + ' failures');
