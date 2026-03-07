/*
 * draw3d_visual_tests.js
 *
 * Interactive Draw3D visual test suite.
 * Use chat:
 *   !pass
 *   !fail <reason>
 */

var PREFIX = '[Draw3D Visual Tests] ';
var Pos3D = Java.type('com.jsmacrosce.jsmacros.api.math.Pos3D');

var results = [];
var draw = Hud.createDraw3D();
var summaryPassed = 0;
var summaryIndex = 0;

function log(msg) {
  Chat.log(PREFIX + msg);
}

function sleep(ms) {
  Time.sleep(ms);
}

function nowPlayer() {
  var p = Player.getPlayer();
  if (p == null)
    throw new Error(
      'No player found. Join a world before running visual tests.'
    );
  return p;
}

function posFromPlayer(offsetX, offsetY, offsetZ) {
  var p = nowPlayer();
  return {
    x: p.getX() + offsetX,
    y: p.getY() + offsetY,
    z: p.getZ() + offsetZ
  };
}

function listSize(list) {
  if (list == null) return -1;
  if (typeof list.size === 'function') return list.size();
  if (typeof list.length === 'number') return list.length;
  return -1;
}

function waitForDecision() {
  var ev;
  var message;
  var trimmed;
  var reason;
  while (true) {
    ev = JsMacros.waitForEvent('SendMessage');
    if (ev == null || ev.event == null) continue;
    message = ev.event.message;
    if (message == null) continue;
    trimmed = String(message).trim();

    if (trimmed.toLowerCase() === '!pass') {
      return { passed: true, reason: '' };
    }
    if (trimmed.toLowerCase() === '!retry') {
      return { retry: true };
    }
    if (trimmed.toLowerCase().indexOf('!fail') === 0) {
      reason =
        trimmed.length > 5 ? trimmed.substring(5).trim() : 'No reason provided';
      return { passed: false, reason: reason };
    }
  }
}

function runVisual(id, name, instructions, setupFn, postAssertFn) {
  var decision;
  var postError;
  var reason;
  postError = '';

  draw.register();
  while (true) {
    draw.clear();
    setupFn();

    log(`--- ${id} ${name} ---`);
    log(instructions);
    log('Type !pass, !fail <reason>, or !retry in chat.');

    decision = waitForDecision();
    if (decision.retry) {
      log(`[RETRY] ${id} ${name}`);
      continue;
    }
    break;
  }

  if (decision.passed && typeof postAssertFn === 'function') {
    try {
      postAssertFn();
    } catch (e) {
      decision.passed = false;
      postError = String(e);
    }
  }

  if (decision.passed) {
    results.push({ id: id, name: name, pass: true, reason: '' });
    log(`[PASS] ${id} ${name}`);
  } else {
    reason = decision.reason;
    if (postError !== '') reason = `${reason} | post-assert: ${postError}`;
    results.push({ id: id, name: name, pass: false, reason: reason });
    log(`[FAIL] ${id} ${name} -> ${reason}`);
  }
}

function nearestNonPlayerEntity(radius) {
  var p = nowPlayer();
  var entities = World.getEntities(radius);
  var best = null;
  var bestDist = 1e300;
  var i;
  var e;
  var dx;
  var dy;
  var dz;
  var dist;
  var myUuid = p.getUUID();

  if (entities == null) return null;
  for (i = 0; i < listSize(entities); i++) {
    e = entities.get(i);
    if (e == null) continue;
    if (String(e.getUUID()) === String(myUuid)) continue;
    dx = e.getX() - p.getX();
    dy = e.getY() - p.getY();
    dz = e.getZ() - p.getZ();
    dist = dx * dx + dy * dy + dz * dz;
    if (dist < bestDist) {
      bestDist = dist;
      best = e;
    }
  }
  return best;
}

function summonEntityNearPlayer() {
  Chat.say('/summon zombie ~ ~ ~5');
  sleep(700);
  return nearestNonPlayerEntity(16);
}

function ensureRegistered() {
  draw.register();
}

function ensureCleanup() {
  try {
    draw.clear();
  } catch (_) {}
  try {
    draw.unregister();
  } catch (_) {}
}

