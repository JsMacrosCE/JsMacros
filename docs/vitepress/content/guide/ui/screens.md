---
outline: deep
---
# Custom screens

A script screen combines the same drawing elements as a HUD overlay with
interactive widgets. Create one with `Hud.createScreen`, build its contents in an
init callback, then open it with `Hud.openScreen`.

```js
const screen = Hud.createScreen("my screen", true);

screen.setOnInit(JavaWrapper.methodToJava((self) => {
  self.addText("what should I do?", 10, 40, Color.rgb(0xFF, 0xFF, 0xFF), true);
  self.addButton(10, 20, 120, 20, "Say hello", JavaWrapper.methodToJava((button) => {
    Chat.say("hello"); // this sends a real chat message
  }));
}));

Hud.openScreen(screen);
```

::: warning
The button in this example sends a real message to the server. Change it to
`Chat.log("hello")` while testing if you do not want to send anything.
:::

The second argument to `createScreen` controls the darkened menu background. The
three-argument overload also controls whether the world behind the screen is
blurred.

## Build inside `setOnInit`

Opening a screen initializes it. Initialization clears elements that were added
outside the init callback, which is why adding text or buttons immediately after
`Hud.createScreen` and before `Hud.openScreen` does not work: they are gone by the
time the screen appears.

Put the elements in `setOnInit`, and they are rebuilt whenever the screen is opened
or `reloadScreen()` runs. The callback receives the screen, so use its parameter
(the `self` above) rather than the outer `screen` variable.

## Widgets

Inside the same init callback, add interactive widgets:

```js
screen.setOnInit(JavaWrapper.methodToJava((self) => {
  self.addButton(10, 20, 120, 20, "Press me", JavaWrapper.methodToJava((button) => {
    Chat.log("pressed");
  }));

  self.addTextInput(10, 50, 200, 20, "type a name", JavaWrapper.methodToJava((text) => {
    Chat.log(`you typed: ${text}`);
  }));

  self.addSlider(10, 80, 200, 20, "volume", 0.5, 10, JavaWrapper.methodToJava((slider) => {
    Chat.log(`slider is now ${slider.getValue()}`);
  }));
}));
Hud.openScreen(screen);
```

Widget callbacks are wrapped with `JavaWrapper.methodToJava` like any other Java
callback. They receive the widget helper, which exposes the current value and
allows the script to update the widget later.

## Closing

`screen.close()` closes the screen. `Hud.openScreen(null)` closes whatever screen is
currently open.

Set a close callback if the script needs to react:

```js
screen.setOnClose(JavaWrapper.methodToJava((closing) => {
  Chat.log("screen closed");
}));
```

`screen.shouldCloseOnEsc` controls whether Escape closes the screen;
`screen.shouldPause` controls whether the game pauses while it is open.

## Rebuilding

`screen.reloadScreen()` clears the screen's elements and reruns its init callback.
Use it when the window size changes or the script needs a fresh layout.

## Keep the script alive

Screen callbacks are `MethodWrapper`s, so a script with an open screen stays alive
while those callbacks exist. If the screen is temporary, close it and let the
callbacks go. If the script must stop immediately, call `JavaWrapper.stop()`.

## Related pages

- [HUD and Draw2D](/guide/ui/hud-and-draw2d)
- [Callbacks](/guide/concepts/callbacks)
- API reference: [`Hud`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FHud),
  [`Color`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FColor),
  [`IScreen`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/render/IScreen),
  [`ScriptScreen`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/render/ScriptScreen)
