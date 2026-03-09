package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
*///? } else {

import net.minecraft.world.entity.monster.SpellcasterIllager;
//? }

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(SpellcasterIllager.class)
public interface MixinSpellcastingIllagerEntityHelper {

    // Don't make this static, it will disable the compile and reload feature!
    @Accessor("DATA_SPELL_CASTING_ID")
    EntityDataAccessor<Byte> getSpellKey();

}
