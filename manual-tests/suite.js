Client.runOnMainThread(JavaWrapper.methodToJava(() => {
    const results = {};
    let entryScreen = null;
    let hostCaptureError = null;
    try {
        entryScreen = Hud.getOpenScreen();
    } catch (error) {
        hostCaptureError = String(error);
    }
    let cleanup = () => {};
    const wrap = callback => JavaWrapper.methodToJava(callback);

    const TESTS = [
        {
            id: "SCREEN-OPEN-001",
            category: "Screen",
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
            title: "Escape and Pause",
            setup(screen, state) {
                screen.drawTitle = false;
                screen.shouldCloseOnEsc = false;
                screen.shouldPause = false;
                screen.addText("No title should appear above this line.", 24, 46, 0xFFFFFF, false);
                screen.addText("Press Esc: this screen must remain open. In a world, it must not pause the game.", 24, 66, 0xFFFFFF, false);
                screen.addText("Use Back when the check is complete.", 24, 86, 0xFFFFFF, false);
                check(state, !screen.drawTitle && !screen.shouldCloseOnEsc && !screen.shouldPause, "Screen flags accepted their configured values");
            }
        },
        {
            id: "SCREEN-INPUT-001",
            category: "Input",
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
            id: "WIDGETS-DIRECT-001",
            category: "Widgets",
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
                check(state, screen.getButtonWidgets().length >= 4, "Builder widgets were added to the screen");
                check(state, screen.getTextFields().length >= 1, "Builder text field was added to the screen");
            }
        },
        {
            id: "RENDER-PRIMITIVES-001",
            category: "Rendering",
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
            title: "Draw2D Overlay",
            setup(screen, state) {
                const status = screen.addText("Cyan preview proves Draw2D in-screen. Register then close to see the global HUD overlay at the top-left.", 24, 46, 0xFFFFFF, false);
                const overlay = Hud.createDraw2D();
                overlay.setOnInit(wrap(draw => draw.addText("Manual suite overlay", 8, 8, -11141121, true)));
                screen.draw2DBuilder(overlay).pos(220, 72).size(180, 24).buildAndAdd();
                state.keepOverlayOnce = false;
                state.cleanup = () => {
                    if (state.keepOverlayOnce) {
                        state.keepOverlayOnce = false;
                        return;
                    }
                    overlay.unregister();
                };
                screen.addButton(24, 72, 160, 20, "Register Overlay", wrap(() => {
                    overlay.register();
                    const registered = Hud.listDraw2Ds().contains(overlay);
                    check(state, registered, "Registered overlay is listed by Hud.listDraw2Ds()");
                    status.setText(registered ? "Overlay registered. Back unregisters it; the close button keeps it visible." : "Overlay registration was not recorded.");
                }));
                screen.addButton(24, 98, 160, 20, "Register and Close", wrap(() => {
                    overlay.register();
                    check(state, Hud.listDraw2Ds().contains(overlay), "Registered overlay is listed before closing");
                    state.keepOverlayOnce = true;
                    status.setText("Closing; the overlay stays registered so it is visible on the HUD.");
                    screen.close();
                }));
                screen.addButton(24, 124, 160, 20, "Unregister Overlay", wrap(() => {
                    overlay.unregister();
                    const unregistered = !Hud.listDraw2Ds().contains(overlay);
                    check(state, unregistered, "Unregistered overlay is absent from Hud.listDraw2Ds()");
                    status.setText(unregistered ? "Overlay unregistered." : "Overlay was still listed after unregistering.");
                }));
                screen.addButton(24, 150, 160, 20, "Clear All Overlays", wrap(() => {
                    Hud.clearDraw2Ds();
                    const cleared = Hud.listDraw2Ds().size() === 0;
                    check(state, cleared, "Hud.clearDraw2Ds() removed all registered overlays");
                    status.setText(cleared ? "All registered overlays cleared." : "Overlays remained after clearing.");
                }));
            }
        },
        {
            id: "TEXT-INTERACTION-001",
            category: "Text",
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
            title: "Host Screen",
            setup(screen, state) {
                const name = entryScreen == null ? "None" : String(entryScreen.getScreenClassName());
                screen.addText("Screen captured before opening the suite: " + name, 24, 46, 0xFFFFFF, false);
                screen.addText("Run the suite from a vanilla screen such as inventory to test screen wrapping and return navigation.", 24, 66, 0xFFFFFF, false);
                if (entryScreen == null) {
                    state.manualOnly = true;
                    screen.addText(hostCaptureError == null ? "No host screen was captured. This run is not applicable." : "Host capture was unavailable: " + hostCaptureError, 24, 86, 0xFFFFFF, false);
                } else {
                    check(state, String(entryScreen.getScreenClassName()).length > 0, "Captured host screen exposes its class name");
                    screen.addButton(24, 98, 180, 20, "Return to Host Screen", wrap(() => Hud.openScreen(entryScreen)));
                }
            }
        }
    ];

    function check(state, passed, label) {
        state.checks.push({ passed: !!passed, label: label });
    }

    function automaticCount(state) {
        return state.checks.filter(check => check.passed).length + "/" + state.checks.length;
    }

    function resultLabel(test) {
        const result = results[test.id];
        return result == null ? "Not checked" : result.outcome + " (" + result.automatic + ")";
    }

    function runCleanup() {
        const current = cleanup;
        cleanup = () => {};
        try {
            current();
        } catch (error) {
            Chat.log("Manual suite cleanup failed: " + error);
        }
    }

    function openScreen(title, init) {
        runCleanup();
        const screen = Hud.createScreen(title, true);
        screen.setOnClose(wrap(() => runCleanup()));
        screen.setOnInit(wrap(() => init(screen)));
        Hud.openScreen(screen);
        return screen;
    }

    function centeredButton(screen, y, label, callback) {
        const width = 180;
        screen.addButton(Math.floor((screen.getWidth() - width) / 2), y, width, 20, label, wrap(callback));
    }

    function categories() {
        return [...new Set(TESTS.map(test => test.category))];
    }

    function openHome() {
        openScreen("Manual Tests", screen => {
            screen.textBuilder("Select a category, a specific test, or run the available tests in sequence.")
                .pos(0, 46).color(0xFFFFFF).shadow(false).alignHorizontally("center").buildAndAdd();
            centeredButton(screen, 72, "Run All Tests", () => openTest(TESTS.map((test, index) => index), 0, openHome));
            let y = 100;
            for (const category of categories()) {
                centeredButton(screen, y, category, () => openCategory(category));
                y += 24;
            }
            centeredButton(screen, y + 8, "Results", () => openResults(openHome));
        });
    }

    function openCategory(category) {
        const indexes = TESTS.map((test, index) => test.category === category ? index : -1).filter(index => index >= 0);
        openScreen(category, screen => {
            screen.addText("Select a test, or run this category in sequence.", 24, 46, 0xFFFFFF, false);
            centeredButton(screen, 72, "Run " + category, () => openTest(indexes, 0, () => openCategory(category)));
            let y = 100;
            for (const index of indexes) {
                centeredButton(screen, y, TESTS[index].title, () => openTest([index], 0, () => openCategory(category)));
                y += 24;
            }
            centeredButton(screen, y + 8, "Back", openHome);
        });
    }

    function openTest(indexes, position, back) {
        const test = TESTS[indexes[position]];
        const state = { checks: [] };
        const screen = openScreen(test.title, current => {
            test.setup(current, state);
            addTestControls(current, test, state, indexes, position, back);
        });
        cleanup = () => {
            if (state.cleanup != null) {
                state.cleanup();
            }
        };
    }

    function addTestControls(screen, test, state, indexes, position, back) {
        const controlsY = screen.getHeight() - 50;
        const automatic = automaticCount(state);
        const result = resultLabel(test);
        screen.addText("Automatic checks: " + automatic + "   Result: " + result, 24, controlsY - 22, 0xFFFFFF, false);
        screen.addButton(24, controlsY, 100, 20, "Mark Passed", wrap(() => {
            results[test.id] = { outcome: state.manualOnly ? "Skipped" : "Passed", automatic: automaticCount(state) };
            openTest(indexes, position, back);
        }));
        screen.addButton(132, controlsY, 100, 20, "Mark Failed", wrap(() => {
            results[test.id] = { outcome: "Failed", automatic: automaticCount(state) };
            openTest(indexes, position, back);
        }));
        screen.addButton(240, controlsY, 100, 20, "Clear Result", wrap(() => {
            delete results[test.id];
            openTest(indexes, position, back);
        }));
        if (position > 0) {
            centeredButton(screen, controlsY - 48, "Previous", () => openTest(indexes, position - 1, back));
        }
        if (position + 1 < indexes.length) {
            centeredButton(screen, controlsY - 24, "Next", () => openTest(indexes, position + 1, back));
        } else {
            centeredButton(screen, controlsY - 24, "Results", () => openResults(back));
        }
        centeredButton(screen, controlsY - 72, "Back", back);
    }

    function openResults(back, page) {
        const currentPage = page == null ? 0 : page;
        const pageSize = 15;
        const first = currentPage * pageSize;
        const last = Math.min(first + pageSize, TESTS.length);
        openScreen("Results", screen => {
            let y = 46;
            screen.addText("Results " + (currentPage + 1) + "/" + Math.ceil(TESTS.length / pageSize), 24, y, 0xFFFFFF, false);
            y += 20;
            for (let index = first; index < last; index++) {
                const test = TESTS[index];
                screen.addText(test.id + "  " + test.title + ": " + resultLabel(test), 24, y, 0xFFFFFF, false);
                y += 20;
            }
            const controlsY = screen.getHeight() - 50;
            if (currentPage > 0) {
                centeredButton(screen, controlsY - 24, "Previous Page", () => openResults(back, currentPage - 1));
            }
            if (last < TESTS.length) {
                centeredButton(screen, controlsY, "Next Page", () => openResults(back, currentPage + 1));
            }
            centeredButton(screen, controlsY - 48, "Back", back);
        });
    }

    openHome();
}));
