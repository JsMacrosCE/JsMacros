/*
 * JsMacrosCE manual client suite
 *
 * Test modes:
 *   auto    - programmatic checks only. Run headlessly from chat/menu; no page.
 *   screen  - needs the suite screen open for input (typing, clicking). The page
 *             carries Passed/Failed/Previous/Next/Back controls.
 *   observe - needs the world visible. The runner closes the suite screen,
 *             prints instructions plus clickable Pass/Fail/Skip/Retry/Menu in
 *             chat, and keeps a small HUD hint while the test runs.
 *   event   - reserved for future listener-based tests; use prepare/verify the
 *             same way as observe and register JsMacros.on(...) in prepare.
 *
 * Chat clickables require the chat screen open (press T). Press H (or click
 * [Menu]) to reopen the browse screen. The script runs a tick loop so the
 * context stays alive while no screen is open.
 */

const wrap = callback => JavaWrapper.methodToJava(callback);

const session = {
    queue: [],
    results: {},
    decision: null,
    quit: false,
    hostScreen: null,
    hostCaptureError: null,
    keyListener: null,
    hint: null,
    overlays: []
};

function onMain(task) {
    Client.runOnMainThread(wrap(task), true, 120000);
}

function request(type, payload) {
    session.queue.push({ type: type, payload: payload });
}

function decide(value) {
    session.decision = value;
}

function trackOverlay(overlay) {
    session.overlays.push(overlay);
}

function untrackOverlay(overlay) {
    const index = session.overlays.indexOf(overlay);
    if (index >= 0) {
        session.overlays.splice(index, 1);
    }
}

// TextBuilder.withColor(int) takes a ChatFormatting id, not RGB. Use the
// three-argument overload so the harness colors are actual RGB values.
function applyRgb(builder, color) {
    const value = color == null ? 0xFFFFFF : color;
    builder.withColor((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF);
}

function chatLine(segments) {
    const builder = Chat.createTextBuilder();
    for (const segment of segments) {
        const text = typeof segment === "string" ? segment : segment.text;
        const color = typeof segment === "string" ? 0xFFFFFF : segment.color;
        builder.append(text);
        applyRgb(builder, color);
        if (typeof segment !== "string" && segment.action != null) {
            builder.withCustomClickEvent(wrap(segment.action));
        }
    }
    Chat.log(builder.build());
}

function chatText(text, color) {
    const builder = Chat.createTextBuilder();
    builder.append(text);
    applyRgb(builder, color);
    Chat.log(builder.build());
}

function postLines(lines) {
    for (const line of lines) {
        chatText(line, 0xFFFFFF);
    }
}

function postPanel(entries) {
    const segments = [{ text: "[JsMacrosCE] ", color: 0x55FFFF }];
    for (const entry of entries) {
        segments.push({ text: "[" + entry.label + "]", color: entry.color, action: entry.action });
        segments.push({ text: " ", color: 0xFFFFFF });
    }
    chatLine(segments);
}

function plan() {
    return [
        { label: "Run All", color: 0xFFFFFF, action: () => request("run-all") },
        { label: "Automated", color: 0x55FF55, action: () => request("auto") },
        { label: "Visual", color: 0x55FFFF, action: () => request("observe-all") },
        { label: "Screen", color: 0xFFFF55, action: () => request("screen-all") },
        { label: "Menu", color: 0xAAAAAA, action: () => request("menu") },
        { label: "Results", color: 0xFFAA55, action: () => request("results") }
    ];
}

function postDecisionLine(test) {
    chatLine([
        { text: test.id + " running. Verdict: ", color: 0xFFFFFF },
        { label: "Pass", text: "[Pass]", color: 0x55FF55, action: () => decide("pass") },
        { text: " ", color: 0xFFFFFF },
        { text: "[Fail]", color: 0xFF5555, action: () => decide("fail") },
        { text: " ", color: 0xFFFFFF },
        { text: "[Skip]", color: 0xFFFF55, action: () => decide("skip") },
        { text: " ", color: 0xFFFFFF },
        { text: "[Retry]", color: 0x55FFFF, action: () => decide("retry") },
        { text: " ", color: 0xFFFFFF },
        // Open the menu directly: a queued request would not run until this
        // test's wait loop ends.
        { text: "[Menu]", color: 0xAAAAAA, action: () => openMenu() }
    ]);
}

/* ------------------------------------------------------------------ */
/* helpers                                                            */
/* ------------------------------------------------------------------ */

function check(state, passed, label) {
    const ok = !!passed;
    state.checks.push({ passed: ok, label: label });
    // Screen tests register a live status Text so the count is visible after a
    // Check button is pressed (init-time text alone never refreshed).
    if (state.statusText != null) {
        try {
            state.statusText.setText("Checks: " + automaticCount(state));
        } catch (ignored) {
        }
    }
    return ok;
}

function automaticCount(state) {
    return state.checks.filter(check => check.passed).length + "/" + state.checks.length;
}

function listSize(list) {
    return list == null ? 0 : list.size();
}

function alphaOf(argb) {
    return (argb >>> 24) & 0xFF;
}

function rgbOf(argb) {
    return argb & 0xFFFFFF;
}

function approx(actual, expected) {
    return Math.abs(actual - expected) < 1e-9;
}

function sameObject(a, b) {
    return a === b || (a != null && b != null && a.equals(b));
}

function forwardPosition(player, distance, yOffset) {
    const yaw = player.getYaw() * Math.PI / 180;
    const pitch = player.getPitch() * Math.PI / 180;
    const fx = -Math.sin(yaw) * Math.cos(pitch);
    const fy = -Math.sin(pitch);
    const fz = Math.cos(yaw) * Math.cos(pitch);
    const eye = player.getEyePos();
    return { x: eye.x + fx * distance, y: eye.y + fy * distance + yOffset, z: eye.z + fz * distance };
}

// Re-anchors a surface in front of the player's eyes. Called every tick so the
// surface stays head-locked (position and rotation) while the camera moves.
function updateHeadLockedSurface(surface, player) {
    const yaw = player.getYaw() * Math.PI / 180;
    const pitch = player.getPitch() * Math.PI / 180;
    const fx = -Math.sin(yaw) * Math.cos(pitch);
    const fy = -Math.sin(pitch);
    const fz = Math.cos(yaw) * Math.cos(pitch);
    const eye = player.getEyePos();
    const distance = 0.75;
    const width = 1.3;
    const height = 0.35;
    surface.setPos(eye.x + fx * distance - width / 2, eye.y + fy * distance - 0.08 - height / 2, eye.z + fz * distance);
    surface.setRotations(-player.getPitch(), 180 + player.getYaw(), 0);
}

function nearestNonPlayerEntity(player, radius) {
    if (player == null) {
        return null;
    }
    const entities = World.getEntities(radius);
    const self = String(player.getUUID());
    let nearest = null;
    let nearestDistance = Infinity;
    for (let i = 0; i < listSize(entities); i++) {
        const candidate = entities.get(i);
        if (candidate == null || String(candidate.getUUID()) === self || !candidate.isAlive()) {
            continue;
        }
        const dx = candidate.getX() - player.getX();
        const dy = candidate.getY() - player.getY();
        const dz = candidate.getZ() - player.getZ();
        const distance = dx * dx + dy * dy + dz * dz;
        if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = candidate;
        }
    }
    return nearest;
}

function entityAlive(entity) {
    if (entity == null) {
        return false;
    }
    try {
        return entity.isAlive();
    } catch (ignored) {
        return false;
    }
}

function withDraw(fn) {
    const draw = Hud.createDraw3D();
    try {
        return fn(draw);
    } finally {
        try {
            draw.clear();
        } catch (ignored) {
        }
        try {
            draw.unregister();
        } catch (ignored) {
        }
    }
}

function centeredButton(screen, y, label, callback) {
    const width = Math.min(260, screen.getWidth() - 48);
    screen.addButton(Math.floor((screen.getWidth() - width) / 2), y, width, 20, label, wrap(callback));
}

function screenCleanup(state) {
    if (state.cleanup != null) {
        try {
            state.cleanup();
        } catch (error) {
            Chat.log("Manual suite cleanup failed: " + error);
        }
        state.cleanup = null;
    }
}

/* ------------------------------------------------------------------ */
/* tests                                                              */
/* ------------------------------------------------------------------ */

