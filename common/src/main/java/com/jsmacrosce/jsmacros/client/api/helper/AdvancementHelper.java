package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinClientAdvancementManager;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AdvancementHelper extends BaseHelper<AdvancementNode> {
    private static final Minecraft mc = Minecraft.getInstance();


    public AdvancementHelper(AdvancementNode base) {
        super(base);
    }

    /**
     * @return the parent advancement or {@code null} if there is none.
     * @since 1.8.4
     */
    @Nullable
    public AdvancementHelper getParent() {
        return base.parent() == null ? null : new AdvancementHelper(base.parent());
    }

    /**
     * @return a list of all child advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getChildren() {
        return StreamSupport.stream(base.children().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * @return the requirements of this advancement.
     * @since 1.8.4
     */
    public List<List<String>> getRequirements() {
        return base.advancement().requirements().requirements();
    }

    /**
     * @return the amount of requirements.
     * @since 1.8.4
     */
    public int getRequirementCount() {
        return base.advancement().requirements().size();
    }

    /**
     * @return the identifier of this advancement.
     * @since 1.8.4
     */
    @DocletReplaceReturn("AdvancementId")
    public String getId() {
        return base.holder().id().toString();
    }

    /**
     * @return the experience awarded by this advancement.
     * @since 1.8.4
     */
    public int getExperience() {
        return base.advancement().rewards().experience();
    }

    /**
     * @return the loot table ids for this advancement's rewards.
     * @since 1.8.4
     */
    public String[] getLoot() {
        return base.advancement().rewards().loot().stream().map(e -> e.location().toString()).toArray(String[]::new);
    }

    /**
     * @return the recipes unlocked through this advancement.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaArray<RecipeId>")
    public String[] getRecipes() {
        return base
                .advancement()
                .rewards()
                .recipes()
                .stream()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .toArray(String[]::new);
    }

    /**
     * @return the progress.
     * @since 1.8.4
     */
    public AdvancementProgressHelper getProgress() {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        return new AdvancementProgressHelper(((MixinClientAdvancementManager) player.connection.getAdvancements()).getAdvancementProgresses().get(base.holder()));
    }

    /**
     * @since 1.9.0
     * @return the json string of this advancement.
     */
    public String toJson() {
        return Advancement.CODEC.encodeStart(JsonOps.INSTANCE, base.advancement()).getOrThrow().toString();
    }

    @Override
    public String toString() {
        return String.format("AdvancementHelper:{\"id\": \"%s\"}", getId());
    }

}
