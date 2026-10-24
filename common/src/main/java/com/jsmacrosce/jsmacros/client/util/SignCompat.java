package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import java.util.Arrays;
import java.util.List;
//? if >=26.3 {
/*import net.minecraft.world.level.block.entity.SignTextSlot;
*///?}

@DocletIgnore
public final class SignCompat {
    private SignCompat() {
    }

    public static List<String> messages(SignBlockEntity sign, boolean front, boolean filter) {
        //? if >=26.3 {
        /*return sign.getText(front ? SignTextSlot.FRONT : SignTextSlot.BACK).getMessages(filter)
                .stream().map(Component::getString).toList();
        *///?} else {
        return Arrays.stream(sign.getText(front).getMessages(filter)).map(Component::getString).toList();
        //?}
    }

    public static void setMessages(SignBlockEntity sign, boolean front, List<String> lines) {
        //? if >=26.3 {
        /*SignText.Mutable text = SignText.EMPTY.asMutable();
        for (int i = 0; i < 4; i++) text.setLine(i, Component.nullToEmpty(lines.get(i)));
        sign.setText(text.asImmutable(), front ? SignTextSlot.FRONT : SignTextSlot.BACK);
        *///?} else {
        SignText text = new SignText();
        for (int i = 0; i < 4; i++) text = text.setMessage(i, Component.nullToEmpty(lines.get(i)));
        sign.setText(text, front);
        //?}
        sign.setChanged();
    }

    public static ServerboundSignUpdatePacket update(SignBlockEntity sign, boolean front, List<String> lines) {
        //? if >=26.3 {
        /*return new ServerboundSignUpdatePacket(sign.getBlockPos(), List.copyOf(lines), front ? SignTextSlot.FRONT : SignTextSlot.BACK);
        *///?} else {
        return new ServerboundSignUpdatePacket(sign.getBlockPos(), front, lines.get(0), lines.get(1), lines.get(2), lines.get(3));
        //?}
    }
}
