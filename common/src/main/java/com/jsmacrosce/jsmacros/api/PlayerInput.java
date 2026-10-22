package com.jsmacrosce.jsmacros.api;

import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One tick's worth of player input, bundled up: how far the two movement axes are pushed, the two
 * view angles, and the three state keys. A script builds these, hands them to
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer the Player library}, and the
 * movement queue plays them back one per client tick, so twenty of them is a second of scripted
 * walking. This is a plain data holder: nothing here talks to the game.<br>
 * Every field is public and writable, so an input can be built up after the fact, and the
 * constructors that leave values out just default them to {@code 0} or {@code false}. Build one
 * empty and assign the fields when the values are only known later, which is what the
 * {@code Player.moveForward} family does.<br>
 * What the queue makes of a queued input is narrower than the fields suggest, and it is worth
 * knowing before scripting against it. The playback path only ever asks each field a yes or no
 * question: the two movement axes are compared against {@code 0} to decide whether the forward,
 * back, left and right keys are faked as pressed, and whatever magnitude sits in between is
 * thrown away, so {@code 0.5} and {@code 1} walk identically and only {@code 0} differs, by
 * standing still, which is also what stops a sprint. The three booleans are handed straight
 * through as key presses. The two view angles are not applied to the player at all, so neither
 * of them turns or tilts them; {@code yaw} and {@code pitch} say what they do reach instead.<br>
 * The angles are absolute rather than offsets from wherever the player is currently looking, so
 * {@code 0} on the yaw means south and not "keep facing the same way". The
 * {@code Player.moveForward} family adds the player's current yaw for you before queueing, which
 * is the relative form of that, but since the queued yaw is not what steers the player those
 * calls are in practice a shorthand for "hold forward" in whichever direction they already face.<br>
 * An input is copied when it is queued, so changing one afterwards has no effect on what the queue
 * plays back. That is deliberate, and it means you can safely reuse the same object for a whole
 * run of ticks.
 * example:
 * <pre>
 * // one tick of "hold forward", level pitch, nothing else pressed.
 * // the yaw here says south, and the queue walks whichever way the player is
 * // already facing, so this only steers the prediction on the next line
 * const step = Player.createPlayerInput(1, 0, 0, 0, false, false, false);
 * // where that single tick would end up, without actually taking it
 * Chat.log(`one tick of walking puts me at ${Player.predictInput(step)}`);
 *
 * // a second of walking is the same input twenty times over
 * for (let tick = 0; tick !== 20; tick += 1) {
 *   Player.addInput(step);
 * }
 * </pre>
 * @author NotSomeBot
 * @since 1.4.0
 */