try {
  log('=== Starting Draw3D visual tests ===');
  log('This suite is interactive.');
  log('When prompted, type !pass or !fail <reason>.');

  ensureRegistered();
  if (
    typeof event !== 'undefined' &&
    event != null &&
    typeof event.unregisterOnStop === 'function'
  ) {
    event.unregisterOnStop(false, draw);
  }

  runVisual(
    'V01',
    'register_shows',
    'A semi-transparent filled red box should appear at your feet.',
    () => {
      var p = posFromPlayer(0, 0, 0);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xff0000,
        255,
        0xff0000,
        102,
        true,
        false
      );
      draw.register();
    }
  );

  runVisual(
    'V02',
    'unregister_hides',
    'The red box should disappear after unregister.',
    () => {
      var p = posFromPlayer(0, 0, 0);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xff0000,
        255,
        0xff0000,
        102,
        true,
        false
      );
      draw.register();
      sleep(200);
      draw.unregister();
    }
  );

  runVisual(
    'V03',
    'reregister_shows',
    'The same red box should reappear after re-register.',
    () => {
      var p = posFromPlayer(0, 0, 0);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xff0000,
        255,
        0xff0000,
        102,
        true,
        false
      );
      draw.register();
    }
  );

  runVisual(
    'V04',
    'box_wireframe_only',
    'A 2x2x2 red wireframe box should appear 3 blocks in front of you (no fill, see-through).',
    () => {
      var p = posFromPlayer(0, 0, 3);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 2,
        p.y + 2,
        p.z + 2,
        0xff0000,
        255,
        0x000000,
        0,
        false,
        false
      );
    }
  );

  runVisual(
    'V05',
    'box_filled',
    'The same box should now have semi-transparent blue fill and red outline.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 2,
        p.y + 2,
        p.z + 2,
        0xff0000,
        255,
        0x0000ff,
        102,
        true,
        false
      );
    }
  );

  runVisual(
    'V06',
    'box_cull_true',
    'A half-buried green box with cull=true should have its underground portion hidden.',
    () => {
      var p = posFromPlayer(2, -1, 3);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 2,
        p.z + 1,
        0x00ff00,
        255,
        0x00ff00,
        68,
        true,
        true
      );
    }
  );

  runVisual(
    'V07',
    'box_cull_false',
    'Now with cull=false, the entire half-buried box should be visible through terrain.',
    () => {
      var p = posFromPlayer(2, -1, 3);
      draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 2,
        p.z + 1,
        0x00ff00,
        255,
        0x00ff00,
        68,
        true,
        false
      );
    }
  );

  runVisual(
    'V08',
    'box_setPos_move',
    'A yellow box should move 4 blocks to your right after about 1 second.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      var box = draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xffff00,
        255,
        0xffff00,
        68,
        true,
        false
      );
      sleep(1000);
      box.setPos(p.x + 4, p.y, p.z, p.x + 5, p.y + 1, p.z + 1);
    }
  );

  runVisual(
    'V09',
    'box_setColor_green',
    'A box should switch from red to green.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      var box = draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xff0000,
        255,
        0xff0000,
        68,
        true,
        false
      );
      sleep(800);
      box.setColor(0x00ff00, 255);
      box.setFillColor(0x00ff00, 68);
    }
  );

  runVisual(
    'V10',
    'box_setAlpha_fade',
    'A cyan box should become much more transparent after about 1 second.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      var box = draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0x00ffff,
        255,
        0x00ffff,
        102,
        true,
        false
      );
      sleep(1000);
      box.setAlpha(50);
      box.setFillAlpha(30);
    }
  );

  runVisual(
    'V11',
    'box_setFill_toggle',
    'A magenta box should show fill first, then lose fill and keep only outline.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      var box = draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xff00ff,
        255,
        0xff00ff,
        102,
        true,
        false
      );
      sleep(1000);
      box.setFill(false);
    }
  );

  runVisual(
    'V12',
    'box_setPosToBlock',
    'A white box should exactly outline one whole block 3 blocks ahead.',
    () => {
      var p = nowPlayer();
      var x = Math.floor(p.getX());
      var y = Math.floor(p.getY());
      var z = Math.floor(p.getZ()) + 3;
      var box = draw.addBox(
        0,
        0,
        0,
        1,
        1,
        1,
        0xffffff,
        255,
        0xffffff,
        17,
        false,
        false
      );
      box.setPosToBlock(x, y, z);
    }
  );

  runVisual(
    'V13',
    'box_removeBox',
    'A box should appear, then disappear after about 1 second.',
    () => {
      var p = posFromPlayer(0, 0, 3);
      var box = draw.addBox(
        p.x,
        p.y,
        p.z,
        p.x + 1,
        p.y + 1,
        p.z + 1,
        0xffffff,
        255,
        0xffffff,
        51,
        true,
        false
      );
      sleep(1000);
      draw.removeBox(box);
    },
    () => {
      if (listSize(draw.getBoxes()) !== 0)
        throw new Error('Expected zero boxes after removeBox');
    }
  );

  runVisual(
    'V14',
    'boxBuilder_forBlock',
    'An orange block highlight should appear 2 blocks to your right (built with builder).',
    () => {
      var p = nowPlayer();
      var x = Math.floor(p.getX()) + 2;
      var y = Math.floor(p.getY());
      var z = Math.floor(p.getZ());
      draw
        .boxBuilder(x, y, z)
        .color(0xffaa00)
        .fillColor(0xffaa00, 85)
        .fill(true)
        .buildAndAdd();
    }
  );

  runVisual(
    'V15',
    'line_basic',
    'A white line should appear 5 blocks long in front of you.',
    () => {
      var p = posFromPlayer(0, 1.2, 2);
      draw.addLine(p.x, p.y, p.z, p.x + 5, p.y, p.z, 0xffffff, 255);
    }
  );

  runVisual(
    'V16',
    'line_alpha',
    'A similar white line should appear but be noticeably transparent.',
    () => {
      var p = posFromPlayer(0, 1.2, 2);
      draw.addLine(p.x, p.y, p.z, p.x + 5, p.y, p.z, 0xffffff, 80, false);
    }
  );

  runVisual(
    'V17',
    'line_setPos_move',
    'A green line should rotate/shift into a diagonal after about 1 second.',
    () => {
      var p = posFromPlayer(0, 1.2, 2);
      var line = draw.addLine(p.x, p.y, p.z, p.x + 5, p.y, p.z, 0x00ff00);
      sleep(1000);
      line.setPos(p.x, p.y, p.z, p.x + 3, p.y + 2, p.z + 3);
    }
  );

  runVisual(
    'V18',
    'line_setColor',
    'A line should switch from red to cyan.',
    () => {
      var p = posFromPlayer(0, 1.2, 2);
      var line = draw.addLine(p.x, p.y, p.z, p.x + 4, p.y, p.z + 1, 0xff0000);
      sleep(900);
      line.setColor(0x00ffff, 255);
    }
  );

  runVisual(
    'V19',
    'trace_basic',
    'A magenta trace line should run from your current view origin toward a point 3 blocks ahead.',
    () => {
      var p = posFromPlayer(0, 1.5, 3);
      draw.addTraceLine(p.x, p.y, p.z, 0xff00ff, 255);
    }
  );

  runVisual(
    'V20',
    'trace_dynamic_start',
    'Move your camera around: trace line start should stay attached to your view origin (small bobbing offset is acceptable).',
    () => {
      var p = posFromPlayer(0, 1.5, 4);
      draw.addTraceLine(p.x, p.y, p.z, 0x00ffff, 255);
    }
  );

  runVisual(
    'V21',
    'trace_setPos',
    'A trace line target should jump from right side to left side after 1 second.',
    () => {
      var p1 = posFromPlayer(3, 1.5, 3);
      var p2 = posFromPlayer(-3, 1.5, 3);
      var tl = draw.addTraceLine(p1.x, p1.y, p1.z, 0xffff00, 255);
      sleep(1000);
      tl.setPos(p2.x, p2.y, p2.z);
    }
  );

  runVisual(
    'V22',
    'trace_builder',
    'A light-red trace line should appear using the builder API.',
    () => {
      var p = posFromPlayer(2, 1.5, 4);
      draw
        .traceLineBuilder()
        .pos(new Pos3D(p.x, p.y, p.z))
        .color(255, 128, 128, 255)
        .buildAndAdd();
    }
  );

  runVisual(
    'V23',
    'entity_trace_summon',
    'A zombie should be summoned ahead and a red entity trace line should point to it.',
    () => {
      var e = summonEntityNearPlayer();
      if (e == null)
        throw new Error(
          'Could not find summoned entity. Check command permissions.'
        );
      draw.addEntityTraceLine(e, 0xff0000, 255);
    }
  );

  runVisual(
    'V24',
    'entity_trace_yOffset',
    "A green entity trace line should target above the entity's head (yOffset 2.5).",
    () => {
      var e = nearestNonPlayerEntity(16);
      if (e == null) throw new Error('No nearby non-player entity found.');
      draw.addEntityTraceLine(e, 0x00ff00, 255, 2.5);
    }
  );

  runVisual(
    'V25',
    'entity_trace_auto_remove_on_kill',
    'The entity trace line should disappear after the nearest non-player entity is killed.',
    () => {
      var e = nearestNonPlayerEntity(16);
      if (e == null)
        throw new Error('No nearby non-player entity to test with.');
      draw.addEntityTraceLine(e, 0xffff00, 255);
      sleep(800);
      Chat.say(`/kill ${e.getUUID()}`);
      sleep(900);
    }
  );

  runVisual(
    'V26',
    'point_small',
    'A tiny fully solid white cube point should appear 3 blocks ahead, depth-tested (hidden behind blocks, not under clouds).',
    () => {
      var p = posFromPlayer(0, 1, 3);
      draw.addPoint(p.x, p.y, p.z, 0.15, 0xffffff);
    }
  );

  runVisual(
    'V27',
    'point_unit_cube',
    'A fully solid blue point cube should appear exactly 1x1x1 in size, depth-tested (hidden behind blocks, not under clouds).',
    () => {
      var p = posFromPlayer(0, 1, 3);
      draw.addPoint(p.x, p.y, p.z, 0.5, 0x0000ff);
    }
  );

  runVisual(
    'V28',
    'surface_basic_text',
    "A floating 3D surface with small text 'Hello 3D!' should appear in front of you.",
    () => {
      var p = posFromPlayer(0, 2.0, 3);
      var s = draw.addDraw2D(p.x, p.y, p.z, 2, 1);
      s.addRect(0, 0, s.getWidth(), s.getHeight(), 0x000000, 0x55);
      s.addText('Hello 3D!', 10, 10, 0xffffffff | 0, false);
    }
  );

  runVisual(
    'V29',
    'surface_rotated',
    'A surface should be rotated about 45 degrees on Y axis.',
    () => {
      var p = posFromPlayer(0, 2.0, 3);
      var s = draw.addDraw2D(p.x, p.y, p.z, 0, 45, 0, 2, 1);
      s.addRect(0, 0, s.getWidth(), s.getHeight(), 0x2244aa, 0x55);
      s.addText('Rotated', 8, 8, 0xffffffff | 0, false);
    }
  );

  runVisual(
    'V30',
    'surface_rotateToPlayer',
    'Walk around (including very close): this surface should stay upright and keep facing you from its own position.',
    () => {
      var p = posFromPlayer(0, 2.0, 3);
      var s = draw.addDraw2D(p.x, p.y, p.z, 2, 1);
      s.setRotateToPlayer(true);
      s.setRotateCenter(true);
      s.addRect(0, 0, s.getWidth(), s.getHeight(), 0x22aa22, 0x55);
      s.addText('Faces You', 8, 8, 0xffffffff | 0, false);
    }
  );

  runVisual(
    'V31',
    'surface_mutation',
    'After 1s the surface should resize dramatically in-place; after another 1s it should move right and keep rendering.',
    () => {
      var p = posFromPlayer(0, 2.0, 3);
      var s = draw.addDraw2D(p.x, p.y, p.z, 1.2, 0.8);
      var rect = s.addRect(0, 0, s.getWidth(), s.getHeight(), 0xaa2222, 0x55);
      var label = s.addText('Before', 8, 8, 0xffffffff | 0, false);
      sleep(1000);
      s.setSizes(4.0, 2.2);
      s.removeRect(rect);
      s.removeText(label);
      s.addRect(0, 0, s.getWidth(), s.getHeight(), 0xaa2222, 0x55);
      s.addText('Resized', 8, 8, 0xffffffff | 0, false);
      sleep(1000);
      s.setPos(p.x + 2, p.y, p.z);
    }
  );

  runVisual(
    'V32',
    'surface_light_mode',
    'Stand in a dark area (cave/shadow): WORLD/BRIGHT text should differ (world dimmer). Rect backdrops may look similar on 1.21.8. Marker points show anchors.',
    () => {
      var p = posFromPlayer(0, 2.0, 3);
      var s1 = draw.addDraw2D(p.x - 1.2, p.y, p.z, 1.2, 0.8);
      s1.setWorldLight();
      s1.cull = true;
      s1.addRect(0, 0, s1.getWidth(), s1.getHeight(), 0x0000ff, 0x66, 0, -5);
      s1.addText('WORLD', 6, 6, 0xffffffff | 0, 10, false);
      draw.addPoint(p.x - 1.2, p.y, p.z, 0.05, 0x00ffff, 255, true);

      var s2 = draw.addDraw2D(p.x + 1.2, p.y, p.z, 1.2, 0.8);
      s2.setFullBrightLight();
      s2.cull = true;
      s2.addRect(0, 0, s2.getWidth(), s2.getHeight(), 0xffaa00, 0x66, 0, -5);
      s2.addText('BRIGHT', 6, 6, 0xffffffff | 0, 10, false);
      draw.addPoint(p.x + 1.2, p.y, p.z, 0.05, 0xffffff, 255, true);
    }
  );

  log('=== Finished Draw3D visual tests ===');
} catch (fatal) {
  log(`FATAL: ${String(fatal)}`);
} finally {
  ensureCleanup();

  summaryPassed = 0;
  for (summaryIndex = 0; summaryIndex < results.length; summaryIndex++) {
    if (results[summaryIndex].pass) summaryPassed++;
  }

  log(`Summary: ${summaryPassed}/${results.length} passed`);
  if (summaryPassed !== results.length) {
    for (summaryIndex = 0; summaryIndex < results.length; summaryIndex++) {
      if (!results[summaryIndex].pass) {
        log(
          ' - ' +
            results[summaryIndex].id +
            ' ' +
            results[summaryIndex].name +
            ' :: ' +
            results[summaryIndex].reason
        );
      }
    }
  }
}
