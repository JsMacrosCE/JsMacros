package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinClientAdvancementManager;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents an advancement definition and its position in the advancement tree. Use
 * {@link #getParent()} and {@link #getChildren()} to navigate the tree; rewards and requirements
 * describe the definition, while {@link #getProgress()} reports local-player progress.
 * <p>
 * Obtain advancements from the player's advancement manager.
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *
 *   // one advancement by identifier, and what it awards
 *   const root = manager.getAdvancement("minecraft:story/root");
 *   if (root.getRaw() !== null) {
 *     Chat.log(`${root.getId()} gives ${root.getExperience()} xp`);
 *     if (root.getLoot().length > 0) {
 *       Chat.log(`and loot: ${root.getLoot().join(", ")}`);
 *     }
 *
 *     // walk the tree downwards from there
 *     for (const child of root.getChildren()) {
 *       Chat.log(`  child ${child.getId()}`);
 *     }
 *
 *     // and how far the player has got on it
 *     const progress = root.getProgress();
 *     if (progress.getRaw() !== null) {
 *       Chat.log(`${Math.round(progress.getPercentage() * 100)} percent`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class AdvancementHelper extends BaseHelper<AdvancementNode> {
    private static final Minecraft mc = Minecraft.getInstance();


    /** Wraps the supplied advancement node.
     *
     * @param base raw advancement node to wrap
     */
    public AdvancementHelper(AdvancementNode base) {
        super(base);
    }

    /**
     * Returns this advancement's parent in the tree, or {@code null} if it is a root.
     *
     * @return the parent advancement or {@code null} if there is none.
     * @since 1.8.4
     */
    @Nullable
    public AdvancementHelper getParent() {
        return base.parent() == null ? null : new AdvancementHelper(base.parent());
    }

    /**
     * Returns this node's direct children, not their descendants. A leaf has no children.
     *
     * @return a list of all child advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getChildren() {
        return StreamSupport.stream(base.children().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns the advancement's criterion requirements grouped by completion rule.
     * <p>
     * Every group must be satisfied; satisfying any one criterion in a group satisfies that group.
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * for (const group of adv.getRequirements()) {
     *   // one of these is enough for this group, and every group is needed
     *   Chat.log(`needs one of: ${group.join(" or ")}`);
     * }
     * </pre>
     *
     * @return the requirements of this advancement.
     * @since 1.8.4
     */
    public List<List<String>> getRequirements() {
        return base.advancement().requirements().requirements();
    }

    /**
     * Returns the number of requirement groups, not the number of individual criteria.
     *
     * @return the amount of requirements.
     * @since 1.8.4
     */
    public int getRequirementCount() {
        return base.advancement().requirements().size();
    }

    /**
     * Returns the advancement's data-pack identifier.
     *
     * @return the identifier of this advancement.
     * @since 1.8.4
     */
    @DocletReplaceReturn("AdvancementId")
    public String getId() {
        return base.holder().id().toString();
    }

    /**
     * Returns the experience points awarded when this advancement is earned.
     *
     * @return the experience awarded by this advancement.
     * @since 1.8.4
     */
    public int getExperience() {
        return base.advancement().rewards().experience();
    }

    /**
     * Returns the identifiers of loot tables granted by this advancement.
     *
     * @return the loot table ids for this advancement's rewards.
     * @since 1.8.4
     */
    public String[] getLoot() {
        return base.advancement().rewards().loot().stream().map(e -> e
                //? if >=1.21.11 {
                /*.identifier()
                *///? } else {
                .location()
                //? }
                .toString()).toArray(String[]::new);
    }

    /**
     * Returns identifiers of recipes unlocked by this advancement; empty when it has no recipe rewards.
     *
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
                //? if >=1.21.11 {
                /*.map(ResourceKey::identifier)
                *///? } else {
                .map(ResourceKey::location)
                //? }
                .map(ResourceLocation::toString)
                .toArray(String[]::new);
    }

    /**
     * Returns the local player's progress for this advancement, from the progress data received
     * from the server. If no progress entry exists, the returned helper wraps {@code null}; its
     * progress methods cannot be used in that state.
     *
     * @return the local player's progress, or a helper wrapping {@code null} if no progress entry exists.
     * @since 1.8.4
     */
    public AdvancementProgressHelper getProgress() {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        return new AdvancementProgressHelper(((MixinClientAdvancementManager) player.connection.getAdvancements()).getAdvancementProgresses().get(base.holder()));
    }

    /**
     * Returns this advancement's definition encoded as a JSON string.
     *
     * @return this advancement's JSON definition.
     * @since 1.9.0
     */
    public String toJson() {
        return Advancement.CODEC.encodeStart(JsonOps.INSTANCE, base.advancement()).getOrThrow().toString();
    }

    /** Returns a diagnostic representation containing this advancement's identifier.
     *
     * @return diagnostic representation of this advancement
     */
    @Override
    public String toString() {
        return String.format("AdvancementHelper:{\"id\": \"%s\"}", getId());
    }

}
