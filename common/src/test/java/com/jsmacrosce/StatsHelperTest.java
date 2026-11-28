package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.helper.StatsHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinStatHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;

// Bootstraps registries only. No client, server, world, or native window is created.
public final class StatsHelperTest {
    private static int checks;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var fly = Stats.CUSTOM.get(Stats.FLY_ONE_CM);
        TestCounter counter = new TestCounter();
        counter.setValue(null, fly, 500_000);
        StatsHelper stats = new StatsHelper(counter);

        check(stats.getCustomStat("minecraft:fly_one_cm") == 500_000, "custom stat raw value");
        check(stats.getCustomStat("fly_one_cm") == 500_000, "custom stat without namespace");
        check(stats.getCustomFormattedStat("minecraft:fly_one_cm").equals(fly.format(500_000)), "custom stat formatter");
        check(stats.hasCustomStat("minecraft:fly_one_cm"), "registered custom stat");
        check(!stats.hasCustomStat("minecraft:not_a_stat"), "unknown custom stat is not registered");

        checkThrows(() -> stats.getCustomStat("minecraft:not_a_stat"), "unknown custom stat count");
        checkIsNull(stats.getCustomFormattedStat("minecraft:not_a_stat"), "unknown custom stat text");
        checkThrows(() -> stats.getBlockMined("minecraft:not_a_block"), "unknown typed stat count");
        checkThrows(() -> stats.getRawStatValue("minecraft.custom:minecraft.not_a_stat"), "unknown keyed stat count");
        checkIsNull(stats.getFormattedStatValue("minecraft.custom:minecraft.not_a_stat"), "unknown keyed stat text");
        checkIsNull(stats.getStatText("minecraft.custom:minecraft.not_a_stat"), "unknown keyed stat category");

        System.out.println("StatsHelper: " + checks + " stat checks passed");
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    private static void checkIsNull(Object value, String message) {
        checks++;
        if (value != null) throw new AssertionError(message + " was not null");
    }

    private static void checkThrows(Runnable action, String message) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message + " did not throw IllegalArgumentException");
    }

    /** Exposes the map normally supplied by the MixinStatHandler mixin, which is not applied here. */
    private static final class TestCounter extends StatsCounter implements MixinStatHandler {
        @Override
        public Object2IntMap<Stat<?>> getStatMap() {
            return stats;
        }
    }
}
