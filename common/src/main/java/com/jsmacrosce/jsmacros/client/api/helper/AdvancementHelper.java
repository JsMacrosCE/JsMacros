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
 * one advancement in the tree, together with where it sits in that tree.
 * <p>
 * The tree is the hierarchy the advancements tab shows: a root with children, each of those with
 * children of its own. This class is a node in it, so it answers two different kinds of question
 * at once. About the advancement itself: {@link #getId()}, {@link #getExperience()},
 * {@link #getRequirements()} and the rewards. And about the tree: {@link #getParent()} and
 * {@link #getChildren()}, which is what a script walks to find a whole branch.
 * <p>
 * How far along the player is on it is a separate object, {@link #getProgress()}, because
 * progress belongs to the player rather than to the advancement.
 * <p>
 * An advancement is reached through the manager, which is
 * {@code Player.getPlayer().getAdvancementManager()}. The manager can be asked for one
 * advancement by identifier or for all of them; a fresh helper is made on each call, so there
 * is nothing to keep alive.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *
 *   // one advancement by identifier, and what it awards
 *   const root = manager.getAdvancement("minecraft:story/root");
 *   Chat.log(`${root.getId()} gives ${root.getExperience()} xp`);
 *   if (root.getLoot().length > 0) {
 *     Chat.log(`and loot: ${root.getLoot().join(", ")}`);
 *   }
 *
 *   // walk the tree downwards from there
 *   for (const child of root.getChildren()) {
 *     Chat.log(`  child ${child.getId()}`);
 *   }
 *
 *   // and how far the player has got on it
 *   const progress = root.getProgress();
 *   Chat.log(`${Math.round(progress.getPercentage() * 100)} percent`);
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


    public AdvancementHelper(AdvancementNode base) {
        super(base);
    }

    /**
     * the advancement this one is drawn as a child of, or {@code null} for a root.
     * <p>
     * This is the tree structure rather than the advancement's own definition, so it answers
     * where this sits in the advancements tab. A root has no parent and gives {@code null}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const parent = manager.getAdvancement("minecraft:story/mine_stone").getParent();
     * if (parent !== null) {
     *   Chat.log(`that one is under ${parent.getId()}`);
     * } else {
     *   Chat.log("that one is a root");
     * }
     * </pre>
     *
     * @return the parent advancement or {@code null} if there is none.
     * @since 1.8.4
     */
    @Nullable
    public AdvancementHelper getParent() {
        return base.parent() == null ? null : new AdvancementHelper(base.parent());
    }

    /**
     * the advancements drawn directly below this one.
     * <p>
     * This is one level of the tree, not the whole branch underneath: a script that wants
     * everything below this walks down level by level from here. A leaf gives an empty list
     * rather than {@code null}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // one level down, then one more from each of those
     * const root = manager.getAdvancement("minecraft:story/root");
     * for (const child of root.getChildren()) {
     *   for (const grandchild of child.getChildren()) {
     *     Chat.log(grandchild.getId());
     *   }
     * }
     * </pre>
     *
     * @return a list of all child advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getChildren() {
        return StreamSupport.stream(base.children().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * the conditions that have to be met, grouped the way the tab counts them.
     * <p>
     * The outer list is the groups and the inner lists are the alternatives within a group.
     * <b>Every</b> group has to be satisfied for the advancement to be done, and <b>any one</b>
     * criterion inside a group satisfies that group. An advancement whose criteria are all
     * independent puts each in a group of its own, and for that shape the rule reduces to
     * "every criterion has to be met". The other shape is a group holding more than one entry,
     * which is an explicit "any of these" in the data, and in the vanilla advancements that
     * shape is the common one rather than the exception: of the 1520 advancement files shipped
     * with 1.21.8, 1409 have at least one such group, and 1326 of those are a single group of
     * exactly two alternatives. A script that wants "any of these" has to read the inner
     * lists rather than assume one criterion per group.
     * <br>
     * The inner lists are the game's own objects, so a script should read them rather than write
     * to them.
     * example:
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
     * how many groups of requirements this advancement has.
     * <p>
     * This is the size of {@link #getRequirements()}, so it counts groups and not criteria.
     * The two are the same number only on an advancement that puts every criterion in a group
     * of its own; where a group holds several criteria as alternatives, one group counts once
     * however many of them the player has met. In the vanilla advancements that second shape
     * is the common one, so the two numbers are frequently different and this one is usually
     * the smaller.
     * <br>
     * It is the denominator of {@link AdvancementProgressHelper#getPercentage()}, so a script
     * working out how far along the player is wants this alongside that call.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * // the groups, which is what is left to do
     * Chat.log(`${adv.getRequirementCount()} group(s) of criteria`);
     * </pre>
     *
     * @return the amount of requirements.
     * @since 1.8.4
     */
    public int getRequirementCount() {
        return base.advancement().requirements().size();
    }

    /**
     * the advancement's identifier, the one it is written as in the data files.
     * <p>
     * This is the same string the manager's {@code getAdvancement} takes, so it round-trips:
     * an id from here can be handed straight back to look the advancement up again.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * for (const adv of manager.getAdvancements()) {
     *   Chat.log(adv.getId());
     * }
     * </pre>
     *
     * @return the identifier of this advancement.
     * @since 1.8.4
     */
    @DocletReplaceReturn("AdvancementId")
    public String getId() {
        return base.holder().id().toString();
    }

    /**
     * the experience this advancement awards when it is earned.
     * <p>
     * This is the number the toast shows. It is a property of the advancement, not of the
     * player, so it is the same whether or not the player has earned it; whether it has been
     * earned is on the progress.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const root = manager.getAdvancement("minecraft:story/root");
     * Chat.log(`${root.getId()} is worth ${root.getExperience()} xp`);
     * </pre>
     *
     * @return the experience awarded by this advancement.
     * @since 1.8.4
     */
    public int getExperience() {
        return base.advancement().rewards().experience();
    }

    /**
     * the loot tables this advancement hands out, as identifiers.
     * <p>
     * An advancement can grant items by way of a loot table rather than by giving them
     * directly, and this is where those tables are named. On a vanilla 1.21.8 server the list
     * is in fact always empty: none of the 1520 advancement files the game ships have a loot
     * reward at all, so this only has something in it when a datapack adds one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/root");
     * const loot = adv.getLoot();
     * if (loot.length > 0) {
     *   Chat.log(`drops ${loot.join(", ")}`);
     * } else {
     *   Chat.log("no loot");
     * }
     * </pre>
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
     * the recipes this advancement unlocks, as identifiers.
     * <p>
     * This is what makes a crafting recipe appear in the recipe book, and it is populated for
     * the large majority of advancements: 1395 of the 1520 vanilla files name at least one
     * recipe. The rest award their items some other way, such as experience or nothing at all,
     * and those give an empty list here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * for (const recipe of adv.getRecipes()) {
     *   Chat.log(`unlocks ${recipe}`);
     * }
     * </pre>
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
     * how far the player has got on this advancement.
     * <p>
     * The progress is the player's, not the advancement's, so this is about the player rather
     * than the tree. It is also the call that ties the two halves of this class together: a
     * script that has just found an advancement through {@link #getChildren()} and now wants to
     * know whether the player has it comes here.
     * <br>
     * The progress is read from the connection's own store, and the store only holds what the
     * server has sent. An advancement the server has not sent progress for gives a progress
     * object with nothing behind it, and the first call that reads it throws.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * const progress = adv.getProgress();
     * if (progress.isDone()) {
     *   Chat.log("already earned");
     * } else if (progress.isAnyObtained()) {
     *   Chat.log(`partly done: ${progress.getPercentage()} percent`);
     *   for (const missing of progress.getUnobtainedCriteria()) {
     *     Chat.log(`  still to do: ${missing}`);
     *   }
     * }
     * </pre>
     *
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
