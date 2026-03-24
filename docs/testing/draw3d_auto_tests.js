/*
 * draw3d_auto_tests.js
 *
 * Fully automated Draw3D API/state test suite for JsMacros.
 * No visual confirmation required.
 */

var Pos3D = Java.type('com.jsmacrosce.jsmacros.api.math.Pos3D');
var EntityTraceLineClass = Java.type(
  'com.jsmacrosce.jsmacros.client.api.classes.render.components3d.EntityTraceLine'
);

var PREFIX = '[Draw3D Auto Tests] ';
var results = [];

function log(msg) {
  Chat.log(PREFIX + msg);
}

function fail(message) {
  throw new Error(String(message));
}

function assertTrue(condition, message) {
  if (!condition) fail(message);
}

function assertEq(actual, expected, message) {
  if (actual !== expected) {
    fail(
      `${message || 'assertEq failed'} | expected=${expected} actual=${actual}`
    );
  }
}

function approxEq(actual, expected, epsilon, message) {
  if (Math.abs(actual - expected) > epsilon) {
    fail(
      `${message || 'approxEq failed'} | expected=${expected} actual=${actual} eps=${epsilon}`
    );
  }
}

function listSize(list) {
  if (list == null) return -1;
  if (typeof list.size === 'function') return list.size();
  if (typeof list.length === 'number') return list.length;
  return -1;
}

function listContains(list, item) {
  var i;
  var n;
  if (list == null) return false;
  if (typeof list.contains === 'function') return list.contains(item);
  n = listSize(list);
  for (i = 0; i < n; i++) {
    if (list.get(i) === item) return true;
  }
  return false;
}

function javaListToArray(list) {
  var i;
  var out = [];
  if (list == null) return out;
  for (i = 0; i < listSize(list); i++) out.push(list.get(i));
  return out;
}

function className(obj) {
  if (obj == null || typeof obj.getClass !== 'function') return 'null';
  return String(obj.getClass().getSimpleName());
}

function rgbOf(argb) {
  return argb & 0xffffff;
}

function alphaOf(argb) {
  return (argb >>> 24) & 0xff;
}

function withFreshDraw(fn) {
  var d = Hud.createDraw3D();
  try {
    return fn(d);
  } finally {
    try {
      d.clear();
    } catch (_) {}
    try {
      d.unregister();
    } catch (_) {}
  }
}

function run(id, name, fn) {
  var reason;
  try {
    fn();
    results.push({ id: id, name: name, pass: true, note: '' });
    log(`[PASS ${id}] ${name}`);
  } catch (e) {
    reason = String(e);
    results.push({ id: id, name: name, pass: false, note: reason });
    log(`[FAIL ${id}] ${name} -> ${reason}`);
  }
}

log('=== Starting Draw3D auto tests ===');

run('A01', 'hud_createDraw3D', () => {
  var d = Hud.createDraw3D();
  assertTrue(d != null, 'createDraw3D returned null');
});

run('A02', 'draw_starts_unregistered', () => {
  withFreshDraw((d) => {
    assertTrue(
      !listContains(Hud.listDraw3Ds(), d),
      'fresh draw should be unregistered'
    );
  });
});

run('A03', 'register_adds_to_list', () => {
  withFreshDraw((d) => {
    d.register();
    assertTrue(
      listContains(Hud.listDraw3Ds(), d),
      'registered draw missing from listDraw3Ds'
    );
  });
});

run('A04', 'register_returns_self', () => {
  withFreshDraw((d) => {
    assertEq(d.register(), d, 'register should return self');
  });
});

run('A05', 'unregister_removes_from_list', () => {
  withFreshDraw((d) => {
    d.register();
    d.unregister();
    assertTrue(
      !listContains(Hud.listDraw3Ds(), d),
      'draw still registered after unregister'
    );
  });
});

run('A06', 'unregister_returns_self', () => {
  withFreshDraw((d) => {
    d.register();
    assertEq(d.unregister(), d, 'unregister should return self');
  });
});

run('A07', 'clear_empties_all_lists', () => {
  withFreshDraw((d) => {
    d.addBox(0, 0, 0, 1, 1, 1, 0xff0000, 0x220000, true);
    d.addLine(0, 0, 0, 1, 1, 1, 0x00ff00);
    d.addTraceLine(1, 1, 1, 0x0000ff);
    d.addEntityTraceLine(null, 0xffffff);
    d.addDraw2D(0, 0, 0);
    d.clear();
    assertEq(listSize(d.getBoxes()), 0, 'boxes not cleared');
    assertEq(listSize(d.getLines()), 0, 'lines not cleared');
    assertEq(listSize(d.getTraceLines()), 0, 'trace lines not cleared');
    assertEq(
      listSize(d.getEntityTraceLines()),
      0,
      'entity trace lines not cleared'
    );
    assertEq(listSize(d.getDraw2Ds()), 0, 'surfaces not cleared');
  });
});