const TESTS = [
    {
        id: "SCREEN-OPEN-001",
        category: "Screen",
        mode: "screen",
        title: "Screen Basics",
        setup(screen, state) {
            check(state, String(screen.getScreenClassName()) === "ScriptScreen", "Script screen class is exposed");
            check(state, String(screen.getTitleText().getString()) === "Screen Basics", "Screen title is exposed");
            check(state, screen.getWidth() > 0 && screen.getHeight() > 0, "Screen dimensions are available");
            screen.textBuilder("This suite requests a menu background: dirt on older targets and a blurred menu background on 26.1+.")
                .pos(0, 46).color(0xFFFFFF).shadow(false).alignHorizontally("center").buildAndAdd();
            screen.addText("The status below reports automatic checks for screen metadata.", 24, 72, 0xFFFFFF, false);
        }
    },
    {
        id: "SCREEN-LIFECYCLE-001",
        category: "Screen",
        mode: "screen",
        title: "Screen Lifecycle",
        setup(screen, state) {
            state.initCount = (state.initCount || 0) + 1;
            check(state, state.initCount >= 1, "Initialization callback ran");
            screen.addText("Initialization count: " + state.initCount, 24, 46, 0xFFFFFF, false);
            screen.addText("Reload rebuilds this screen. The count must increase without duplicate controls.", 24, 66, 0xFFFFFF, false);
            screen.addButton(24, 92, 160, 20, "Reload Screen", wrap(() => screen.reloadScreen()));
        }
    },
    {
        id: "SCREEN-FAILURE-001",
        category: "Screen",
        mode: "screen",
        title: "Init Failure Recovery",
        setup(screen, state) {
            check(state, state.failureHandled !== false, "Failure recovery is ready");
            screen.addText("Trigger a controlled init failure. This test must return here with a recorded recovery.", 24, 46, 0xFFFFFF, false);
            screen.addText("Recovery count: " + (state.failureCount || 0), 24, 66, 0xFFFFFF, false);
            screen.addButton(24, 92, 200, 20, "Trigger Controlled Failure", wrap(() => {
                const failing = Hud.createScreen("Init Failure", true);
                failing.setOnFailInit(wrap(() => {
                    state.failureHandled = true;
                    state.failureCount = (state.failureCount || 0) + 1;
                    Hud.openScreen(screen);
                }));
                failing.setOnInit(wrap(() => {
                    throw new Error("Manual suite controlled init failure");
                }));
                Hud.openScreen(failing);
            }));
            if (state.failureHandled) {
                check(state, true, "Failure callback returned to the test");
            }
        }
    },
    {
        id: "SCREEN-CLOSE-001",
        category: "Screen",
        mode: "screen",
        title: "Close and Parent",
        setup(screen, state) {
            screen.addText("Open a child screen, then close it. It must return here and increment the callback count.", 24, 46, 0xFFFFFF, false);
            screen.addText("Child close callbacks: " + (state.closeCount || 0), 24, 66, 0xFFFFFF, false);
            screen.addButton(24, 92, 160, 20, "Open Child", wrap(() => {
                const child = Hud.createScreen("Child Screen", true);
                child.setParent(screen);
                child.setOnClose(wrap(() => {
                    state.closeCount = (state.closeCount || 0) + 1;
                }));
                child.setOnInit(wrap(() => {
                    child.addText("Close this screen to return to Close and Parent.", 24, 46, 0xFFFFFF, false);
                    child.addButton(24, 72, 120, 20, "Close", wrap(() => child.close()));
                }));
                Hud.openScreen(child);
            }));
            if (state.closeCount > 0) {
                check(state, true, "Child close callback ran before returning to its parent");
            }
        }
    },
    {
        id: "SCREEN-ESC-001",
        category: "Screen",
        mode: "screen",
        title: "Escape and Pause",
        setup(screen, state) {
            screen.drawTitle = false;
            screen.shouldCloseOnEsc = false;
            screen.shouldPause = false;
            screen.addText("No title should appear above this line.", 24, 46, 0xFFFFFF, false);
            screen.addText("Press Esc: this screen must remain open. In a world, it must not pause the game.", 24, 66, 0xFFFFFF, false);
            screen.addText("Use Back or Mark Passed when the check is complete.", 24, 86, 0xFFFFFF, false);
            check(state, !screen.drawTitle && !screen.shouldCloseOnEsc && !screen.shouldPause, "Screen flags accepted their configured values");
        }
    },
    {
        id: "SCREEN-INPUT-001",
        category: "Input",
        mode: "screen",
        title: "Screen Input",
        setup(screen, state) {
            const status = screen.addText("Interact with this screen to record input callbacks.", 24, 46, 0xFFFFFF, false);
            state.mouseDown = 0;
            state.mouseUp = 0;
            state.drag = 0;
            state.scroll = 0;
            state.key = 0;
            state.character = 0;
            const update = message => status.setText(message);
            screen.setOnMouseDown(wrap((pos, button) => {
                state.mouseDown++;
                update("Mouse down: " + state.mouseDown + " at " + Math.floor(pos.getX()) + ", " + Math.floor(pos.getY()) + " button " + button);
            }));
            screen.setOnMouseUp(wrap((pos, button) => {
                state.mouseUp++;
                update("Mouse up: " + state.mouseUp + " at " + Math.floor(pos.getX()) + ", " + Math.floor(pos.getY()) + " button " + button);
            }));
            screen.setOnMouseDrag(wrap((vec, button) => {
                state.drag++;
                update("Mouse drag: " + state.drag + " button " + button + " delta " + Math.floor(vec.getDeltaX()) + ", " + Math.floor(vec.getDeltaY()));
            }));
            screen.setOnScroll(wrap((pos, delta) => {
                state.scroll++;
                update("Scroll: " + state.scroll + " delta " + delta.getX() + ", " + delta.getY());
            }));
            screen.setOnKeyPressed(wrap((key, modifiers) => {
                state.key++;
                update("Key press: " + state.key + " key " + key + " modifiers " + modifiers);
            }));
            screen.setOnCharTyped(wrap((character, modifiers) => {
                state.character++;
                update("Character: " + state.character + " '" + character + "' modifiers " + modifiers);
            }));
            screen.addText("Click, drag, scroll, press a key, and type a character. Each action updates the line above.", 24, 72, 0xFFFFFF, false);
        }
    },
    {
        id: "SCREEN-INPUT-002",
        category: "Input",
        mode: "screen",
        title: "Input Modifiers",
        setup(screen, state) {
            state.keyModifiers = 0;
            state.charModifiers = 0;
            state.plainCharSeen = false;
            const status = screen.addText("Hold Shift/Ctrl/Alt/Super and press a key, then type a plain and a Shift character.", 24, 46, 0xFFFFFF, false);
            const updateStatus = () => {
                const seen = state.keyModifiers | state.charModifiers;
                status.setText("key mods " + state.keyModifiers + " | char mods " + state.charModifiers
                    + "   (shift=" + ((seen & 1) !== 0 ? "yes" : "no")
                    + ", ctrl=" + ((seen & 2) !== 0 ? "yes" : "no")
                    + ", alt=" + ((seen & 4) !== 0 ? "yes" : "no")
                    + ", super=" + ((seen & 8) !== 0 ? "yes" : "no") + ")");
            };
            // Key events always carry the full GLFW modifier set, including
            // Ctrl/Alt, so they are the reliable path to assert.
            screen.setOnKeyPressed(wrap((key, modifiers) => {
                state.keyModifiers |= modifiers;
                updateStatus();
            }));
            // Char events are only delivered for plain/Shift/Super on many
            // platforms, so this path is covered but not required for Ctrl/Alt.
            screen.setOnCharTyped(wrap((character, modifiers) => {
                state.charModifiers |= modifiers;
                if (modifiers === 0) {
                    state.plainCharSeen = true;
                }
                updateStatus();
            }));
            screen.addButton(24, 78, 200, 20, "Check Modifiers", wrap(() => {
                check(state, (state.keyModifiers & 1) !== 0, "Shift observed on a key press");
                check(state, (state.keyModifiers & 2) !== 0, "Ctrl observed on a key press");
                check(state, (state.keyModifiers & 4) !== 0, "Alt observed on a key press");
                check(state, (state.keyModifiers & 8) !== 0, "Super observed on a key press");
                check(state, state.plainCharSeen, "A plain (no-modifier) character was recorded");
                check(state, (state.charModifiers & 1) !== 0, "Shift observed on a character");
                status.setText("Check -> key " + state.keyModifiers + " char " + state.charModifiers
                    + "   Checks: " + automaticCount(state));
            }));
            screen.addText("Press each modifier key once, then type a plain and a Shift character, then press Check.", 24, 108, 0xFFFFFF, false);
        }
    },
    {
        id: "WIDGETS-DIRECT-001",
        category: "Widgets",
        mode: "screen",
        title: "Direct Widgets",
        setup(screen, state) {
            const status = screen.addText("Use every control. Its callback reports its current state here.", 24, 46, 0xFFFFFF, false);
            let buttonCalls = 0;
            const action = screen.addButton(24, 70, 120, 20, "Action", wrap(() => {
                buttonCalls++;
                status.setText("Button callback count: " + buttonCalls);
            }));
            const checkbox = screen.addCheckbox(24, 96, 160, 20, "Checked", false, wrap(widget => {
                status.setText("Checkbox: " + widget.isChecked());
            }));
            const slider = screen.addSlider(24, 122, 160, 20, "Value", 0.5, 5, wrap(widget => {
                status.setText("Slider value: " + widget.getValue());
            }));
            const cycle = screen.addCyclingButton(24, 148, 160, 20, 0, ["One", "Two", "Three"], ["One", "Two", "Three"], "One", "", wrap(widget => {
                status.setText("Cycle value: " + widget.getStringValue());
            }));
            const input = screen.addTextInput(24, 174, 160, 20, "Type here", wrap(text => {
                status.setText("Text input: " + text);
            }));
            const lock = screen.addLockButton(194, 174, wrap(widget => {
                widget.setLocked(!widget.isLocked());
                status.setText("Locked: " + widget.isLocked());
            }));
            action.setTooltip("This button also has a script tooltip.");
            input.setSuggestion(" hint");
            check(state, checkbox.isChecked() === false, "Checkbox initial state is false");
            check(state, slider.getSteps() === 5, "Slider step count is retained");
            screen.addButton(194, 70, 150, 20, "Programmatic Action", wrap(() => {
                try {
                    action.click(false);
                } catch (error) {
                    status.setText("Programmatic click failed: " + error);
                }
            }));
            screen.addButton(194, 96, 150, 20, "Set Widget State", wrap(() => {
                checkbox.setChecked(true);
                slider.setValue(1.0);
                cycle.forward();
                lock.setLocked(true);
                input.setText("updated", false);
                status.setText("Widget setters applied");
            }));
        }
    },
    {
        id: "WIDGETS-BUILDER-001",
        category: "Widgets",
        mode: "screen",
        title: "Widget Builders",
        setup(screen, state) {
            const status = screen.addText("Use the builder-created controls below.", 24, 46, 0xFFFFFF, false);
            screen.buttonBuilder().message("Builder Button").pos(24, 70).size(150, 20)
                .action(wrap(() => status.setText("Builder button callback ran"))).build();
            screen.checkBoxBuilder(true).message("Builder Check").pos(24, 96).size(150, 20)
                .action(wrap(widget => status.setText("Builder check: " + widget.isChecked()))).build();
            screen.sliderBuilder().message("Builder Slider").pos(24, 122).size(150, 20).steps(4).initially(2)
                .action(wrap(widget => status.setText("Builder slider: " + widget.getValue()))).build();
            screen.lockButtonBuilder(false).pos(194, 96)
                .action(wrap(widget => {
                    widget.setLocked(!widget.isLocked());
                    status.setText("Builder lock: " + widget.isLocked());
                })).build();
            screen.textFieldBuilder().message("Builder input").suggestion(" hint").pos(24, 148).size(150, 20)
                .action(wrap(text => status.setText("Builder input: " + text))).build();
            const formatter = wrap(value => Chat.createTextBuilder().append(String(value)).build());
            screen.cyclicButtonBuilder(formatter).values("A", "B", "C").initially("A").option("Builder Cycle").pos(24, 174).size(180, 20)
                .action(wrap(widget => status.setText("Builder cycle: " + widget.getStringValue()))).build();
            check(state, listSize(screen.getButtonWidgets()) >= 4, "Builder widgets were added to the screen");
            check(state, listSize(screen.getTextFields()) >= 1, "Builder text field was added to the screen");
        }
    },
    {
        id: "WIDGETS-PREDICATE-001",
        category: "Widgets",
        mode: "screen",
        title: "Text Field Predicate",
        setup(screen, state) {
            const status = screen.addText("The field accepts at most three characters. Try typing and using the setters.", 24, 46, 0xFFFFFF, false);
            const input = screen.addTextInput(24, 72, 200, 20, "predicate", wrap(text => {
                status.setText("Field text: " + text);
            }));
            input.setTextPredicate(wrap(text => String(text).length <= 3));
            screen.addButton(24, 100, 200, 20, "Check Programmatic Set", wrap(() => {
                input.setText("abcd", false);
                check(state, input.getText().length <= 3, "Predicate rejects a too-long programmatic setText");
                input.setText("abc", false);
                check(state, input.getText() === "abc", "Predicate accepts a valid programmatic setText");
            }));
            screen.addButton(24, 126, 200, 20, "Reset Predicate", wrap(() => {
                input.resetTextPredicate();
                input.setText("abcd", false);
                check(state, input.getText() === "abcd", "resetTextPredicate() removes the length filter");
                status.setText("Predicate reset. Field text: " + input.getText());
            }));
            screen.addText("Type four or more characters first: the field must stop growing at three.", 24, 156, 0xFFFFFF, false);
        }
    },
    {
        id: "RENDER-PRIMITIVES-001",
        category: "Rendering",
        mode: "screen",
        title: "Render Primitives",
        setup(screen, state) {
            screen.addText("Blue rotated rectangle; white baseline; yellow Mutated text; diamond image; count-3 diamond; damaged sword bar; cyan Nested child label.", 24, 46, 0xFFFFFF, false);
            const rect = screen.rectBuilder(24, 72, 130, 110).color(30, 80, 160, 220).rotation(5).zIndex(1).buildAndAdd();
            const line = screen.lineBuilder(24, 118, 154, 118).color(255, 255, 255).width(2).zIndex(2).buildAndAdd();
            const text = screen.textBuilder("Mutable text").pos(38, 86).color(255, 255, 0).shadow(false).zIndex(3).buildAndAdd();
            screen.imageBuilder("minecraft:textures/item/diamond.png").pos(174, 72).size(32, 32).regions(0, 0, 16, 16, 16, 16).buildAndAdd();
            screen.itemBuilder().item("minecraft:diamond").pos(220, 72).overlayText("3").scale(1.5).buildAndAdd();
            const damagedSword = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative().setDamage(100);
            screen.itemBuilder(damagedSword).pos(270, 72).overlayVisible(true).buildAndAdd();
            const child = Hud.createDraw2D();
            child.setOnInit(wrap(draw => draw.addText("Nested", 0, 0, -11141121, false)));
            screen.draw2DBuilder(child).pos(320, 72).size(80, 30).buildAndAdd();
            rect.setRotation(8);
            text.setText("Mutated text");
            screen.removeElement(text);
            screen.reAddElement(text);
            check(state, rect.getRotation() === 8, "Rectangle mutation is retained");
            check(state, screen.getElements().contains(text), "Removed element can be re-added");
            screen.addText("The rectangle is intentionally rotated; the text and line are not. The sword has only a durability bar. Nested is at 320,72.", 24, 140, 0xFFFFFF, false);
        }
    },
    {
        id: "DRAW2D-OVERLAY-001",
        category: "Rendering",
        mode: "observe",
        title: "Draw2D Overlay",
        instructions: [
            "The cyan 'Manual suite overlay' is on the HUD at the top-left while this test runs.",
            "Confirm it is visible, then mark the result."
        ],
        prepare(state) {
            const overlay = Hud.createDraw2D();
            overlay.setOnInit(wrap(draw => draw.addText("Manual suite overlay", 0, 0, -11141121, true)));
            overlay.register();
            state.overlay = overlay;
            check(state, Hud.listDraw2Ds().contains(overlay), "register() lists the overlay");
            overlay.unregister();
            check(state, !Hud.listDraw2Ds().contains(overlay), "unregister() removes the overlay");
            overlay.register();
            check(state, Hud.listDraw2Ds().contains(overlay), "register() adds it back");
            return true;
        },
        teardown(state) {
            if (state.overlay != null) {
                try {
                    state.overlay.unregister();
                } catch (ignored) {
                }
                state.overlay = null;
            }
        }
    },
    {
        id: "TEXT-INTERACTION-001",
        category: "Text",
        mode: "screen",
        title: "Text Interaction",
        setup(screen, state) {
            const status = screen.addText("Hover and click each labeled example.", 24, 46, 0xFFFFFF, false);
            const goldTooltip = Chat.createTextBuilder().append("Only the gold text should show this tooltip.").withColor(0xA).build();
            const aquaTooltip = Chat.createTextBuilder().append("Only the aqua segment should show this tooltip.").withColor(0xB).build();
            const goldText = Chat.createTextBuilder().append("[1] Hover this gold text").withColor(0x6).withShowTextHover(goldTooltip).build();
            const segmentedText = Chat.createTextBuilder().append("[2] Plain prefix ").withColor(0x7).append("aqua hover target").withColor(0xB).withShowTextHover(aquaTooltip).append(" plain suffix").withColor(0x7).build();
            const clickText = Chat.createTextBuilder().append("[3] Click this green text").withColor(0xA)
                .withCustomClickEvent(wrap(() => status.setText("Custom text click callback ran"))).build();
            screen.addText(goldText, 24, 72, 0xFFFFFF, false);
            screen.addText(segmentedText, 24, 92, 0xFFFFFF, false);
            screen.addText(clickText, 24, 112, 0xFFFFFF, false);
            screen.addText("[4] This unstyled text must not show a tooltip.", 24, 132, 0xFFFFFF, false);
            check(state, true, "Styled and custom-click text was added");
        }
    },
    {
        id: "TEXT-WORLD-HOVER-001",
        category: "Text",
        mode: "screen",
        title: "World Hover",
        setup(screen, state) {
            const player = Player.getPlayer();
            if (player == null) {
                state.manualOnly = true;
                screen.addText("Join a world before running this test.", 24, 46, 0xFFFFFF, false);
                return;
            }
            const itemText = Chat.createTextBuilder().append("[1] Hover the diamond preview").withColor(0xB)
                .withShowItemHover(Client.getRegistryManager().getItemStack("minecraft:diamond")).build();
            const entityText = Chat.createTextBuilder().append("[2] Hover the player preview").withColor(0xA)
                .withShowEntityHover(player).build();
            screen.addText("Hover each line. The diamond tooltip should contain 'Diamond' and with Advanced Tooltips enabled, 'minecraft:diamond' and '<X> component(s)'.", 24, 46, 0xFFFFFF, false);
            screen.addText("Entity details requires Advanced Tooltips (F3+H) and should show the player name, type, and uuid.", 24, 60, 0xFFFFFF, false);
            screen.addText(itemText, 24, 78, 0xFFFFFF, false);
            screen.addText(entityText, 24, 94, 0xFFFFFF, false);
            check(state, true, "World item and entity hover styles were added");
        }
    },
    {
        id: "SCREEN-RENDER-001",
        category: "Rendering",
        mode: "screen",
        title: "Screen Render Callback",
        setup(screen, state) {
            state.renderCalls = 0;
            const status = screen.addText("Render callback count: 0", 24, 46, 0xFFFFFF, false);
            screen.setOnRender(wrap(() => {
                state.renderCalls++;
            }));
            screen.addText("Wait briefly, then press Check Render Callback. The count must be greater than zero.", 24, 72, 0xFFFFFF, false);
            screen.addButton(24, 98, 180, 20, "Check Render Callback", wrap(() => {
                status.setText("Render callback count: " + state.renderCalls);
                check(state, state.renderCalls > 0, "Render callback ran");
            }));
        }
    },
    {
        id: "SCREEN-HOST-001",
        category: "Integration",
        mode: "screen",
        title: "Host Screen",
        setup(screen, state) {
            const entry = session.hostScreen;
            const name = entry == null ? "None" : String(entry.getScreenClassName());
            screen.addText("Screen captured before opening the suite: " + name, 24, 46, 0xFFFFFF, false);
            screen.addText("Run the suite from a vanilla screen such as inventory to test screen wrapping and return navigation.", 24, 66, 0xFFFFFF, false);
            if (entry == null) {
                state.manualOnly = true;
                screen.addText(session.hostCaptureError == null ? "No host screen was captured. This run is not applicable." : "Host capture was unavailable: " + session.hostCaptureError, 24, 86, 0xFFFFFF, false);
            } else {
                check(state, String(entry.getScreenClassName()).length > 0, "Captured host screen exposes its class name");
                screen.addButton(24, 98, 180, 20, "Return to Host Screen", wrap(() => Hud.openScreen(entry)));
            }
        }
    },
    {
        id: "INTERACT-TARGET-001",
        category: "Interaction",
        mode: "observe",
        title: "Target Override",
        instructions: [
            "The crosshair target is overridden to the solid block under you.",
            "Look around: the block outline must stay locked on that block even when you look away.",
            "The override is checked automatically when you mark the result."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            const interactions = player == null ? null : Player.getInteractionManager();
            if (interactions == null) {
                return false;
            }
            interactions.setTargetShapeCheck(false, false);
            interactions.setTargetAirCheck(false, false);
            interactions.setTargetRangeCheck(false, false);
            // The block the player stands in is usually air; use the one below.
            const pos = player.getBlockPos().offset(0, -1, 0);
            interactions.setTarget(pos.getX(), pos.getY(), pos.getZ());
            state.targetPos = pos;
            state.interactions = interactions;
            check(state, interactions.hasTargetOverride(), "setTarget() marks a target override");
            return true;
        },
        verify(state) {
            if (state.interactions == null || state.targetPos == null) {
                return;
            }
            const targeted = state.interactions.getTargetedBlock();
            check(state, targeted != null
                && targeted.getX() === state.targetPos.getX()
                && targeted.getY() === state.targetPos.getY()
                && targeted.getZ() === state.targetPos.getZ(), "The client pick result still matches the override");
        },
        teardown(state) {
            if (state.interactions != null) {
                try {
                    state.interactions.clearTargetOverride();
                    state.interactions.resetTargetChecks();
                } catch (ignored) {
                }
                state.interactions = null;
            }
        }
    },
    {
        id: "DRAW3D-LIFECYCLE-001",
        category: "Draw3D",
        mode: "auto",
        title: "Draw3D Lifecycle",
        run(state) {
            withDraw(draw => {
                check(state, draw != null, "Hud.createDraw3D() returned an object");
                check(state, !Hud.listDraw3Ds().contains(draw), "A fresh Draw3D is not registered");
                check(state, sameObject(draw.register(), draw), "register() returns self for chaining");
                check(state, Hud.listDraw3Ds().contains(draw), "register() lists the Draw3D");
                check(state, sameObject(draw.unregister(), draw), "unregister() returns self for chaining");
                check(state, !Hud.listDraw3Ds().contains(draw), "unregister() removes the Draw3D");
                draw.addBox(0, 0, 0, 1, 1, 1, 0xFF0000, 0x220000, true);
                draw.addLine(0, 0, 0, 1, 1, 1, 0x00FF00);
                draw.addTraceLine(1, 1, 1, 0x0000FF);
                draw.addEntityTraceLine(null, 0xFFFFFF);
                draw.addDraw2D(0, 0, 0);
                check(state, listSize(draw.getBoxes()) === 1, "Elements are added to the Draw3D");
                draw.clear();
                check(state, listSize(draw.getBoxes()) === 0, "clear() removes boxes");
                check(state, listSize(draw.getLines()) === 0, "clear() removes lines");
                check(state, listSize(draw.getTraceLines()) === 0, "clear() removes trace lines");
                check(state, listSize(draw.getEntityTraceLines()) === 0, "clear() removes entity trace lines");
                check(state, listSize(draw.getDraw2Ds()) === 0, "clear() removes surfaces");
            });
            // Hud.clearDraw3Ds() empties the global registry. Keep any draws that
            // were registered before this test (other scripts) and restore them.
            const baseline = [];
            const registered = Hud.listDraw3Ds();
            for (let i = 0; i < listSize(registered); i++) {
                baseline.push(registered.get(i));
            }
            Hud.clearDraw3Ds();
            check(state, listSize(Hud.listDraw3Ds()) === 0, "Hud.clearDraw3Ds() empties the global registry");
            for (let i = 0; i < baseline.length; i++) {
                baseline[i].register();
            }
            check(state, listSize(Hud.listDraw3Ds()) === baseline.length, "Pre-existing Draw3Ds can be re-registered");
        }
    },
    {
        id: "DRAW3D-PRIMITIVES-001",
        category: "Draw3D",
        mode: "auto",
        title: "Draw3D Primitives",
        run(state) {
            withDraw(draw => {
                const box = draw.addBox(0, 0, 0, 1, 1, 1, 0xFF0000, 0x220000, true);
                const line = draw.addLine(0, 0, 0, 1, 1, 1, 0x00FF00);
                const trace = draw.addTraceLine(1, 2, 3, 0x0000FF);
                draw.addEntityTraceLine(null, 0xFFFFFF);
                draw.addPoint(1, 2, 3, 0.5, 0x123456);
                const surface = draw.addDraw2D(0, 0, 0, 0, 0, 0, 2, 1, 100, false, false);
                check(state, listSize(draw.getBoxes()) === 2, "addBox() and addPoint() both create boxes");
                check(state, listSize(draw.getLines()) === 1, "addLine() creates a line");
                // getTraceLines() matches TraceLine, and EntityTraceLine extends it,
                // so the entity trace is included here as well.
                check(state, listSize(draw.getTraceLines()) === 2, "addTraceLine() and addEntityTraceLine() create trace lines");
                check(state, listSize(draw.getEntityTraceLines()) === 1, "addEntityTraceLine() creates an entity trace line");
                check(state, listSize(draw.getDraw2Ds()) === 1, "addDraw2D() creates a surface");
                draw.removeBox(box);
                check(state, listSize(draw.getBoxes()) === 1, "removeBox() removes one box");
                draw.reAddElement(box);
                check(state, listSize(draw.getBoxes()) === 2, "reAddElement() re-adds the box");
                draw.removeLine(line);
                check(state, listSize(draw.getLines()) === 0, "removeLine() removes the line");
                draw.removeTraceLine(trace);
                check(state, listSize(draw.getTraceLines()) === 1, "removeTraceLine() removes the plain trace line");
                draw.removeDraw2D(surface);
                check(state, listSize(draw.getDraw2Ds()) === 0, "removeDraw2D() removes the surface");

                const Pos3D = Java.type("com.jsmacrosce.jsmacros.api.math.Pos3D");
                const point = draw.addPoint(1, 2, 3, 0.5, 0x123456);
                check(state, point.pos.x2 - point.pos.x1 === 1 && point.pos.y2 - point.pos.y1 === 1 && point.pos.z2 - point.pos.z1 === 1,
                    "addPoint(x, y, z, radius) creates a cube with side 2*radius");
                const posPoint = draw.addPoint(new Pos3D(1, 2, 3), 0.3, 0x123456);
                check(state, approx(posPoint.pos.x2 - posPoint.pos.x1, 0.6) && approx(posPoint.pos.y2 - posPoint.pos.y1, 0.6) && approx(posPoint.pos.z2 - posPoint.pos.z1, 0.6),
                    "addPoint(Pos3D, radius) creates a cube with side 2*radius");
                const block = draw.boxBuilder().forBlock(8, 9, 10).build();
                check(state, block.pos.x1 === 8 && block.pos.y1 === 9 && block.pos.z1 === 10
                    && block.pos.x2 - block.pos.x1 === 1 && block.pos.y2 - block.pos.y1 === 1 && block.pos.z2 - block.pos.z1 === 1,
                    "boxBuilder().forBlock(x, y, z) spans exactly one block");
                const beforeTrace = listSize(draw.getTraceLines());
                const posTrace = draw.addTraceLine(new Pos3D(1, 2, 3), 0xABCDEF);
                check(state, listSize(draw.getTraceLines()) === beforeTrace + 1
                    && sameObject(posTrace, draw.getTraceLines().get(beforeTrace)),
                    "addTraceLine(Pos3D, color) returns the trace line it adds");

                const etl = draw.addEntityTraceLine(null, 0xFF00FF);
                check(state, sameObject(etl.setEntity(null), etl), "EntityTraceLine.setEntity(null) returns self for chaining");
                check(state, etl.shouldRemove === false, "EntityTraceLine.shouldRemove starts false");
                const EntityTraceLine = Java.type("com.jsmacrosce.jsmacros.client.api.classes.render.components3d.EntityTraceLine");
                EntityTraceLine.dirty = false;
                check(state, EntityTraceLine.dirty === false, "EntityTraceLine.dirty is an accessible static flag");
            });
        }
    },
    {
        id: "DRAW3D-STYLE-001",
        category: "Draw3D",
        mode: "auto",
        title: "Draw3D Style and Builders",
        run(state) {
            withDraw(draw => {
                const box = draw.addBox(0, 0, 0, 1, 1, 1, 0x112233, 77, 0x445566, 88, true);
                check(state, alphaOf(box.color) === 77 && rgbOf(box.color) === 0x112233, "Box outline color and alpha are retained");
                check(state, alphaOf(box.fillColor) === 88 && rgbOf(box.fillColor) === 0x445566, "Box fill color and alpha are retained");
                box.setAlpha(64);
                check(state, alphaOf(box.color) === 64 && rgbOf(box.color) === 0x112233, "Box.setAlpha() preserves RGB");
                box.setFillAlpha(200);
                check(state, alphaOf(box.fillColor) === 200 && rgbOf(box.fillColor) === 0x445566, "Box.setFillAlpha() preserves RGB");
                box.setPosToBlock(10, 20, 30);
                check(state, box.pos.x1 === 10 && box.pos.y1 === 20 && box.pos.z1 === 30, "Box.setPosToBlock() sets the min corner");
                check(state, box.pos.x2 - box.pos.x1 === 1 && box.pos.y2 - box.pos.y1 === 1 && box.pos.z2 - box.pos.z1 === 1, "Box.setPosToBlock() sets a unit extent");
                const line = draw.addLine(0, 0, 0, 1, 2, 3, 0xAA22CC);
                line.setAlpha(100);
                check(state, alphaOf(line.color) === 100 && rgbOf(line.color) === 0xAA22CC, "Line3D.setAlpha() preserves RGB");

                const bb = draw.boxBuilder().pos1(1, 2, 3).pos2(4, 5, 6).color(0x102030).alpha(77).fillColor(0x405060).fillAlpha(88).fill(true).cull(true);
                check(state, bb.getPos1().x === 1 && bb.getPos2().z === 6, "Box.Builder positions are retained");
                check(state, bb.getColor() === 0x102030 && bb.getAlpha() === 77, "Box.Builder color and alpha are retained");
                check(state, bb.getFillColor() === 0x405060 && bb.getFillAlpha() === 88, "Box.Builder fill color and alpha are retained");
                check(state, bb.isFilled() && bb.isCulled(), "Box.Builder fill and cull flags are retained");

                const lb = draw.lineBuilder().pos1(1, 2, 3).pos2(4, 5, 6).color(0x123456).alpha(88).cull(true);
                check(state, lb.getPos1().x === 1 && lb.getPos2().z === 6, "Line3D.Builder positions are retained");
                check(state, lb.getColor() === 0x123456 && lb.getAlpha() === 88 && lb.isCulled(), "Line3D.Builder color, alpha and cull are retained");

                const tb = draw.traceLineBuilder().pos(1, 2, 3).color(0x012345).alpha(66);
                check(state, tb.getPos().x === 1 && tb.getColor() === 0x012345 && tb.getAlpha() === 66, "TraceLine.Builder getters are retained");

                const eb = draw.entityTraceLineBuilder().entity(null).yOffset(1.25).color(0x778899).alpha(44);
                check(state, eb.getYOffset() === 1.25 && eb.getColor() === 0x778899 && eb.getAlpha() === 44, "EntityTraceLine.Builder getters are retained");

                // The bare-RGB paths run through ColorUtil.fixAlpha, so a colour
                // without an alpha byte is treated as opaque instead of invisible.
                const bare = draw.addBox(0, 0, 0, 1, 1, 1, 0xFF0000, 0, false);
                check(state, alphaOf(bare.color) === 255 && rgbOf(bare.color) === 0xFF0000, "Box outline fixAlpha() defaults bare RGB to alpha 255");
                bare.setColor(0x00FF00, 255);
                check(state, alphaOf(bare.color) === 255 && rgbOf(bare.color) === 0x00FF00, "Box.setColor(rgb, alpha) sets the outline colour");
                bare.setFillColor(0x010203, 128);
                check(state, alphaOf(bare.fillColor) === 128 && rgbOf(bare.fillColor) === 0x010203, "Box.setFillColor(rgb, alpha) sets the fill colour");
                bare.setPosToPoint(3, 4, 5, 0.3);
                check(state, approx(bare.pos.x2 - bare.pos.x1, 0.6) && approx(bare.pos.y2 - bare.pos.y1, 0.6) && approx(bare.pos.z2 - bare.pos.z1, 0.6),
                    "Box.setPosToPoint(x, y, z, radius) creates a cube with side 2*radius");

                // Regression for the old Box.Builder.color(int, int) bug where it
                // assigned fillColor instead of the outline colour.
                const builderColor = draw.boxBuilder().fillColor(0xA1B2C3).color(0x112233, 77).build();
                check(state, alphaOf(builderColor.color) === 77 && rgbOf(builderColor.color) === 0x112233,
                    "Box.Builder.color(rgb, alpha) sets the outline colour, not the fill colour");
                check(state, rgbOf(builderColor.fillColor) === 0xA1B2C3, "Box.Builder.fillColor(rgb) is retained");

                const bareLine = draw.addLine(0, 0, 0, 1, 2, 3, 0xFFFFFF);
                bareLine.setColor(0x123456);
                check(state, alphaOf(bareLine.color) === 255 && rgbOf(bareLine.color) === 0x123456, "Line3D.setColor(rgb) uses fixAlpha()");
                const alphaTrace = draw.addTraceLine(1, 2, 3, 0x112233, 90);
                check(state, sameObject(alphaTrace.setAlpha(33), alphaTrace), "TraceLine.setAlpha() returns self for chaining");
                check(state, sameObject(alphaTrace.setColor(0x445566, 12), alphaTrace), "TraceLine.setColor(rgb, alpha) returns self for chaining");
            });
        }
    },
    {
        id: "DRAW3D-SURFACE-001",
        category: "Draw3D",
        mode: "auto",
        title: "Draw3D Surface",
        run(state) {
            withDraw(draw => {
                const surface = draw.addDraw2D(0, 0, 0, 0, 0, 0, 4, 2, 200, false, false);
                check(state, surface.getMinSubdivisions() === 200, "Surface minSubdivisions is retained");
                check(state, surface.getWidth() === 400 && surface.getHeight() === 200, "Surface width and height derive from size and minSubdivisions");
                surface.setMinSubdivisions(100);
                check(state, surface.getWidth() === 200 && surface.getHeight() === 100, "Surface size updates with minSubdivisions");
                surface.setPos(9, 8, 7);
                surface.setRotations(10, 20, 30);
                surface.setSizes(8, 4);
                check(state, surface.pos.x === 9 && surface.rotations.y === 20 && surface.getSizes().x === 8, "Surface position, rotation and size mutators work");

                surface.addRect(0, 0, 40, 20, 0x225522, 175, 0, 0);
                surface.addText("Surface", 4, 5, 0xFFFFFF, false);
                surface.addLine(0, 0, 40, 20, 0x55DDFF, 2, 1.0, 0);
                surface.addImage(0, 0, 16, 16, "minecraft:textures/item/diamond.png", 0, 0, 16, 16, 16, 16);
                surface.addItem(0, 0, "minecraft:diamond_sword", true, 1.0, 0);
                const nested = Hud.createDraw2D();
                nested.setOnInit(wrap(child => child.addText("Nested", 0, 0, 0xFFFFFF, false)));
                surface.addDraw2D(nested, 0, 0, 20, 10, 0);
                check(state, listSize(surface.getRects()) === 1, "Surface rect added");
                check(state, listSize(surface.getTexts()) === 1, "Surface text added");
                check(state, listSize(surface.getLines()) === 1, "Surface line added");
                check(state, listSize(surface.getImages()) === 1, "Surface image added");
                check(state, listSize(surface.getItems()) === 1, "Surface item added");
                check(state, listSize(surface.getDraw2Ds()) === 1, "Surface nested Draw2D added");
            });
        }
    },
    {
        id: "INVENTORY-ITEMTAGS-001",
        category: "Inventory",
        mode: "auto",
        title: "Item Tags",
        run(state) {
            const stack = Client.getRegistryManager().getItemStack("minecraft:diamond");
            const tags = stack.getTags();
            check(state, tags != null, "ItemStackHelper.getTags() returns a list");
            const names = [];
            for (let i = 0; i < listSize(tags); i++) {
                names.push(String(tags.get(i)));
            }
            check(state, names.length > 0, "The diamond item reports at least one tag");
            check(state, names.every(name => name.indexOf(":") > 0), "Tag ids are namespaced");
        }
    },
    {
        id: "DRAW3D-VISUAL-001",
        category: "Draw3D",
        mode: "observe",
        title: "Draw3D Visual",
        instructions: [
            "Look straight ahead: red filled box, green line, blue trace line, and a Draw3D surface panel.",
            "The yellow box is always-on-top; the magenta box is depth-tested.",
            "Put a wall between you and the far boxes: yellow must stay visible, magenta must be hidden."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            if (player == null) {
                return false;
            }
            const draw = Hud.createDraw3D();
            state.draw = draw;
            const p = forwardPosition(player, 3.0, -0.6);
            // Explicit fill alpha: the 10-arg overload takes alpha from the fill
            // color, so 0x550000 (alpha 0) would render outline-only.
            draw.addBox(p.x - 0.5, p.y - 0.5, p.z - 0.5, p.x + 0.5, p.y + 0.5, p.z + 0.5, 0xFF3333, 0xFF, 0x550000, 0x80, true, false);
            draw.addLine(p.x - 1.2, p.y + 0.8, p.z, p.x + 1.2, p.y + 0.8, p.z, 0x33FF66, false);
            // End at the box centre so the trace visibly points at the red cube.
            draw.addTraceLine(p.x, p.y, p.z, 0x33CCFF);
            const far = forwardPosition(player, 12.0, 0);
            draw.addBox(far.x - 0.6, far.y - 0.6, far.z - 0.6, far.x + 0.6, far.y + 0.6, far.z + 0.6, 0xFFFF00, 0x000000, false, false);
            draw.addBox(far.x + 1.8, far.y - 0.6, far.z - 0.6, far.x + 3.0, far.y + 0.6, far.z + 0.6, 0xFF00FF, 0x000000, false, true);
            const surface = draw.addDraw2D(p.x - 1.0, p.y + 0.4, p.z, 0, 0, 0, 2.0, 1.0, 120, true, false);
            surface.addRect(0, 0, 120, 60, 0x1F1F1F, 200, 0, 0);
            surface.addText("Draw3D", 8, 8, 0xFFFFFF, 1, false, 0.8, 0);
            draw.register();
            check(state, Hud.listDraw3Ds().contains(draw), "Visual Draw3D is registered");
            check(state, listSize(draw.getBoxes()) === 3, "Three boxes added");
            check(state, listSize(draw.getLines()) === 1, "One line added");
            check(state, listSize(draw.getTraceLines()) === 1, "One trace line added");
            check(state, listSize(draw.getDraw2Ds()) === 1, "One surface added");
            return true;
        },
        teardown(state) {
            if (state.draw != null) {
                try {
                    state.draw.unregister();
                } catch (ignored) {
                }
                state.draw = null;
            }
        }
    },
    {
        id: "DRAW3D-ENTITY-001",
        category: "Draw3D",
        mode: "observe",
        title: "Entity Trace Line",
        instructions: [
            "Stand near a mob or animal: a red trace line points at its feet (yOffset 0) and a green line points above its head (yOffset 2.5).",
            "Move and look around: both lines must track the entity rather than stay in world space.",
            "Kill the entity (attack it or /kill): both lines must disappear automatically. The test passes once they do, or click Pass/Fail/Skip."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            if (player == null) {
                return false;
            }
            const target = nearestNonPlayerEntity(player, 16);
            if (target == null) {
                chatText("No nearby non-player entity to trace; skipping DRAW3D-ENTITY-001.", 0xFFFF55);
                return false;
            }
            const draw = Hud.createDraw3D();
            state.draw = draw;
            state.target = target;
            draw.addEntityTraceLine(target, 0xFF0000, 255, 0);
            draw.addEntityTraceLine(target, 0x00FF00, 255, 2.5);
            draw.register();
            state.sawTrace = true;
            check(state, listSize(draw.getEntityTraceLines()) === 2, "Two entity trace lines were added");
            check(state, sameObject(draw.getEntityTraceLines().get(0).setEntity(target), draw.getEntityTraceLines().get(0)),
                "EntityTraceLine.setEntity(entity) returns self for chaining");
            return true;
        },
        done(state) {
            if (state.sawTrace !== true || state.draw == null || entityAlive(state.target)) {
                return false;
            }
            // The render pass flags shouldRemove, then Draw3D drops the line.
            return listSize(state.draw.getEntityTraceLines()) === 0;
        },
        verify(state) {
            if (state.sawTrace === true && !entityAlive(state.target)) {
                check(state, listSize(state.draw.getEntityTraceLines()) === 0, "Trace lines were auto-removed once the entity was gone");
            }
        },
        teardown(state) {
            if (state.draw != null) {
                try {
                    state.draw.unregister();
                } catch (ignored) {
                }
                state.draw = null;
            }
            state.target = null;
        }
    },
    {
        id: "DRAW3D-SURFACE-002",
        category: "Draw3D",
        mode: "observe",
        title: "Surface Render Regression",
        instructions: [
            "Front panel: a green rotated rect renders above a red rect (zIndex), with text, a cyan line, a diamond image and a diamond sword item.",
            "A small blue 'Nested' sub-panel sits on the front panel.",
            "A second, tilted panel nearby has an orange diagonal line.",
            "The 'Bound Surface' panel near the camera must stay locked in view as you move and look around.",
            "Known on this branch: 3D surfaces may not render at all (Surface.render is disabled). If nothing appears, mark Skip and note it.",
            "Confirm every element is visible, then mark the result."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            if (player == null) {
                return false;
            }
            const draw = Hud.createDraw3D();
            state.draw = draw;
            const p = forwardPosition(player, 3.0, -0.6);
            const main = draw.addDraw2D(p.x - 1.2, p.y, p.z, 0, player.getYaw(), 0, 2.4, 1.6, 160, true, false);
            main.setRotateToPlayer(false);
            main.setRotateCenter(true);
            main.zIndexScale = 0.002;
            main.addRect(0, 0, 160, 100, 0x1F1F1F, 220, 0, 0);
            main.addRect(8, 8, 152, 92, 0x2A2A2A, 210, 0, 1);
            main.addRect(16, 20, 84, 76, 0xCC3333, 180, 6, 2);
            main.addRect(24, 28, 92, 84, 0x33CC66, 180, -6, 3);
            main.addText("Surface 3D Test", 10, 8, 0xFFFFFF, 10, true, 1.0, 0);
            main.addText("zIndex: red under green", 10, 84, 0xFFFF88, 10, false, 0.75, 0);
            main.addLine(8, 96, 152, 96, 0x55DDFF, 9, 2.0, 0);
            const image = main.addImage(102, 22, 24, 24, 4, "minecraft:textures/item/diamond.png", 0, 0, 16, 16, 16, 16, 0);
            image.setZIndex(4);
            const item = main.addItem(128, 18, 7, "minecraft:diamond_sword", true, 1.0, -8);
            item.setZIndex(7);
            const nested = Hud.createDraw2D();
            nested.setOnInit(wrap(child => {
                child.addRect(0, 0, 40, 28, 0x111188, 180, 0, 0);
                child.addText("Nested", 4, 9, 0xFFFFFF, 1, false, 0.7, 0);
            }));
            main.addDraw2D(nested, 108, 58, 40, 28, 8);
            const rotatedPoint = forwardPosition(player, 4.2, -0.1);
            const rotated = draw.addDraw2D(rotatedPoint.x + 0.6, rotatedPoint.y, rotatedPoint.z, 20, player.getYaw(), 0, 1.6, 0.9, 120, true, false);
            rotated.setRotateCenter(true);
            rotated.addRect(0, 0, 120, 68, 0x0F4D66, 190, 0, 0);
            rotated.addText("Rotated panel", 10, 10, 0xFFFFFF, 1, true, 0.8, 0);
            rotated.addLine(8, 54, 110, 18, 0xFFAA33, 2, 2.0, 0);
            const bound = draw.addDraw2D(p.x, p.y + 0.5, p.z, 0, 0, 0, 1.3, 0.35, 64, true, false);
            bound.setRotateCenter(true);
            bound.setRotateToPlayer(false);
            bound.addRect(0, 0, 64, 18, 0x225522, 175, 0, 0);
            bound.addText("Bound Surface", 4, 5, 0xDDFFDD, 1, false, 0.65, 0);
            state.bound = bound;
            updateHeadLockedSurface(bound, player);
            draw.register();
            check(state, listSize(main.getTexts()) >= 2, "Main surface texts were added");
            check(state, listSize(main.getRects()) >= 4, "Main surface rects were added");
            check(state, listSize(main.getLines()) >= 1, "Main surface line was added");
            check(state, listSize(main.getImages()) >= 1, "Main surface image was added");
            check(state, listSize(main.getItems()) >= 1, "Main surface item was added");
            check(state, listSize(main.getDraw2Ds()) >= 1, "Main surface nested Draw2D was added");
            check(state, main.zIndexScale === 0.002, "Surface.zIndexScale is writeable");
            check(state, main.isRotatingCenter(), "setRotateCenter(true) is retained");
            check(state, !main.doesRotateToPlayer(), "setRotateToPlayer(false) is retained");
            return true;
        },
        tick(state) {
            if (state.bound == null) {
                return;
            }
            // Re-anchor on the game thread: pos/rotations are read by the render
            // thread while it sorts elements, so they are not safe to write from
            // the script thread.
            onMain(() => {
                const player = Player.getPlayer();
                if (state.bound != null && player != null) {
                    updateHeadLockedSurface(state.bound, player);
                }
            });
        },
        teardown(state) {
            if (state.draw != null) {
                try {
                    state.draw.unregister();
                } catch (ignored) {
                }
                state.draw = null;
            }
            state.bound = null;
        }
    },
    {
        id: "DRAW3D-SURFACE-003",
        category: "Draw3D",
        mode: "observe",
        title: "Surface Facing and Mutation",
        instructions: [
            "A green 'Faces You' panel must keep turning to face you from its own position as you walk around it, including up close.",
            "A red 'Before' panel starts small, then grows and changes colour in place, then moves to the right, staying visible throughout.",
            "Known on this branch: 3D surfaces may not render at all (Surface.render is disabled). If nothing appears, mark Skip and note it.",
            "Confirm both panels keep rendering through the change, then mark the result."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            if (player == null) {
                return false;
            }
            const draw = Hud.createDraw3D();
            state.draw = draw;
            const facingPoint = forwardPosition(player, 3.0, -0.2);
            const facing = draw.addDraw2D(facingPoint.x - 1.4, facingPoint.y + 1.6, facingPoint.z, 1.2, 0.8);
            facing.setRotateToPlayer(true);
            facing.setRotateCenter(true);
            facing.addRect(0, 0, facing.getWidth(), facing.getHeight(), 0x22AA22, 0x55, 0, 0);
            facing.addText("Faces You", 8, 8, 0xFFFFFF, 1, false, 0.7, 0);
            state.facing = facing;

            const centre = forwardPosition(player, 3.0, 0.6);
            const mutating = draw.addDraw2D(centre.x + 1.2, centre.y, centre.z, 1.2, 0.8);
            state.mutating = mutating;
            state.mutatingRect = mutating.addRect(0, 0, mutating.getWidth(), mutating.getHeight(), 0xAA2222, 0x55, 0, 0);
            state.mutatingText = mutating.addText("Before", 8, 8, 0xFFFFFF, 1, false, 0.7, 0);
            state.ticks = 0;
            state.mutated = false;
            draw.register();
            check(state, listSize(draw.getDraw2Ds()) === 2, "Both surfaces were added");
            check(state, facing.doesRotateToPlayer(), "setRotateToPlayer(true) is retained");
            return true;
        },
        tick(state) {
            if (state.mutated || state.mutating == null) {
                return;
            }
            state.ticks++;
            if (state.ticks !== 20) {
                return;
            }
            state.mutated = true;
            onMain(() => {
                const surface = state.mutating;
                if (surface == null) {
                    return;
                }
                surface.setSizes(4.0, 2.2);
                surface.removeRect(state.mutatingRect);
                surface.removeText(state.mutatingText);
                surface.addRect(0, 0, surface.getWidth(), surface.getHeight(), 0x2255AA, 0x66, 0, 0);
                surface.addText("Resized", 8, 8, 0xFFFFFF, 1, false, 0.9, 0);
                const player = Player.getPlayer();
                if (player != null) {
                    const moved = forwardPosition(player, 3.0, 2.0);
                    surface.setPos(moved.x + 2.0, moved.y, moved.z);
                }
            });
        },
        verify(state) {
            if (!state.mutated || state.mutating == null) {
                return;
            }
            check(state, approx(state.mutating.getSizes().x, 4.0) && approx(state.mutating.getSizes().y, 2.2), "Surface resized in place");
            check(state, listSize(state.mutating.getRects()) === 1 && listSize(state.mutating.getTexts()) === 1,
                "Surface kept exactly the new rect and text after mutation");
            check(state, !state.mutating.getRects().contains(state.mutatingRect), "The removed rect is gone after mutation");
        },
        teardown(state) {
            if (state.draw != null) {
                try {
                    state.draw.unregister();
                } catch (ignored) {
                }
                state.draw = null;
            }
            state.facing = null;
            state.mutating = null;
            state.mutatingRect = null;
            state.mutatingText = null;
        }
    },
    {
        id: "DRAW3D-ITEM-001",
        category: "Draw3D",
        mode: "observe",
        title: "Item Overlay Text",
        instructions: [
            "Top-left HUD: a black panel with a diamond item. The item must show the overlay text 'overlay_test_text' in the item's count position.",
            "In front of you: a 3D surface with the same diamond item and overlay text.",
            "Known on this branch: 3D surfaces may not render at all (Surface.render is disabled). If the 3D half never appears, mark Skip and note it.",
            "Confirm the overlay text is readable in both places, then mark the result."
        ],
        prepare(state) {
            const player = Player.getPlayer();
            if (player == null) {
                return false;
            }
            const hud = Hud.createDraw2D();
            hud.setOnInit(wrap(overlay => {
                overlay.addRect(8, 8, 150, 52, 0x000000, 100);
                overlay.addText("2D HUD", 12, 12, 0xFFFFFF, false);
                overlay.itemBuilder().item("minecraft:diamond").pos(12, 24).overlayVisible(true).overlayText("overlay_test_text").buildAndAdd();
            }));
            hud.register();
            state.hud = hud;
            check(state, Hud.listDraw2Ds().contains(hud), "The HUD item overlay is registered");

            const draw = Hud.createDraw3D();
            state.draw = draw;
            const p = forwardPosition(player, 4.0, 0);
            const surface = draw.addDraw2D(p.x - 1.0, p.y, p.z, 2, 1);
            surface.addRect(0, 0, surface.getWidth(), surface.getHeight(), 0x000000, 100, 0, 0);
            surface.addText("3D Surface", 8, 8, 0xFFFFFF, 1, false, 0.8, 0);
            surface.itemBuilder().item("minecraft:diamond").pos(8, 24).overlayVisible(true).overlayText("overlay_test_text").buildAndAdd();
            draw.register();
            check(state, listSize(surface.getItems()) === 1, "The surface item was added");
            return true;
        },
        teardown(state) {
            if (state.hud != null) {
                try {
                    state.hud.unregister();
                } catch (ignored) {
                }
                state.hud = null;
            }
            if (state.draw != null) {
                try {
                    state.draw.unregister();
                } catch (ignored) {
                }
                state.draw = null;
            }
        }
    },
    {
        id: "HUD-DEBUG-001",
        category: "Hud",
        mode: "observe",
        title: "Debug Screen Overlay",
        instructions: [
            "The aqua 'Debug suite overlay' is registered on the HUD for this test and is visible even without F3.",
            "Press F3 and confirm the same aqua line also renders on top of the debug HUD.",
            "Press F3 again to close the debug screen, then mark the result."
        ],
        prepare(state) {
            if (String(Client.getModLoader()).toLowerCase().indexOf("fabric") < 0) {
                return false;
            }
            const overlay = Hud.createDraw2D();
            overlay.setOnInit(wrap(draw => draw.addText("Debug suite overlay", 0, 0, -11141121, true)));
            overlay.register();
            state.overlay = overlay;
            check(state, Hud.listDraw2Ds().contains(overlay), "Overlay is registered before opening the debug screen");
            return true;
        },
        teardown(state) {
            if (state.overlay != null) {
                try {
                    state.overlay.unregister();
                } catch (ignored) {
                }
                state.overlay = null;
            }
        }
    }
];

