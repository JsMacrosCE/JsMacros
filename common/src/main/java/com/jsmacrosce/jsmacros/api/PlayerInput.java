package com.jsmacrosce.jsmacros.api;

import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data for one tick of queued player input. Playback uses the signs of the movement axes to select
 * key presses and sets the player's yaw and pitch from this input; fractional magnitudes do not
 * scale movement. Prediction also uses the axis magnitudes and angles. Inputs are copied when
 * queued, so later changes to this object do not affect the queued copy.
 * <pre>
 * // one tick of "hold forward" facing south, level pitch, nothing else pressed
 * const step = Player.createPlayerInput(1, 0, 0, 0, false, false, false);
 * // where that single tick would end up, without adding it to the movement queue
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
     * Forward/back movement input; positive is forward, negative is backward, and zero is neutral.
     * Prediction passes this value through as the forward component of movement input.
     */
    public float movementForward;
    /**
     * Sideways movement input; positive is left, negative is right, and zero is neutral. Prediction
     * passes this value through as the sideways component of movement input.
     */
    public float movementSideways;
    /**
     * Absolute yaw in degrees: 0 faces south, +90 west, 180 north, and -90 east. Playback sets the
     * player's yaw from this value; prediction sets the simulated entity's yaw from it.
     */
    public float yaw;
    /**
     * Absolute pitch in degrees; positive values point down and negative values point up. Playback
     * sets the player's pitch from this value; prediction sets the simulated entity's pitch from it.
     */
    public float pitch;
    /**
     * Whether jump is requested for this tick. In prediction, it only jumps when grounded and
     * its cooldown is zero; holding this value true does not reset the cooldown.
     */
    public boolean jumping;
    /**
     * Whether sneak is requested for this tick. Prediction applies crouch movement scaling based
     * on the dummy's crouching state at the start of the tick, so the scaling lags a tick behind
     * changes to this value.
     */
    public boolean sneaking;
    /**
     * Whether sprint is requested for this tick. Prediction starts sprinting only when this is
     * true, sneaking is false, and the forward axis is positive; an existing sprint ends when the
     * forward axis is not positive or a horizontal collision occurs.
     */
    public boolean sprinting;

    /**
     * Creates an input with all axes and angles set to zero and all key states false.
     *
     * @since 1.4.0
     */
    public PlayerInput() {
        this(0.0F, 0.0F, 0.0F, 0.0F, false, false, false);
    }

    /**
     * Creates an input with the supplied forward and sideways axes and yaw; pitch and key states
     * default to zero/false.
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
     * Creates an input with the supplied forward axis, yaw and key states; sideways movement and
     * pitch default to zero, and sneaking defaults to false. The second argument is yaw, not the
     * sideways axis; use the seven-argument constructor to supply sideways movement.
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
     * Creates an input from double-precision axis and angle values, narrowing them to floats.
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
     * Creates an input with every field explicitly supplied.
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
     * Copies all fields from {@code input} into an independent object.
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
     * Parses CSV rows into inputs. The first line supplies case-sensitive field-name headers;
     * subsequent rows must have the same number of columns or are skipped. Fields omitted from
     * the header retain their defaults. Values are comma-separated, spaces are removed, and
     * quoted CSV values are not supported. For parsed rows, unknown field names throw
     * {@link NoSuchFieldException}, private field names throw {@link IllegalAccessException},
     * and invalid numeric values throw {@link NumberFormatException}. Boolean values are parsed
     * with {@link Boolean#valueOf(String)}.
     * <pre>
     * const csv = `movementForward,movementSideways,yaw,pitch,jumping,sneaking,sprinting
     * 1,0,90,0,false,false,true
     * 1,0,90,0,false,false,true`;
     * // queue two input ticks
     * const steps = Player.createPlayerInputsFromCsv(csv);
     * let i = 0;
     * while (i !== steps.size()) {
     *   Player.addInput(steps.get(i));
     *   i += 1;
     * }
     * </pre>
     *
     * @param csv CSV string to be parsed
     * @return Inputs parsed from rows with the expected number of columns
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
     * Parses a JSON value into a {@code PlayerInput}. Keys use the field names; keys not matching
     * fields are ignored, and omitted fields retain their defaults. The JSON must be a complete
     * object, including braces.
     * <pre>
     * // Parse one input from JSON
     * const step = Player.createPlayerInputsFromJson('{"movementForward": 1, "yaw": 90, "sprinting": true}');
     * Player.addInput(step);
     * </pre>
     *
     * @param json JSON string to be parsed
     * @return The parsed input
     * @since 1.4.0
     */
    public static PlayerInput fromJson(String json) {
        return gson.fromJson(json, PlayerInput.class);
    }

    /**
     * Converts a map of field names and string values into an input; omitted fields retain their
     * defaults. Unknown or private field names throw, and numeric values must parse as floats.
     *
     * @param input Map to be converted
     * @return The converted input
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
     * Returns the fields as comma-separated text. With {@code varNames} true, it returns quoted
     * field names and values (a JSON object body without braces); otherwise, it returns values
     * only (a CSV row without a header). Neither result alone is accepted by the corresponding
     * parser: wrap the named form in braces for {@link #fromJson(String)}, or provide a header for
     * {@link #fromCsv(String)}.
     * @param varNames whether to include field names in the output
     * @return The field representation described above
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

    /**
     * Returns the public fields declared by {@code PlayerInput} as {@code PlayerInput{name=value, ...}}.
     * Field order is not guaranteed.
     * @return A representation of this input's declared public fields
     */
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

    /**
     * Copies the seven movement, rotation, and action fields into a new base {@code PlayerInput};
     * fields added by a subclass are not retained.
     * @return An independent base {@code PlayerInput} copy
     */
    @Override
    public PlayerInput clone() {
        return new PlayerInput(this);
    }

}
