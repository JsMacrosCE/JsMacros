package com.jsmacrosce.jsmacros.client.tick;

/**
 * Holds a recorded timestamp and TPS sample.
 * <pre>
 * const TPSData = Java.type("com.jsmacrosce.jsmacros.client.tick.TPSData");
 * const sample = new TPSData(Date.now(), 20);
 * </pre>
 */
public class TPSData {
    /** Timestamp in milliseconds since the Unix epoch when the sample was recorded. */
    public final long recvTime;
    /** Tick rate in ticks per second; may be an instantaneous or averaged sample. */
    public final double tps;

    /** Stores the supplied timestamp and TPS sample without computing or validating them.
     * @param time sample timestamp in milliseconds since the Unix epoch
     * @param tps tick rate in ticks per second
     */
    public TPSData(long time, double tps) {
        this.recvTime = time;
        this.tps = tps;
    }

}