function categories() {
    return [...new Set(TESTS.map(test => test.category))];
}

function testsInCategory(category) {
    return TESTS.filter(test => test.category === category);
}

function modeLabel(mode) {
    return mode === "auto" ? "auto" : mode === "observe" ? "visual" : mode === "event" ? "event" : "screen";
}

function record(test, state, outcome) {
    session.results[test.id] = {
        outcome: state.manualOnly ? "Skipped" : outcome,
        automatic: automaticCount(state)
    };
}

function resultLabel(test) {
    const result = session.results[test.id];
    return result == null ? "Not checked" : result.outcome + " (" + result.automatic + ")";
}

/* ------------------------------------------------------------------ */
/* screen UI                                                          */
/* ------------------------------------------------------------------ */

function openMenu() {
    onMain(() => {
        const screen = Hud.createScreen("Manual Tests", true);
        screen.setOnInit(wrap(current => {
            const columns = 3;
            const gap = 8;
            const margin = 12;
            const columnWidth = Math.floor((current.getWidth() - margin * 2 - gap * (columns - 1)) / columns);
            const grid = (index, y, label, callback) => {
                const x = margin + (index % columns) * (columnWidth + gap);
                current.addButton(x, y, columnWidth, 20, label, wrap(callback));
            };
            current.addText("Chat: [Pass]/[Fail]/[Skip]/[Retry] during a visual test. H reopens this menu.", 24, 42, 0xAAAAAA, false);
            grid(0, 64, "Run All Tests", () => request("run-all"));
            grid(1, 64, "Run Automated", () => request("auto"));
            grid(2, 64, "Run Visual", () => request("observe-all"));
            grid(0, 88, "Run Screen", () => request("screen-all"));
            const categoryNames = categories();
            let y = 116;
            for (let i = 0; i < categoryNames.length; i++) {
                grid(i, y, categoryNames[i], () => request("category", categoryNames[i]));
                if (i % columns === columns - 1) {
                    y += 24;
                }
            }
            if (categoryNames.length % columns !== 0) {
                y += 24;
            }
            grid(0, y + 8, "Results", () => request("results"));
            grid(1, y + 8, "Quit", () => request("quit"));
        }));
        Hud.openScreen(screen);
    });
}

