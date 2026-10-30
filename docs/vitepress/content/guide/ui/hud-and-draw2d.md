---
outline: deep
---
# HUD and Draw2D

`Hud.createDraw2D()` creates a 2D overlay. Elements use GUI-scaled pixels from the
top-left of the window. The overlay renders only while it is registered.

The robust shape is to build the overlay in `setOnInit`; `register()` runs that
callback and clears the previous elements first, so the overlay can be rebuilt when
it is registered again.

```js
const draw = Hud.createDraw2D();

draw.setOnInit(JavaWrapper.methodToJava((self) => {
  self.addRect(10, 10, 210, 60, Color.argb(0x80, 0, 0, 0));
  self.addText("hello", 18, 22, Color.rgb(0xFF, 0xFF, 0xFF), true);
}));

draw.register();
```

If you build the overlay once and never register it again, adding elements directly
before `register()` also works; without an init callback the elements are preserved.
Custom screens are different: the game always clears their elements during
initialization, so [screen contents must be built in `setOnInit`](/guide/ui/screens).

`unregister()` stops rendering but keeps the elements. `clear()` removes them.

## Colors and coordinates

Use the `Color` library rather than writing raw ARGB integers:

```js
Color.rgb(0xFF, 0xFF, 0xFF);          // opaque white
Color.argb(0x80, 0, 0, 0);            // half-transparent black
Color.fromPacked(0x80000000);         // an existing packed value
Color.withAlpha(Color.rgb(255, 0, 0), 0x80); // same red, new alpha
```

`Color.rgb(red, green, blue)` builds an opaque color. `Color.argb(alpha, red,
green, blue)` takes all four components; each is `0`-`255`. `Color.fromPacked`
converts a 32-bit value such as `0x80000000` without JavaScript's signed-number
surprise.

One render rule is worth knowing: a zero alpha byte with nonzero RGB means "alpha
was omitted" and is made opaque. Use alpha `1` for a nearly transparent color.
Transparent black (`Color.argb(0, 0, 0, 0)`) remains fully transparent.

Coordinates are GUI-scaled pixels, not raw window pixels. On a high-DPI display or
a different GUI scale, the same coordinates land in a different physical place but
still line up with the rest of the HUD.

Mouse positions from `Hud.getMouseX()` and `Hud.getMouseY()` use the same space.

## Common elements

```js
const draw = Hud.createDraw2D();

draw.addText("status", 10, 10, Color.rgb(0xFF, 0xFF, 0xFF), true);
draw.addRect(10, 24, 200, 44, Color.argb(0xAA, 0, 0, 0));
draw.addLine(10, 24, 200, 24, Color.rgb(0, 255, 0));
draw.addItem(10, 50, "minecraft:diamond");

draw.register();
```

Each element has its own z-index and can be repositioned after it is added. Keep
the element handles if the script needs to update text or position later:

```js
const label = draw.addText("0", 10, 10, Color.rgb(0xFF, 0xFF, 0xFF), true);

JsMacros.on("Tick", JavaWrapper.methodToJava(() => {
  label.setText(`tick ${World.getTime()}`);
}));
```

## Draw3D

`Hud.createDraw3D()` renders in world space. Elements use block coordinates and are
drawn relative to the camera. Unlike `Draw2D`, it has no init callback; add the
elements, then register the overlay. Use `clear()` to remove them.

```js
const draw = Hud.createDraw3D();

draw.addBox(0, 64, 0, 1, 65, 1, Color.rgb(255, 0, 0), Color.argb(0x88, 0, 0, 0), true);
draw.addLine(0, 64, 0, 8, 72, 0, Color.rgb(0, 255, 0));

draw.register();
```

`unregister()` stops rendering without removing the elements; `clear()` removes
them. The box arguments are the two corners, the outline and fill colors, and
whether the box is filled.

## Lifecycle

An overlay with callbacks keeps the script context alive. If the overlay is
temporary, unregister it and clear it when the script is done:

```js
draw.unregister();
draw.clear();
```

If the script should stop entirely, `JavaWrapper.stop()` closes the context.
Closing releases event locks and interrupts the threads bound to the script.

## Performance

Every registered overlay renders on the client thread. Keep the element count
small, avoid rebuilding the overlay every tick, and update existing elements
instead of clearing and recreating them. A `setOnInit` callback is for rebuilds on
registration, not for per-frame work.

## Related pages

- [Custom screens](/guide/ui/screens)
- [Threading and the main thread](/guide/concepts/threading)
- API reference: [`Hud`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FHud),
  [`Color`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FColor),
  [`Draw2D`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/render/Draw2D),
  [`Draw3D`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/render/Draw3D)
