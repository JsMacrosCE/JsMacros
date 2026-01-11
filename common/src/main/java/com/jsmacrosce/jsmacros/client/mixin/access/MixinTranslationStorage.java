package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ClientLanguage.class)
public class MixinTranslationStorage {

    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false), method = "loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resources/language/ClientLanguage;", locals = LocalCapture.CAPTURE_FAILHARD)
    private static void insertFabricLanguageData(ResourceManager resourceManager, List<String> definitions, boolean rightToLeft, CallbackInfoReturnable<ClientLanguage> cir, Map<String, String> map) {
        Map<String, String> translations = new LinkedHashMap<>();
        for (String lang : definitions) {
            Set<Map<String, String>> res = JsMacrosClient.clientCore.extensions.getAllExtensions().stream().map(e -> e.getTranslations(lang)).collect(Collectors.toSet());
            for (Map<String, String> r : res) {
                translations.putAll(r);
            }
        }
        translations.forEach(map::putIfAbsent);
    }

}
