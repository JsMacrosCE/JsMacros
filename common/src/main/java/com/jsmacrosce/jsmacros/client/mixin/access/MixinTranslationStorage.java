package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.extensions.Extension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
@DocletIgnore
public class MixinTranslationStorage {

    /**
     * Adds extension-provided labels to the client's language table.
     * <p>
     * Resource packs win: extension labels are inserted first, then the client's own language
     * files overwrite them, so an extension label remains only where the client has no entry.
     * Between two extensions defining the same key in the same language, the extension the loader
     * registered first wins. Languages are merged in the order Minecraft loads them, so a label
     * from the selected language replaces the English fallback from the same extension.
     */
    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false), method = "loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resources/language/ClientLanguage;", locals = LocalCapture.CAPTURE_FAILHARD)
    private static void insertFabricLanguageData(ResourceManager resourceManager, List<String> definitions, boolean rightToLeft, CallbackInfoReturnable<ClientLanguage> cir, Map<String, String> map) {
        Map<String, String> translations = new LinkedHashMap<>();
        // Loading is cache-backed after the first call, but hoisting the lookup keeps this off
        // the per-language path and makes the ordering contract above explicit.
        List<Extension> extensions = List.copyOf(JsMacrosClient.clientCore.extensions.getAllExtensions());
        for (String lang : definitions) {
            Map<String, String> languageTranslations = new LinkedHashMap<>();
            for (Extension extension : extensions) {
                extension.getTranslations(lang).forEach(languageTranslations::putIfAbsent);
            }
            translations.putAll(languageTranslations);
        }
        translations.forEach(map::putIfAbsent);
    }

}
