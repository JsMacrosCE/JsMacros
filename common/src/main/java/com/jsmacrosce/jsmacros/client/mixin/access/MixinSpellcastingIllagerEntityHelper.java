package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
*///? } else {

import net.minecraft.world.entity.monster.SpellcasterIllager;
//? }

/** Accesses the synchronized spell ID on a spellcasting illager.
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(SpellcasterIllager.class)
@DocletIgnore
public interface MixinSpellcastingIllagerEntityHelper {

    // Don't make this static, it will disable the compile and reload feature!
    /** Returns the entity-data key used to read the spellcasting illager's synchronized spell ID. */
    @Accessor("DATA_SPELL_CASTING_ID")
    EntityDataAccessor<Byte> getSpellKey();

}