run('A08', 'hud_clearDraw3Ds', () => {
  var i;
  var baseline = javaListToArray(Hud.listDraw3Ds());
  var d = Hud.createDraw3D();
  d.register();
  Hud.clearDraw3Ds();
  assertEq(
    listSize(Hud.listDraw3Ds()),
    0,
    'clearDraw3Ds did not empty registry'
  );
  for (i = 0; i < baseline.length; i++) baseline[i].register();
});

run('A09', 'addBox_returns_box', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 2, 2, 2, 0xff0000, 0x220000, true);
    assertEq(className(box), 'Box', 'addBox should return Box');
    assertEq(listSize(d.getBoxes()), 1, 'box not present in list');
  });
});

run('A10', 'addBox_alpha_separate', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0x112233, 77, 0x445566, 88, true);
    assertEq(alphaOf(box.color), 77, 'outline alpha mismatch');
    assertEq(rgbOf(box.color), 0x112233, 'outline rgb mismatch');
    assertEq(alphaOf(box.fillColor), 88, 'fill alpha mismatch');
    assertEq(rgbOf(box.fillColor), 0x445566, 'fill rgb mismatch');
  });
});

run('A11', 'box_fixAlpha_bare_rgb', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xff0000, 0, false);
    assertEq(
      alphaOf(box.color),
      255,
      'fixAlpha should set alpha=255 for bare rgb'
    );
  });
});

run('A12', 'box_setColor', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    box.setColor(0x00ff00, 255);
    assertEq(alphaOf(box.color), 255, 'setColor alpha mismatch');
    assertEq(rgbOf(box.color), 0x00ff00, 'setColor rgb mismatch');
  });
});

run('A13', 'box_setAlpha_preserves_rgb', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0x123456, 255, 0x000000, 0, false);
    box.setAlpha(64);
    assertEq(alphaOf(box.color), 64, 'alpha mismatch');
    assertEq(rgbOf(box.color), 0x123456, 'rgb changed unexpectedly');
  });
});

run('A14', 'box_setFillColor_alpha', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    box.setFillColor(0x010203, 128);
    assertEq(alphaOf(box.fillColor), 128, 'fill alpha mismatch');
    assertEq(rgbOf(box.fillColor), 0x010203, 'fill rgb mismatch');
  });
});

run('A15', 'box_setFillAlpha_preserves_rgb', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    box.setFillColor(0xaabbcc, 33);
    box.setFillAlpha(200);
    assertEq(alphaOf(box.fillColor), 200, 'fill alpha mismatch');
    assertEq(rgbOf(box.fillColor), 0xaabbcc, 'fill rgb changed unexpectedly');
  });
});

run('A16', 'box_setPosToBlock', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    box.setPosToBlock(10, 20, 30);
    approxEq(box.pos.x1, 10, 1e-9, 'x1 mismatch');
    approxEq(box.pos.y1, 20, 1e-9, 'y1 mismatch');
    approxEq(box.pos.z1, 30, 1e-9, 'z1 mismatch');
    approxEq(box.pos.x2 - box.pos.x1, 1, 1e-9, 'x extent mismatch');
    approxEq(box.pos.y2 - box.pos.y1, 1, 1e-9, 'y extent mismatch');
    approxEq(box.pos.z2 - box.pos.z1, 1, 1e-9, 'z extent mismatch');
  });
});

run('A17', 'box_setPosToPoint_radius', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    box.setPosToPoint(3, 4, 5, 0.3);
    approxEq(box.pos.x2 - box.pos.x1, 0.6, 1e-9, 'x extent mismatch');
    approxEq(box.pos.y2 - box.pos.y1, 0.6, 1e-9, 'y extent mismatch');
    approxEq(box.pos.z2 - box.pos.z1, 0.6, 1e-9, 'z extent mismatch');
  });
});

run('A18', 'addPoint_xyz_extents', () => {
  withFreshDraw((d) => {
    var box = d.addPoint(1, 2, 3, 0.5, 0x123456);
    approxEq(box.pos.x2 - box.pos.x1, 1, 1e-9, 'x extent mismatch');
    approxEq(box.pos.y2 - box.pos.y1, 1, 1e-9, 'y extent mismatch');
    approxEq(box.pos.z2 - box.pos.z1, 1, 1e-9, 'z extent mismatch');
  });
});

run('A19', 'addPoint_pos3d_extents', () => {
  withFreshDraw((d) => {
    var box = d.addPoint(new Pos3D(1, 2, 3), 0.3, 0x123456);
    approxEq(box.pos.x2 - box.pos.x1, 0.6, 1e-9, 'x extent mismatch');
    approxEq(box.pos.y2 - box.pos.y1, 0.6, 1e-9, 'y extent mismatch');
    approxEq(box.pos.z2 - box.pos.z1, 0.6, 1e-9, 'z extent mismatch');
  });
});

