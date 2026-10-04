package com.jsmacrosce.doclet.core.util;

import java.util.Map;
import java.util.Objects;

public final class ExternalTypeLinksTest {
    public static void main(String[] args) {
        for (String version : new String[] {"1.21.5", "1.21.8", "1.21.10", "1.21.11"}) {
            equal(link("net.minecraft.client.multiplayer", "PlayerInfo", version),
                "https://mappings.dev/" + version + "/net/minecraft/client/multiplayer/PlayerInfo.html");
        }
        for (String version : new String[] {"26.1", "26.1.2", "26.2", "27.1"}) {
            equal(link("net.minecraft.client.multiplayer", "PlayerInfo", version),
                "https://mcsrc.dev/2/" + version + "/net/minecraft/client/multiplayer/PlayerInfo");
        }
        equal(link("net.minecraft.world.phys", "HitResult.Type", "1.21.8"),
            "https://mappings.dev/1.21.8/net/minecraft/world/phys/HitResult$Type.html");
        equal(link("net.minecraft.world.phys", "HitResult$Type", "26.1.2"),
            "https://mcsrc.dev/2/26.1.2/net/minecraft/world/phys/HitResult$Type");
        for (String pkg : new String[] {"com.mojang.blaze3d.systems", "com.mojang.math", "com.mojang.realmsclient.dto"}) {
            equal(link(pkg, "Example", "26.1.2"), "https://mcsrc.dev/2/26.1.2/" + pkg.replace('.', '/') + "/Example");
        }
        for (String pkg : new String[] {"com.mojang.authlib", "com.mojang.brigadier", "com.mojang.serialization", "net.minecraftish"}) {
            equal(link(pkg, "Example", "26.1.2"), null);
        }
        for (String version : new String[] {null, "", "latest", "26.1-snapshot-1", "26.1.2/other"}) {
            equal(link("net.minecraft.client.multiplayer", "PlayerInfo", version), null);
        }
        equal(ExternalTypeLinks.resolve("java.util", "Map.Entry", "26.1.2",
            Map.of("java.util", "https://example.org/api/java/util/")),
            "https://example.org/api/java/util/Map.Entry.html");
        equal(ExternalTypeLinks.resolve("java.util", "Map$Entry", null,
            Map.of("java.util", "https://example.org/api/java/util")),
            "https://example.org/api/java/util/Map.Entry.html");
        equal(ExternalTypeLinks.resolve("com.mojang.authlib", "GameProfile", "26.1.2",
            Map.of("com.mojang.authlib", "https://example.org/api/com/mojang/authlib/")),
            "https://example.org/api/com/mojang/authlib/GameProfile.html");
        equal(ExternalTypeLinks.resolve("net.minecraft.client.multiplayer", "PlayerInfo", "26.1.2",
            Map.of("net.minecraft.client.multiplayer", "https://example.org/explicit/")),
            "https://example.org/explicit/PlayerInfo.html");
        System.out.println("ExternalTypeLinks: 26 regression checks passed");
    }

    private static String link(String pkg, String name, String version) {
        return ExternalTypeLinks.resolve(pkg, name, version, Map.of());
    }

    private static void equal(String actual, String expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
