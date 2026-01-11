package com.jsmacrosce.jsmacros.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Ensures Sponge Mixin remaps the refmap from Fabric's intermediary namespace to Mojmap when running on NeoForge.
 */
public final class JSMacrosMixinPlugin implements IMixinConfigPlugin {

    private static final String FABRIC_LOADER_CLASS = "net.fabricmc.loader.api.FabricLoader";
    private static final String NEOFORGE_DIST_PROP = "fml.neoForgeVersion";

    @Override
    public void onLoad(String mixinPackage) {
        if (isRunningOnFabric()) {
            return;
        }

        setIfAbsent("mixin.env.remapRefMap", "true");
        setIfAbsent("mixin.env.refMapRemappingEnv", "intermediary");
        setIfAbsent("mixin.env.refMapRemappingDestinationEnv", resolveDestinationNamespace());
        setIfAbsent("mixin.env.refMapRemappingForce", "true");
        setIfAbsent("mixin.env.disableRefMap", "false");
        setIfAbsent("mixin.env.obfuscationContext", resolveDestinationNamespace());

        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        env.setObfuscationContext(resolveDestinationNamespace());
        env.setOption(MixinEnvironment.Option.REFMAP_REMAP, true);
        env.setOption(MixinEnvironment.Option.DISABLE_REFMAP, false);
    }

    private static boolean isRunningOnFabric() {
        try {
            Class.forName(FABRIC_LOADER_CLASS, false, JSMacrosMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static String resolveDestinationNamespace() {
        if (System.getProperty(NEOFORGE_DIST_PROP) != null) {
            return "official";
        }
        return "named";
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // no-op
    }

    @Override
    public @Nullable List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }
}