@SuppressWarnings("unused")
public class PlayerInput {
    private static final Gson gson = new Gson();
    /**
     * how hard the player is pushed forwards or backwards, in the range the game itself uses for
     * that axis: {@code 1} is holding the forward key, {@code -1} is holding the back key and
     * {@code 0} is neither.<br>
     * Only the sign of this reaches the player. The playback path compares it against {@code 0}
     * to decide whether the forward or the back key is faked as pressed, and the game then walks
     * at its own walking speed, so a value in between walks exactly as fast as {@code 1} rather
     * than at a fraction of it: a half speed walk is not something this field can ask for today.
     * The magnitude is only honoured by the prediction dummy, which scales the axis by
     * {@code 0.98} and walks with it, so a prediction of a fractional walk is not what the player
     * would do.<br>
     * {@code 0} is also what stops a sprint, since a sprint only survives while this axis is
     * still pushing forwards.
     */
    public float movementForward;
    /**
     * how hard the player is pushed left or right, in the same way as
     * {@code movementForward}: {@code 1} is holding the left key, {@code -1} is
     * holding the right key and {@code 0} is neither. As with that field, only the sign reaches
     * the player, so a value in between strafes at the same speed as {@code 1} rather than at a
     * fraction of it.
     */
    public float movementSideways;
    /**
     * the absolute yaw the input's movement is resolved against, in degrees, in the game's own
     * convention: {@code 0} is south, {@code 90} is west, {@code -90} is east and {@code 180} is
     * north. It is absolute rather than an offset from wherever the player is looking, so a value
     * of {@code 0} always means south and never "keep facing the same way". The relative form
     * lives in {@code Player.moveForward} and the rest, which add the player's current yaw for
     * you before queueing the input.<br>
     * What it reaches today is narrower than it looks. The movement queue only takes its sign: it
     * compares the two movement axes against {@code 0} to decide which key presses to fake, and
     * resolves the direction of travel against the player's real, current yaw rather than this
     * one, so setting this does not turn the player and the direction of travel does not come
     * from it. Nothing in the playback path ever assigns this yaw to the player, so a relative
     * yaw is not relative to anything either and those calls walk the player in whatever
     * direction they are already facing. The one consumer of this field is the prediction dummy
     * that {@code Player.predictInput} runs, which does set its own rotation to it, so the value
     * is still worth filling in to keep the prediction honest, but a prediction is a guess: it
     * will disagree with where the player actually goes whenever this does not match the player's
     * own yaw, and the queue logs that difference in its debug output when it happens.
     */
    public float yaw;
    /**
     * the absolute pitch the input is recorded with, in degrees, in the game's own convention:
     * negative is up, positive is down, and {@code 0} is level.<br>
     * The movement queue never applies it. The player keeps whatever pitch they are already at,
     * since nothing in the playback path assigns a pitch, and so this does not change where a
     * queued input takes them. It is still worth setting, since
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#getCurrentPlayerInput()
     * getCurrentPlayerInput()} reads it back out of the real player, and it is what a queued
     * input's own {@code toString} and {@code fromJson} round trip preserves.
     */
    public float pitch;
    /**
     * whether the jump key is held for this tick. A jump only comes out of it when the player is
     * on the ground, and after one fires there is a short cooldown during which further ticks with
     * this set do nothing, so repeating the same input does not produce a jump every tick.
     */
    public boolean jumping;
    /**
     * whether the sneak key is held for this tick, which the movement queue turns into a faked
     * shift key press so that the game itself lowers the player. It is read as the state the
     * player should be in rather than as an edge, so it applies from the first tick it is set and
     * stays until a tick sets it back.<br>
     * The prediction dummy also scales both movement axes down by {@code 0.3} for a crouch, and
     * that part lags by one tick: the scale is applied against whether the dummy was already
     * crouching when the tick began, not against this field, so a predicted crouch is not slowed
     * on the tick it starts and still is on the tick it ends. That is a detail of the crouch
     * state rather than of this field, which is set and read at the right time either way.
     */
    public boolean sneaking;
    /**
     * whether the player is sprinting for this tick. Starting a sprint needs this set, the
     * movement forward axis actually pushing forwards, and no sneaking, and once a sprint is going
     * it keeps going only while the forward axis is still pushing, so setting this on an input
     * that stands still does nothing.
     */
    public boolean sprinting;

    /**
     * Creates a new {@code PlayerInput} Object with all values set either to 0 or false.
     * <br>
     * The other constructor that takes every value is
     * {@code PlayerInput(movementForward, movementSideways, yaw, pitch, jumping, sneaking, sprinting)},
     * which takes seven arguments in that order.
     *
     * @since 1.4.0
     */
    public PlayerInput() {
        this(0.0F, 0.0F, 0.0F, 0.0F, false, false, false);
    }

    /**
     * Creates a new {@code PlayerInput} Object with all other values set either to 0 or false.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              absolute yaw of the player, in degrees
     * @since 1.4.0
     */
    public PlayerInput(float movementForward, float movementSideways, float yaw) {
        this(movementForward, movementSideways, yaw, 0.0, false, false, false);
    }

    /**
     * Creates a new {@code PlayerInput} Object with all other values set either to 0 or false.<br>
     * Note that the second argument here is the yaw, not the sideways axis, so the sideways axis
     * of the result is {@code 0}. Use
     * {@code PlayerInput(movementForward, movementSideways, yaw, pitch, jumping, sneaking, sprinting)}
     * when a sideways push is wanted as well.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param yaw             absolute yaw of the player, in degrees
     * @param jumping         jump input
     * @param sprinting       sprint input
     * @since 1.4.0
     */
    public PlayerInput(float movementForward, float yaw, boolean jumping, boolean sprinting) {
        this(movementForward, 0.0F, yaw, 0.0, jumping, false, sprinting);
    }

    /**
     * Creates a new {@code PlayerInput} Object with all double values converted to floats.<br>
     * This is the same shape as the seven argument {@code float} constructor, with the four
     * numbers taken as {@code double}s so that a value computed in script does not have to be
     * narrowed first.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              absolute yaw of the player, in degrees
     * @param pitch            absolute pitch of the player, in degrees
     * @param jumping          jump input
     * @param sneaking         sneak input
     * @param sprinting        sprint input
     * @since 1.4.0
     */
    public PlayerInput(double movementForward, double movementSideways, double yaw, double pitch, boolean jumping, boolean sneaking, boolean sprinting) {
        this((float) movementForward, (float) movementSideways, (float) yaw, (float) pitch, jumping, sneaking, sprinting);
    }