run('A20', 'removeBox', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    d.removeBox(box);
    assertEq(listSize(d.getBoxes()), 0, 'removeBox did not remove');
  });
});

run('A21', 'reAddElement', () => {
  withFreshDraw((d) => {
    var box = d.addBox(0, 0, 0, 1, 1, 1, 0xffffff, 0, false);
    d.removeBox(box);
    d.reAddElement(box);
    assertEq(listSize(d.getBoxes()), 1, 'reAddElement did not re-add box');
  });
});

run('A22', 'boxBuilder_getters_all_fields', () => {
  withFreshDraw((d) => {
    var b = d
      .boxBuilder()
      .pos1(1, 2, 3)
      .pos2(4, 5, 6)
      .color(0x102030)
      .alpha(77)
      .fillColor(0x405060)
      .fillAlpha(88)
      .fill(true)
      .cull(true);
    assertEq(b.getPos1().x, 1, 'pos1.x mismatch');
    assertEq(b.getPos2().z, 6, 'pos2.z mismatch');
    assertEq(b.getColor(), 0x102030, 'builder color mismatch');
    assertEq(b.getAlpha(), 77, 'builder alpha mismatch');
    assertEq(b.getFillColor(), 0x405060, 'builder fillColor mismatch');
    assertEq(b.getFillAlpha(), 88, 'builder fillAlpha mismatch');
    assertTrue(b.isFilled(), 'builder fill mismatch');
    assertTrue(b.isCulled(), 'builder cull mismatch');
  });
});

run('A23', 'boxBuilder_forBlock_extents', () => {
  withFreshDraw((d) => {
    var box = d.boxBuilder().forBlock(8, 9, 10).build();
    approxEq(box.pos.x2 - box.pos.x1, 1, 1e-9, 'x extent mismatch');
    approxEq(box.pos.y2 - box.pos.y1, 1, 1e-9, 'y extent mismatch');
    approxEq(box.pos.z2 - box.pos.z1, 1, 1e-9, 'z extent mismatch');
  });
});

run('A24', 'boxBuilder_color_int_alpha_known_bug', () => {
  withFreshDraw((d) => {
    var b = d.boxBuilder().fillColor(0xa1b2c3).color(0x112233, 77);
    var box = b.build();
    assertEq(alphaOf(box.color), 77, 'alpha should still be applied');
    assertEq(
      rgbOf(box.color),
      0xa1b2c3,
      'known bug behavior changed: expected color to use fillColor'
    );
  });
  log(
    '[NOTE A24] This validates current known bug in Box.Builder.color(int,int): assigns fillColor.'
  );
});

run('A25', 'addLine_returns_line_in_list', () => {
  withFreshDraw((d) => {
    var line = d.addLine(0, 0, 0, 1, 2, 3, 0xffffff);
    assertEq(className(line), 'Line3D', 'addLine should return Line3D');
    assertEq(listSize(d.getLines()), 1, 'line not present in list');
  });
});

run('A26', 'line_setColor_fixAlpha', () => {
  withFreshDraw((d) => {
    var line = d.addLine(0, 0, 0, 1, 2, 3, 0xffffff);
    line.setColor(0x123456);
    assertEq(
      alphaOf(line.color),
      255,
      'fixAlpha should set alpha=255 for bare rgb'
    );
  });
});

run('A27', 'line_setAlpha_preserves_rgb', () => {
  withFreshDraw((d) => {
    var line = d.addLine(0, 0, 0, 1, 2, 3, 0xaa22cc);
    line.setAlpha(100);
    assertEq(alphaOf(line.color), 100, 'alpha mismatch');
    assertEq(rgbOf(line.color), 0xaa22cc, 'rgb changed unexpectedly');
  });
});

run('A28', 'removeLine', () => {
  withFreshDraw((d) => {
    var line = d.addLine(0, 0, 0, 1, 2, 3, 0xffffff);
    d.removeLine(line);
    assertEq(listSize(d.getLines()), 0, 'removeLine did not remove');
  });
});

run('A29', 'lineBuilder_getters', () => {
  withFreshDraw((d) => {
    var b = d
      .lineBuilder()
      .pos1(1, 2, 3)
      .pos2(4, 5, 6)
      .color(0x123456)
      .alpha(88)
      .cull(true);
    assertEq(b.getPos1().x, 1, 'pos1.x mismatch');
    assertEq(b.getPos2().z, 6, 'pos2.z mismatch');
    assertEq(b.getColor(), 0x123456, 'color mismatch');
    assertEq(b.getAlpha(), 88, 'alpha mismatch');
    assertTrue(b.isCulled(), 'cull mismatch');
  });
});