function openCategory(category) {
    onMain(() => {
        const screen = Hud.createScreen(category, true);
        screen.setOnInit(wrap(current => {
            current.addText("Select a test.", 24, 46, 0xFFFFFF, false);
            const list = testsInCategory(category);
            const screenTests = list.filter(test => test.mode === "screen");
            let y = 72;
            for (const test of list) {
                centeredButton(current, y, test.title, () => {
                    if (test.mode === "auto") {
                        request("auto-one", test);
                    } else if (test.mode === "observe" || test.mode === "event") {
                        request("observe", test);
                    } else {
                        request("screen-seq", {
                            tests: screenTests,
                            position: screenTests.indexOf(test),
                            backType: "category",
                            backPayload: category
                        });
                    }
                });
                y += 24;
            }
            centeredButton(current, y + 8, "Back", () => request("menu"));
        }));
        Hud.openScreen(screen);
    });
}

function openScreenSequence(tests, position, backType, backPayload) {
    const test = tests[position];
    const state = { checks: [] };
    onMain(() => {
        const screen = Hud.createScreen(test.title, test.background == null ? true : test.background);
        screen.setOnClose(wrap(() => screenCleanup(state)));
        const goBack = () => request(backType, backPayload);
        const goNext = () => {
            if (position + 1 < tests.length) {
                request("screen-seq", { tests: tests, position: position + 1, backType: backType, backPayload: backPayload });
            } else {
                request("results");
            }
        };
        screen.setOnInit(wrap(current => {
            // init() runs again on resize/reload; drop the old automatic checks
            // so the reported count stays idempotent.
            state.checks.length = 0;
            const controlsY = current.getHeight() - 48;
            state.statusText = current.addText("Checks: 0/0", 24, controlsY - 44, 0xFFFFFF, false);
            test.setup(current, state);
            state.statusText.setText("Checks: " + automaticCount(state) + "   result: " + resultLabel(test));
            current.addButton(24, controlsY - 22, 90, 20, "Passed", wrap(() => {
                record(test, state, "Passed");
                goNext();
            }));
            current.addButton(124, controlsY - 22, 90, 20, "Failed", wrap(() => {
                record(test, state, "Failed");
                goNext();
            }));
            current.addButton(224, controlsY - 22, 90, 20, "Back", wrap(goBack));
            let navigatorX = 24;
            if (position > 0) {
                current.addButton(navigatorX, controlsY, 90, 20, "Previous", wrap(() => request("screen-seq", { tests: tests, position: position - 1, backType: backType, backPayload: backPayload })));
                navigatorX += 100;
            }
            if (position + 1 < tests.length) {
                current.addButton(navigatorX, controlsY, 90, 20, "Next", wrap(() => request("screen-seq", { tests: tests, position: position + 1, backType: backType, backPayload: backPayload })));
            } else {
                current.addButton(navigatorX, controlsY, 90, 20, "Results", wrap(() => request("results")));
            }
        }));
        Hud.openScreen(screen);
    });
}