    /**
     * Creates a new {@code PlayerInput} Object, taking every value.<br>
     * This is the one to reach for when every field is known up front; the shorter constructors
     * exist to leave the fields a script does not care about at their defaults.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              absolute yaw of the player, in degrees
     * @param pitch            absolute pitch of the player, in degrees
     * @param jumping          jump input
     * @param sneaking         sneak input
     * @param sprinting        sprint input
     * @since 1.4.0
     */
    public PlayerInput(float movementForward, float movementSideways, float yaw, float pitch, boolean jumping, boolean sneaking, boolean sprinting) {
        this.movementForward = movementForward;
        this.movementSideways = movementSideways;
        this.yaw = yaw;
        this.pitch = pitch;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
    }

    /**
     * Creates a clone {@code PlayerInput} Object, field for field.<br>
     * The result is an ordinary object, so writing to it afterwards does not touch {@code input}.
     * This is what the movement queue uses when an input is queued, which is why a queued input
     * cannot be changed by editing the object that was handed over.
     *
     * @param input the {@code PlayerInput} object to be cloned
     * @since 1.4.0
     */
    public PlayerInput(PlayerInput input) {
        this.movementForward = input.movementForward;
        this.movementSideways = input.movementSideways;
        this.yaw = input.yaw;
        this.pitch = input.pitch;
        this.jumping = input.jumping;
        this.sneaking = input.sneaking;
        this.sprinting = input.sprinting;
    }

    /**
     * Parses each row of CSV string into a {@code PlayerInput}.
     * The capitalization of the header matters.<br>
     * About the columns:
     * <ul>
     *   <li> {@code movementForward} and {@code movementSideways} as a number</li>
     *   <li>{@code yaw} and {@code pitch} as an absolute number</li>
     *   <li>{@code jumping}, {@code sneaking} and {@code sprinting} have to be boolean</li>
     * </ul>
     * <p>
     * The separation must be a "," it's a csv...(but spaces don't matter)<br>
     * Quoted values don't work
     * <br>
     * The first line is always read as the header, so it names the columns rather than
     * describing an input of its own, and every later line has to have exactly as many commas as
     * that header does. A line that does not is quietly skipped rather than reported, so a
     * malformed row disappears from the result instead of raising anything.<br>
     * The values are read by looking up a field of this class by the header's name, so the header
     * has to spell a field exactly and a name that does not is an error rather than a skipped
     * column. A number that is not one is a runtime error too, while a boolean that is not one
     * reads as {@code false}, since only the text {@code true} counts as true. A column left out of
     * the header entirely simply keeps its default on every input that comes back.
     * example:
     * <pre>
     * const csv = `movementForward,movementSideways,yaw,pitch,jumping,sneaking,sprinting
     * 1,0,90,0,false,false,true
     * 1,0,90,0,false,false,true`;
     * // two ticks of sprinting to the west
     * const steps = Player.createPlayerInputsFromCsv(csv);
     * let i = 0;
     * while (i !== steps.size()) {
     *   Player.addInput(steps.get(i));
     *   i += 1;
     * }
     * </pre>
     *
     * @param csv CSV string to be parsed
     * @return {@code List<PlayerInput>} Each row parsed as a {@code PlayerInput}
     * @throws NoSuchFieldException if a header names something on this class that is not a field
     *                              at all
     * @throws IllegalAccessException if a header names a private field, which is the one field on
     *                                this class a script may not set
     * @since 1.4.0
     */
    public static List<PlayerInput> fromCsv(String csv) throws NoSuchFieldException, IllegalAccessException {
        String[] rows = csv.replace(" ", "").split("\n");
        List<PlayerInput> output = new ArrayList<>();
        String[] headers = rows[0].split(",");
        String[] row;
        Map<String, String> mappedRow;
        for (int rowNo = 1; rowNo < rows.length; rowNo++) {
            row = rows[rowNo].split(",");
            if (row.length == headers.length) {
                mappedRow = new HashMap<>();
                for (int i = 0; i < row.length; i++) {
                    mappedRow.put(headers[i], row[i]);
                }
                output.add(fromMap(mappedRow));
            }
        }
        return output;
    }