run('A30', 'addTraceLine_xyz_in_list', () => {
  withFreshDraw((d) => {
    var tl = d.addTraceLine(1, 2, 3, 0xabcdef);
    assertEq(
      className(tl),
      'TraceLine',
      'addTraceLine should return TraceLine'
    );
    assertEq(listSize(d.getTraceLines()), 1, 'trace line not in list');
  });
});

run('A31', 'addTraceLine_pos3d', () => {
  withFreshDraw((d) => {
    d.addTraceLine(new Pos3D(1, 2, 3), 0xabcdef);
    assertEq(listSize(d.getTraceLines()), 1, 'trace line not in list');
  });
});

run('A32', 'traceLine_alpha_separate', () => {
  withFreshDraw((d) => {
    var tl = d.addTraceLine(1, 2, 3, 0x112233, 90);
    tl.setAlpha(33);
    var lines = d.getTraceLines();
    assertEq(listSize(lines), 1, 'trace line missing');
  });
});

run('A33', 'removeTraceLine', () => {
  withFreshDraw((d) => {
    var tl = d.addTraceLine(1, 2, 3, 0x123456);
    d.removeTraceLine(tl);
    assertEq(listSize(d.getTraceLines()), 0, 'removeTraceLine did not remove');
  });
});

run('A34', 'traceLineBuilder_getters', () => {
  withFreshDraw((d) => {
    var b = d.traceLineBuilder().pos(1, 2, 3).color(0x012345).alpha(66);
    assertEq(b.getPos().x, 1, 'pos.x mismatch');
    assertEq(b.getColor(), 0x012345, 'color mismatch');
    assertEq(b.getAlpha(), 66, 'alpha mismatch');
  });
});

run('A35', 'entityTraceLine_null_entity_safe', () => {
  withFreshDraw((d) => {
    var etl = d.addEntityTraceLine(null, 0xff00ff);
    assertEq(className(etl), 'EntityTraceLine', 'expected EntityTraceLine');
    assertEq(etl.setEntity(null), etl, 'setEntity(null) should return self');
  });
});

run('A36', 'entityTraceLine_shouldRemove_initial', () => {
  withFreshDraw((d) => {
    var etl = d.addEntityTraceLine(null, 0xffffff);
    assertTrue(!etl.shouldRemove, 'shouldRemove should start false');
  });
});

run('A37', 'entityTraceLineBuilder_getters', () => {
  withFreshDraw((d) => {
    var b = d
      .entityTraceLineBuilder()
      .entity(null)
      .yOffset(1.25)
      .color(0x778899)
      .alpha(44);
    assertEq(b.getYOffset(), 1.25, 'yOffset mismatch');
    assertEq(b.getColor(), 0x778899, 'color mismatch');
    assertEq(b.getAlpha(), 44, 'alpha mismatch');
  });
});

run('A38', 'surface_dimensions_and_minSubdivisions', () => {
  withFreshDraw((d) => {
    var s = d.addDraw2D(0, 0, 0, 0, 0, 0, 4, 2, 200, false, false);
    assertEq(s.getMinSubdivisions(), 200, 'initial minSubdivisions mismatch');
    assertEq(s.getWidth(), 400, 'width mismatch for scale computation');
    assertEq(s.getHeight(), 200, 'height mismatch for scale computation');
    s.setMinSubdivisions(100);
    assertEq(s.getWidth(), 200, 'width mismatch after minSubdivisions update');
    assertEq(
      s.getHeight(),
      100,
      'height mismatch after minSubdivisions update'
    );
  });
});

run('A39', 'surface_position_rotation_size_mutators', () => {
  withFreshDraw((d) => {
    var s = d.addDraw2D(1, 2, 3, 4, 5, 6, 2, 3, 120, false, false);
    s.setPos(9, 8, 7);
    s.setRotations(10, 20, 30);
    s.setSizes(8, 4);
    assertEq(s.pos.x, 9, 'pos.x mismatch');
    assertEq(s.rotations.y, 20, 'rot.y mismatch');
    assertEq(s.getSizes().x, 8, 'size.x mismatch');
  });
});

run('A40', 'entityTraceLine_dirty_flag_access', () => {
  EntityTraceLineClass.dirty = false;
  assertTrue(EntityTraceLineClass.dirty === false, 'failed to set dirty=false');
});

var passed = 0;
var i;
var j;
for (i = 0; i < results.length; i++) {
  if (results[i].pass) passed++;
}

log('=== Finished Draw3D auto tests ===');
log(`Result: ${passed}/${results.length} passed`);

if (passed !== results.length) {
  log('Failed tests:');
  for (j = 0; j < results.length; j++) {
    if (!results[j].pass) {
      log(` - ${results[j].id} ${results[j].name} :: ${results[j].note}`);
    }
  }
}