function openResults() {
    // Results are posted in chat, so get the menu out of the way.
    onMain(() => {
        const open = Hud.getOpenScreen();
        if (open != null) {
            open.close();
        }
    });
    for (const test of TESTS) {
        chatText(test.id + " [" + modeLabel(test.mode) + "] " + test.title + ": " + resultLabel(test), resultColor(test));
    }
    postPanel(plan());
    const automated = TESTS.filter(test => test.mode === "auto" && session.results[test.id] != null);
    const passed = automated.filter(test => {
        const result = session.results[test.id];
        return result != null && result.automatic.split("/")[0] === result.automatic.split("/")[1];
    });
    chatText("Automated: " + passed.length + "/" + automated.length + " fully passed.", passed.length === automated.length ? 0x55FF55 : 0xFFAA55);
}

function resultColor(test) {
    const result = session.results[test.id];
    if (result == null) {
        return 0xAAAAAA;
    }
    if (result.outcome === "Passed") {
        return 0x55FF55;
    }
    if (result.outcome === "Failed") {
        return 0xFF5555;
    }
    if (result.outcome === "Skipped") {
        return 0xFFFF55;
    }
    return 0xFFFFFF;
}

/* ------------------------------------------------------------------ */
/* runners                                                            */
/* ------------------------------------------------------------------ */

