package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.*;

/**
 * Lets scripts identify entity helpers through the {@code EntityHelpers} global,
 * without knowing their Java package names.
 * Checks follow helper inheritance, not entity registry IDs. These are Java types,
 * so their existing constructors and static members are also available. The group
 * members are read-only. Listing a type here does not change which helper a factory returns.
 * <pre>
 * const entities = World.getEntities();
 * if (entities !== null) {
 *   for (const ent of entities) {
 *     if (ent instanceof EntityHelpers.Horse) {
 *       Chat.log(ent.getVariant());
 *     }
 *   }
 * }
 * </pre>
 */
@ScriptNamespace("EntityHelpers")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class EntityHelpers {
    private EntityHelpers() {
    }

    // Scripts depend on these field names, so keep them when Java classes are moved or renamed.
    public static final Class<EntityHelper> Entity = EntityHelper.class;
    public static final Class<LivingEntityHelper> Living = LivingEntityHelper.class;
    public static final Class<MobEntityHelper> Mob = MobEntityHelper.class;
    public static final Class<PlayerEntityHelper> Player = PlayerEntityHelper.class;
    public static final Class<ClientPlayerEntityHelper> ClientPlayer = ClientPlayerEntityHelper.class;
    public static final Class<MerchantEntityHelper> Merchant = MerchantEntityHelper.class;
    public static final Class<VillagerEntityHelper> Villager = VillagerEntityHelper.class;
    public static final Class<ItemEntityHelper> Item = ItemEntityHelper.class;
    public static final Class<EnderDragonEntityHelper> EnderDragon = EnderDragonEntityHelper.class;
    public static final Class<WitherEntityHelper> Wither = WitherEntityHelper.class;
    public static final Class<ArmorStandEntityHelper> ArmorStand = ArmorStandEntityHelper.class;
    public static final Class<EndCrystalEntityHelper> EndCrystal = EndCrystalEntityHelper.class;
    public static final Class<ItemFrameEntityHelper> ItemFrame = ItemFrameEntityHelper.class;
    public static final Class<PaintingEntityHelper> Painting = PaintingEntityHelper.class;
    public static final Class<DisplayEntityHelper> Display = DisplayEntityHelper.class;
    public static final Class<BlockDisplayEntityHelper> BlockDisplay = BlockDisplayEntityHelper.class;
    public static final Class<ItemDisplayEntityHelper> ItemDisplay = ItemDisplayEntityHelper.class;
    public static final Class<TextDisplayEntityHelper> TextDisplay = TextDisplayEntityHelper.class;
    public static final Class<AbstractPiglinEntityHelper> AbstractPiglin = AbstractPiglinEntityHelper.class;
    public static final Class<BlazeEntityHelper> Blaze = BlazeEntityHelper.class;
    public static final Class<CreeperEntityHelper> Creeper = CreeperEntityHelper.class;
    public static final Class<DrownedEntityHelper> Drowned = DrownedEntityHelper.class;
    public static final Class<EndermanEntityHelper> Enderman = EndermanEntityHelper.class;
    public static final Class<GhastEntityHelper> Ghast = GhastEntityHelper.class;
    public static final Class<GuardianEntityHelper> Guardian = GuardianEntityHelper.class;
    public static final Class<IllagerEntityHelper> Illager = IllagerEntityHelper.class;
    public static final Class<PhantomEntityHelper> Phantom = PhantomEntityHelper.class;
    public static final Class<PiglinEntityHelper> Piglin = PiglinEntityHelper.class;
    public static final Class<PillagerEntityHelper> Pillager = PillagerEntityHelper.class;
    public static final Class<ShulkerEntityHelper> Shulker = ShulkerEntityHelper.class;
    public static final Class<SlimeEntityHelper> Slime = SlimeEntityHelper.class;
    public static final Class<SpellcastingIllagerEntityHelper> SpellcastingIllager = SpellcastingIllagerEntityHelper.class;
    public static final Class<SpiderEntityHelper> Spider = SpiderEntityHelper.class;
    public static final Class<VexEntityHelper> Vex = VexEntityHelper.class;
    public static final Class<VindicatorEntityHelper> Vindicator = VindicatorEntityHelper.class;
    public static final Class<WardenEntityHelper> Warden = WardenEntityHelper.class;
    public static final Class<WitchEntityHelper> Witch = WitchEntityHelper.class;
    public static final Class<ZombieEntityHelper> Zombie = ZombieEntityHelper.class;
    public static final Class<ZombieVillagerEntityHelper> ZombieVillager = ZombieVillagerEntityHelper.class;
    public static final Class<AnimalEntityHelper> Animal = AnimalEntityHelper.class;
    public static final Class<TameableEntityHelper> Tameable = TameableEntityHelper.class;
    // Including parent helpers lets scripts check for any horse-like helper, not just one entity ID.
    public static final Class<AbstractHorseEntityHelper> AbstractHorse = AbstractHorseEntityHelper.class;
    public static final Class<HorseEntityHelper> Horse = HorseEntityHelper.class;
    public static final Class<DonkeyEntityHelper> Donkey = DonkeyEntityHelper.class;
    public static final Class<LlamaEntityHelper> Llama = LlamaEntityHelper.class;
    public static final Class<AllayEntityHelper> Allay = AllayEntityHelper.class;
    public static final Class<AxolotlEntityHelper> Axolotl = AxolotlEntityHelper.class;
    public static final Class<BatEntityHelper> Bat = BatEntityHelper.class;
    public static final Class<BeeEntityHelper> Bee = BeeEntityHelper.class;
    public static final Class<CatEntityHelper> Cat = CatEntityHelper.class;
    public static final Class<DolphinEntityHelper> Dolphin = DolphinEntityHelper.class;
    public static final Class<FishEntityHelper> Fish = FishEntityHelper.class;
    public static final Class<FoxEntityHelper> Fox = FoxEntityHelper.class;
    public static final Class<FrogEntityHelper> Frog = FrogEntityHelper.class;
    public static final Class<GoatEntityHelper> Goat = GoatEntityHelper.class;
    public static final Class<IronGolemEntityHelper> IronGolem = IronGolemEntityHelper.class;
    public static final Class<MooshroomEntityHelper> Mooshroom = MooshroomEntityHelper.class;
    public static final Class<OcelotEntityHelper> Ocelot = OcelotEntityHelper.class;
    public static final Class<PandaEntityHelper> Panda = PandaEntityHelper.class;
    public static final Class<ParrotEntityHelper> Parrot = ParrotEntityHelper.class;
    public static final Class<PigEntityHelper> Pig = PigEntityHelper.class;
    public static final Class<PolarBearEntityHelper> PolarBear = PolarBearEntityHelper.class;
    public static final Class<PufferfishEntityHelper> Pufferfish = PufferfishEntityHelper.class;
    public static final Class<RabbitEntityHelper> Rabbit = RabbitEntityHelper.class;
    public static final Class<SheepEntityHelper> Sheep = SheepEntityHelper.class;
    public static final Class<SnowGolemEntityHelper> SnowGolem = SnowGolemEntityHelper.class;
    public static final Class<StriderEntityHelper> Strider = StriderEntityHelper.class;
    public static final Class<TropicalFishEntityHelper> TropicalFish = TropicalFishEntityHelper.class;
    public static final Class<WolfEntityHelper> Wolf = WolfEntityHelper.class;
    public static final Class<ArrowEntityHelper> Arrow = ArrowEntityHelper.class;
    public static final Class<FishingBobberEntityHelper> FishingBobber = FishingBobberEntityHelper.class;
    public static final Class<TridentEntityHelper> Trident = TridentEntityHelper.class;
    public static final Class<WitherSkullEntityHelper> WitherSkull = WitherSkullEntityHelper.class;
    public static final Class<BoatEntityHelper> Boat = BoatEntityHelper.class;
    public static final Class<FurnaceMinecartEntityHelper> FurnaceMinecart = FurnaceMinecartEntityHelper.class;
    public static final Class<TntMinecartEntityHelper> TntMinecart = TntMinecartEntityHelper.class;
    public static final Class<AreaEffectCloudEntityHelper> AreaEffectCloud = AreaEffectCloudEntityHelper.class;
    public static final Class<FallingBlockEntityHelper> FallingBlock = FallingBlockEntityHelper.class;
    public static final Class<InteractionEntityHelper> Interaction = InteractionEntityHelper.class;
    public static final Class<TntEntityHelper> Tnt = TntEntityHelper.class;
}