    /**
     * Parses a JSON string into a {@code PlayerInput} Object<br>
     * Capitalization of the keys matters.
     * <br>
     * The names are the field names of this class, and a key that names nothing is ignored rather
     * than reported, so a document with a field left out simply comes back with that field at its
     * default. This reads a whole JSON object, so the text has to include the enclosing braces,
     * which the {@link #toString(boolean) varNames} form of {@code toString} does not.
     * example:
     * <pre>
     * // the Player library's name is plural but it hands back a single input
     * const step = Player.createPlayerInputsFromJson('{"movementForward": 1, "yaw": 90, "sprinting": true}');
     * Player.addInput(step);
     * </pre>
     *
     * @param json JSON string to be parsed
     * @return The JSON parsed into a {@code PlayerInput}
     * @since 1.4.0
     */
    public static PlayerInput fromJson(String json) {
        return gson.fromJson(json, PlayerInput.class);
    }

    /**
     * Converts a Map keyed by this class's field names into a {@code PlayerInput}, starting from
     * an all default input so that any field the map leaves out keeps its default.
     * Reads the same field names {@link #fromCsv(String) fromCsv()} does, one parsed row at a
     * time, so this is where a CSV's header and commas turn into the map this takes.
     *
     * @param input Map to be converted
     * @return The map converted into a {@code PlayerInput}
     * @since 1.4.0
     */
    private static PlayerInput fromMap(Map<String, String> input) throws NoSuchFieldException, IllegalAccessException {
        PlayerInput playerInput = new PlayerInput();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            Field field = PlayerInput.class.getDeclaredField(entry.getKey());
            if (Modifier.isPrivate(field.getModifiers())) {
                throw new IllegalAccessException();
            }
            if (float.class.isAssignableFrom(field.getType())) {
                field.set(playerInput, Float.valueOf(entry.getValue()));
            } else if (boolean.class.isAssignableFrom(field.getType())) {
                field.set(playerInput, Boolean.valueOf(entry.getValue()));
            }
        }
        return playerInput;
    }

    /**
     * Converts the current object into a string, as a run of {@code name: value} pairs separated
     * by {@code ", "} over all seven fields, in the order the JVM reports them, which is normally
     * the order they are declared.<br>
     * With {@code varNames} set the field names are included, and the result is the body of a JSON
     * object rather than a whole one: it has no braces around it, so it has to be wrapped in
     * {@code { }} before {@link #fromJson(String) fromJson()} will read it. With {@code varNames}
     * clear the names are left out and the result is a single CSV row with no header, which
     * {@link #fromCsv(String) fromCsv()} also cannot read on its own since it always treats the
     * first line as the header.<br>
     * In both cases the floats are written as plain numbers, so the text reads back fine; the
     * booleans come out as the words {@code true} and {@code false}.<br>
     * This is the way to save an input taken from
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#getCurrentPlayerInput()
     * getCurrentPlayerInput()} and feed it back in later, in either of those two forms. That call
     * reads straight off the local player behind a plain assert, so it throws outside a world
     * rather than handing back an empty input, and the guard belongs on the caller.
     * example:
     * <pre>
     * // getCurrentPlayerInput() dereferences the local player, so there is
     * // nothing to save outside a world
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const saved = Player.getCurrentPlayerInput();
     *   // the pairs, ready to drop between a pair of braces
     *   print(`{${saved.toString(true)}}`);
     *   // one CSV row, ready to go under a header
     *   print(`movementForward,movementSideways,yaw,pitch,jumping,sneaking,sprinting\n${saved.toString(false)}`);
     * }
     * </pre>
     *
     * @param varNames whether to include variable Names(=JSON) or not(=CSV)
     * @return The {@code PlayerInput} object as a string
     * @since 1.4.0
     */
    public String toString(boolean varNames) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(Gson.class)) {
                continue;
            }
            field.setAccessible(true);

            if (varNames) {
                stringBuilder.append("\"");
                stringBuilder.append(field.getName());
                stringBuilder.append("\": ");
            }

            try {
                if (field.getType().equals(float.class)) {
                    stringBuilder.append(field.getFloat(this));
                } else {
                    stringBuilder.append(field.get(this).toString());
                }

            } catch (IllegalAccessException ignored) {
            }
            stringBuilder.append(", ");
        }
        stringBuilder.setLength(stringBuilder.length() - 2);
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlayerInput{");
        String prefix = "";
        for (Field field : PlayerInput.class.getDeclaredFields()) {
            if (Modifier.isPrivate(field.getModifiers())) {
                continue;
            }
            try {
                stringBuilder
                        .append(prefix)
                        .append(field.getName())
                        .append("=")
                        .append(field.get(this));
                prefix = ", ";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    @Override
    public PlayerInput clone() {
        return new PlayerInput(this);
    }

}