function allPassed(state) {
    return state.checks.length > 0 && state.checks.every(check => check.passed);
}

function runAuto(tests) {
    const list = tests == null ? TESTS.filter(test => test.mode === "auto") : tests;
    chatText("Running " + list.length + " automated test(s)...", 0x55FFFF);
    for (const test of list) {
        const state = { checks: [] };
        // Text construction bakes font glyphs, which requires the render
        // thread; run every test body there.
        onMain(() => {
            try {
                test.run(state);
            } catch (error) {
                check(state, false, "Test threw: " + error);
            }
        });
        record(test, state, allPassed(state) ? "Passed" : "Failed");
        chatText((allPassed(state) ? "[PASS] " : "[FAIL] ") + test.id + " " + test.title + " (" + automaticCount(state) + ")", allPassed(state) ? 0x55FF55 : 0xFF5555);
        for (const failed of state.checks.filter(check => !check.passed)) {
            chatText("    - " + failed.label, 0xFFAA55);
        }
    }
    postPanel(plan());
}

function showHint(text) {
    hideHint();
    // register() runs onInit(), which builds Text and measures font width; that
    // must happen on the render thread.
    onMain(() => {
        // Bottom-left, so the hint never overlaps test overlays drawn at the
        // top-left (for example HUD-DEBUG-001's own overlay). Compute the height
        // inside onInit so a window resize repositions it.
        const overlay = Hud.createDraw2D();
        overlay.setOnInit(wrap(draw => {
            const height = Hud.getWindowHeight();
            draw.addText("JsMacrosCE test: " + text, 6, height - 26, 0xFFFF55, true);
            draw.addText("Open chat (T) and click [Pass]/[Fail]/[Skip]; [H] reopens the menu.", 6, height - 14, 0xFFFFFF, true);
        }));
        overlay.register();
        session.hint = overlay;
        trackOverlay(overlay);
    });
}

function hideHint() {
    if (session.hint != null) {
        try {
            session.hint.unregister();
        } catch (ignored) {
        }
        untrackOverlay(session.hint);
        session.hint = null;
    }
}

function isNavigationRequest(request) {
    return request.type === "menu" || request.type === "category" || request.type === "results" || request.type === "quit";
}

function waitDecision(test, state) {
    session.decision = null;
    const deadline = test.timeoutSeconds == null ? null : Date.now() + test.timeoutSeconds * 1000;
    while (!session.quit) {
        // Keep the browse screen usable while a test waits: process navigation
        // requests, but leave test-start requests queued until this test ends.
        while (session.queue.length > 0 && isNavigationRequest(session.queue[0])) {
            handle(session.queue.shift());
        }
        if (session.quit) {
            break;
        }
        if (session.decision != null) {
            const decision = session.decision;
            session.decision = null;
            return decision;
        }
        // Future event tests complete automatically once their listener has
        // captured what they need (see test.done) without a chat click.
        if (test.done != null && test.done(state)) {
            session.decision = null;
            return "pass";
        }
        if (deadline != null && Date.now() > deadline) {
            session.decision = null;
            chatText("Timed out waiting for " + test.id + ".", 0xFF5555);
            return "fail";
        }
        // Optional per-tick hook for animated visual tests (head-locked panels,
        // staged surface mutation). Runs on the script thread; a hook that needs
        // the render thread should marshal with onMain itself.
        if (test.tick != null && state.tickError !== true) {
            try {
                test.tick(state);
            } catch (error) {
                state.tickError = true;
                check(state, false, "Tick hook failed: " + error);
            }
        }
        Client.waitTick();
    }
    return "skip";
}

function runObserve(test) {
    const state = { checks: [] };
    let ready = false;
    // prepare() registers overlays and builds Text, which is render-thread work.
    onMain(() => {
        try {
            ready = test.prepare(state) !== false;
        } catch (error) {
            check(state, false, "Prepare failed: " + error);
            ready = false;
        }
    });
    if (!ready) {
        record(test, state, "Skipped");
        chatText("[SKIP] " + test.id + " " + test.title, 0xFFFF55);
        try {
            if (test.teardown != null) test.teardown(state);
        } catch (ignored) {
        }
        return;
    }
    let decision = "skip";
    try {
        onMain(() => {
            const open = Hud.getOpenScreen();
            if (open != null) {
                open.close();
            }
        });
        showHint(test.title);
        for (;;) {
            postLines(test.instructions || []);
            postDecisionLine(test);
            decision = waitDecision(test, state);
            if (decision === "retry") {
                try {
                    if (test.teardown != null) test.teardown(state);
                } catch (ignored) {
                }
                state.checks.length = 0;
                state.tickError = false;
                let retryReady = false;
                onMain(() => {
                    try {
                        retryReady = test.prepare(state) !== false;
                    } catch (error) {
                        check(state, false, "Prepare failed: " + error);
                        retryReady = false;
                    }
                });
                if (!retryReady) {
                    decision = "skip";
                    break;
                }
                showHint(test.title);
                continue;
            }
            break;
        }
        try {
            if (test.verify != null) test.verify(state);
        } catch (error) {
            check(state, false, "Verify failed: " + error);
        }
    } finally {
        try {
            if (test.teardown != null) test.teardown(state);
        } catch (ignored) {
        }
        hideHint();
    }
    session.results[test.id] = {
        outcome: decision === "pass" ? "Passed" : decision === "fail" ? "Failed" : "Skipped",
        automatic: automaticCount(state)
    };
    chatText("[" + String(decision).toUpperCase() + "] " + test.id + " " + test.title + " (" + automaticCount(state) + ")", decision === "pass" ? 0x55FF55 : decision === "fail" ? 0xFF5555 : 0xFFFF55);
    postPanel(plan());
}

function runObserveAll() {
    for (const test of TESTS.filter(test => test.mode === "observe" || test.mode === "event")) {
        if (session.quit) return;
        runObserve(test);
    }
    chatText("Visual tests complete.", 0x55FFFF);
    postPanel(plan());
}

function runAll() {
    runAuto(null);
    if (session.quit) return;
    runObserveAll();
    if (session.quit) return;
    const tests = TESTS.filter(test => test.mode === "screen");
    if (tests.length > 0) {
        openScreenSequence(tests, 0, "menu", null);
    }
}

/* ------------------------------------------------------------------ */
/* loop                                                               */
/* ------------------------------------------------------------------ */

function handle(request) {
    try {
        switch (request.type) {
            case "menu":
                openMenu();
                break;
            case "category":
                openCategory(request.payload);
                break;
            case "auto":
                runAuto(null);
                break;
            case "auto-one":
                runAuto([request.payload]);
                break;
            case "observe":
            case "event":
                runObserve(request.payload);
                break;
            case "observe-all":
                runObserveAll();
                break;
            case "run-all":
                runAll();
                break;
            case "screen-all":
                openScreenSequence(TESTS.filter(test => test.mode === "screen"), 0, "menu", null);
                break;
            case "screen-seq":
                openScreenSequence(request.payload.tests, request.payload.position, request.payload.backType, request.payload.backPayload);
                break;
            case "results":
                openResults();
                break;
            case "quit":
                session.quit = true;
                break;
            default:
                break;
        }
    } catch (error) {
        Chat.log("Manual suite error: " + error);
        try {
            Chat.log(String(error && error.stack ? error.stack : error));
        } catch (ignored) {
        }
    }
}

function shutdown() {
    hideHint();
    for (const overlay of session.overlays.slice()) {
        try {
            overlay.unregister();
        } catch (ignored) {
        }
    }
    session.overlays.length = 0;
    if (session.keyListener != null) {
        try {
            session.keyListener.off();
        } catch (ignored) {
        }
        session.keyListener = null;
    }
    // Quit (and cancellation) should not leave the suite screen open.
    try {
        onMain(() => {
            const open = Hud.getOpenScreen();
            if (open != null) {
                open.close();
            }
        });
    } catch (ignored) {
    }
    try {
        chatText("Manual suite stopped.", 0x55FFFF);
    } catch (ignored) {
    }
}

(function bootstrap() {
    try {
        try {
            session.hostScreen = Hud.getOpenScreen();
        } catch (error) {
            session.hostCaptureError = String(error);
        }
        session.keyListener = JsMacros.on("Key", wrap(event => {
            // H has no vanilla binding and EventKey already ignores keys while a
            // suite screen is open, so no cancel is needed here.
            if (event.action === 1 && event.key === "key.keyboard.h") {
                try {
                    openMenu();
                } catch (error) {
                    Chat.log("Manual suite menu failed: " + error);
                }
            }
        }));
        chatText("JsMacrosCE manual tests loaded. Use the chat buttons, or press H for the menu.", 0x55FFFF);
        postPanel(plan());
        handle({ type: "menu" });
        while (!session.quit) {
            Client.waitTick();
            while (session.queue.length > 0) {
                handle(session.queue.shift());
            }
        }
    } catch (error) {
        Chat.log("Manual suite interrupted: " + error);
    } finally {
        shutdown();
    }
})();
