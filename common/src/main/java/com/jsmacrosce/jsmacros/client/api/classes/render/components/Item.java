package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
*///? } else {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
//? }

//? if >=1.21.10 <26.1 {
/*import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemRenderer;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemStackRenderState;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemStackRenderStateLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=1.21.11 {
/^import net.minecraft.client.renderer.rendertype.RenderType;
^///?} else {
import net.minecraft.client.renderer.RenderType;
//?}
import java.util.List;
*///? }

 /**
 * an item icon, drawn the way the game draws one in an inventory slot.
 * <p>
 * The icon is always a 16 by 16 box whatever size the element reports, which is what
 * the game's own slots use and the reason the scaled width here is the size multiplied
 * by 16 rather than by anything the item itself says. There is no width to set: an item
 * is a fixed square.
 * <p>
 * The overlay is the set of things drawn over the icon by the game, which is a durability
 * or energy bar, the cooldown overlay, and either the stack count or a text of the
 * script's choosing. It is off unless something turns it on.
 * <p>
 * The colour on this element is not used for tinting an item the way it is for a
 * rectangle. What a 2D element can do about a plain icon is scale, move and turn it, and
 * the item's own model supplies the rest.
 * <p>
 * On a surface in the world, rather than on a 2D overlay, this element has a separate
 * path: the model carries a depth that has to be flattened, and the
 * {@code render3D} taking a {@code PoseStack} is what handles it. {@link Text} has a
 * separate path too, for its own reasons, so this is not the only element with one. On
 * a 2D overlay the normal path is used.
 * <p>
 * The other {@code render3D}, the one taking a {@code GuiGraphics}, is where a
 * 1.21.5-era flattening used to happen. On 1.21.8 it does not, because the two branches
 * that the {@code is3dRender} flag picks between are the same call: the matrix stack
 * from 1.21.5 on is 2D and has no z axis to collapse.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Item implements RenderElement, Alignable<Item> {

    private static final int DEFAULT_ITEM_SIZE = 16;
    private static final float FLAT_ITEM_DEPTH_SCALE = 0.001f;
    private static final float OVERLAY_TEXT_Z_OFFSET = 0.001f;
    private static final Minecraft mc = Minecraft.getInstance();

    //? if >=26.1 {
    /*private static SubmitNodeStorage directItemStorage;
    private static final ItemFeatureRenderer DIRECT_ITEM_RENDERER = new ItemFeatureRenderer();
    *///? }

    /**
    * the overlay this icon was added to, and {@code null} when it has not been put on
    * one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the stack whose icon is drawn, and {@code null} for an icon that draws nothing.
    * <p>
    * A public field, so it can be written directly. The icon is a fixed 16 by 16 box
    * whatever is in the stack, so nothing else about the element is derived from it.
    */
    public ItemStack item;
    /**
    * the text drawn over the icon when the overlay is on, or {@code null} for the
    * stack count instead.
    * <p>
    * The constructors leave this null, so the count is what shows, and a builder starts
    * it as an empty string rather than null, which is a different thing to ask for.
    */
    public String ovText;
    /**
    * whether the game's own decorations are drawn over this icon.
    * <p>
    * Off on an icon made directly unless the constructor was told otherwise, and off on
    * a builder until an {@code overlayText} or an {@code overlayVisible} call.
    */
    public boolean overlay;
    /**
    * the scale applied to this icon, and one is what an icon comes with from a
    * builder. Made through the constructor there is no default at all, since the
    * scale is one of its arguments.
    * <p>
    * The icon is a 16 by 16 box, so a scale of two draws it as a 32 by 32 box. A
    * negative scale is accepted on the element itself and refused by its builder.
    */
    public double scale;
    /**
    * the rotation in degrees, wrapped into a single turn.
    * <p>
    * Only the setter folds it, not the constructor, so an icon made at 450 reads 450 and
    * one set to 450 afterwards reads 90.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of the icon or about its position.
    * <p>
    * False on an icon made directly and true on one from a builder, so the two turn
    * differently at the same angle.
    */
    public boolean rotateCenter;
    /**
    * the x position of this icon, which is the top left of its 16 by 16 box.
    */
    public int x;
    /**
    * the y position of this icon, which is the top of its 16 by 16 box.
    * <p>
    * The counterpart of {@code x}, and read the same way.
    */
    public int y;
    /**
    * the number this icon is ordered against the other elements on its overlay, lower
    * drawn first.
    */
    public int zIndex;

    /**
    * makes an item icon from an id and a count of one.
    * <p>
    * The id goes through the usual parser, so a bare name gets the {@code minecraft}
    * namespace. An id that does not resolve to anything in the game's item registry
    * does not fail: the registry has a default entry and hands that back, so a mistyped
    * id gives the default item rather than nothing.
    * <p>
    * The rotation is stored as given here rather than folded into a single turn, which
    * is the one place in this class where an angle is not reduced.
    *
    * @param x        the x position
    * @param y        the y position
    * @param zIndex   the z-index against the other elements on the overlay
    * @param id       the item to draw, with the namespace optional
    * @param overlay  whether to draw the game's decorations over the icon
    * @param scale    the scale to apply
    * @param rotation the rotation in degrees, stored as given
    */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: float")
    public Item(int x, int y, int zIndex, String id, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, new ItemStackHelper(id, 1), overlay, scale, rotation);
    }

    /**
    * makes an item icon from a stack, with no text of its own over it.
    * <p>
    * The overlay text is left null, so with the overlay turned on what shows over the
    * icon is the stack count when it is more than one.
    *
    * @param x        the x position
    * @param y        the y position
    * @param zIndex   the z-index against the other elements on the overlay
    * @param i        the stack whose icon to draw
    * @param overlay  whether to draw the game's decorations over the icon
    * @param scale    the scale to apply
    * @param rotation the rotation in degrees, stored as given
    */
    public Item(int x, int y, int zIndex, ItemStackHelper i, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, i, overlay, scale, rotation, null);
    }

    /**
    * makes an item icon from a stack, with text of its own to go over it.
    * <p>
    * This is the only constructor that can set the overlay text, and the text is shown
    * in place of the stack count rather than in addition to it. A null text here is the
    * same as not calling this one.
    *
    * @param x         the x position
    * @param y         the y position
    * @param zIndex    the z-index against the other elements on the overlay
    * @param itemStack the stack whose icon to draw
    * @param overlay   whether to draw the game's decorations over the icon
    * @param scale     the scale to apply
    * @param rotation  the rotation in degrees, stored as given
    * @param ovText    the text to draw over the icon, or {@code null} for the count
    */
    public Item(int x, int y, int zIndex, ItemStackHelper itemStack, boolean overlay, double scale, float rotation, String ovText) {
        this.x = x;
        this.y = y;
        this.item = itemStack.getRaw();
        this.overlay = overlay;
        this.scale = scale;
        this.rotation = rotation;
        this.zIndex = zIndex;
        this.ovText = ovText;
    }

    /**
    * puts a different stack on this icon, or empties it.
    * <p>
    * The stack is used as it is rather than copied, so the icon and the helper share
    * one stack and a change to one is a change to both. A null helper is not ignored
    * here as it is on the builder: it clears the icon, and an icon with nothing in it
    * draws nothing at all.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * const ItemStackHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper");
    * icon.setItem(new ItemStackHelper("minecraft:emerald", 1));
    * draw.register();
    * </pre>
    *
    * @param i the stack to draw, or {@code null} for nothing
    * @return self for chaining.
    * @since 1.0.5 [citation needed]
    */
    public Item setItem(ItemStackHelper i) {
        if (i != null) {
            this.item = i.getRaw();
        } else {
            this.item = null;
        }
        return this;
    }

    /**
    * puts a different stack on this icon from an id and a count.
    * <p>
    * The stack is built fresh here, so there is nothing shared with anything else. The
    * id goes through the usual parser, so a bare name gets the {@code minecraft}
    * namespace, and an id that resolves to nothing in the item registry does not fail:
    * the registry's default entry is handed back instead, which for the vanilla item
    * registry is air. So a mistyped id gives an air icon rather than an error.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * draw.register();
    * icon.setItem("minecraft:emerald", 5);
    * </pre>
    *
    * @param id    the item to draw, with the namespace optional
    * @param count the stack size, which is what the overlay puts over the icon when it
    * is on and the overlay text on this icon is still null, as it is on an icon made
    * through a constructor and not on one from a builder
    * @return self for chaining.
    * @since 1.0.5 [citation needed]
    */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, count: int")
    public Item setItem(String id, int count) {
        this.item = new ItemStack(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id)), count);
        return this;
    }

    /**
    * the stack this icon is drawing, as a helper.
    * <p>
    * Wrapped rather than copied, so this is the same stack the icon is using and a
    * change to it changes what is drawn. The builder's own getter is the opposite and
    * hands back a copy.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
    * draw.register();
    * Chat.log(`a stack of ${icon.getItem().getCount()}, empty: ${icon.getItem().isEmpty()}`);
    * </pre>
    *
    * @return the stack being drawn, as a helper
    * @since 1.0.5 [citation needed]
    */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(item);
    }

    /**
    * moves this icon along the x axis, and leaves the y axis alone.
    * <p>
    * The stack is untouched, so this only changes where the 16 by 16 box is drawn.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(0, 0).buildAndAdd();
    * icon.setX(40);
    * draw.register();
    * </pre>
    *
    * @param x the new x position of this element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setX(int x) {
        this.x = x;
        return this;
    }

    /**
    * the x position of this icon, which is the left of its 16 by 16 box.
    * <p>
    * Unchanged by the scale, which makes the box bigger rather than moving it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(40, 20).buildAndAdd();
    * draw.register();
    * Chat.log(`box starts at ${icon.getX()}, ${icon.getY()} and is ${icon.getScaledWidth()} wide`);
    * </pre>
    *
    * @return the x position of this element.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * moves this icon along the y axis, and leaves the x axis alone.
    * <p>
    * The counterpart of {@code setX}, and the stack is untouched.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(0, 0).buildAndAdd();
    * icon.setY(40);
    * draw.register();
    * </pre>
    *
    * @param y the new y position of this element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setY(int y) {
        this.y = y;
        return this;
    }

    /**
    * the y position of this icon, which is the top of its 16 by 16 box.
    * <p>
    * The counterpart of {@code getX}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(40, 20).buildAndAdd();
    * draw.register();
    * Chat.log(`box starts at ${icon.getX()}, ${icon.getY()}`);
    * </pre>
    *
    * @return the y position of this element.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * puts this icon at a position, and leaves the stack alone.
    * <p>
    * Both coordinates are set and nothing is re-measured, since the icon is always a
    * 16 by 16 box.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(0, 0).buildAndAdd();
    * icon.setPos(40, 20);
    * draw.register();
    * </pre>
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    * @since 1.0.5
    */
    public Item setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
    * scales this icon, and one is what an icon comes with.
    * <p>
    * The icon is a 16 by 16 box, so the drawn size is this multiplied by 16. Only zero
    * is refused: a negative scale is accepted here and left in, which turns the box
    * about as well as shrinking it, whereas the builder refuses anything at or below
    * zero, so the same number can be legal on one and not the other.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * icon.setScale(2);
    * draw.register();
    * Chat.log(`drawn ${icon.getScaledWidth()} wide`);
    * </pre>
    *
    * @param scale the scale to apply
    * @return self for chaining.
    * @throws IllegalArgumentException if the scale is zero
    * @since 1.2.6
    */
    public Item setScale(double scale) throws IllegalArgumentException {
        if (scale == 0) {
            throw new IllegalArgumentException("Scale can't be 0");
        }
        this.scale = scale;
        return this;
    }

    /**
    * the scale on this icon, which is one unless something has changed it.
    * <p>
    * A double, and the number the 16 by 16 box is multiplied by.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * icon.setScale(1.5);
    * draw.register();
    * Chat.log(`scale ${icon.getScale()}, drawn ${icon.getScaledWidth()} wide`);
    * </pre>
    *
    * @return the scale of this item.
    * @since 1.8.4
    */
    public double getScale() {
        return scale;
    }

    /**
    * turns this icon by an angle in degrees.
    * <p>
    * The angle is folded into a single turn, so 450 reads back as 90. The constructor
    * does not fold it, so an icon made at 450 reads 450 until this is called.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(20, 20).buildAndAdd();
    * icon.setRotation(45);
    * draw.register();
    * Chat.log(`reads back as ${icon.getRotation()}`);
    * </pre>
    *
    * @param rotation the angle in degrees
    * @return self for chaining.
    * @since 1.2.6
    */
    public Item setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * the rotation on this icon in degrees, folded into a single turn.
    * <p>
    * Between -180 and 180 once a setter has been used, and whatever was given if the
    * icon was only ever made through a constructor or a builder, since those do not
    * fold it either.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(20, 20).rotation(450).buildAndAdd();
    * draw.register();
    * Chat.log(`reads back as ${icon.getRotation()}`);
    * </pre>
    *
    * @return the rotation of this item.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of the icon or about its
    * position.
    * <p>
    * An icon made directly turns about its position and an icon from a builder turns
    * about the middle, so this is what makes the two agree. The middle is half of the
    * 16 by 16 box, so it does not move when the scale changes.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(20, 20).buildAndAdd();
    * icon.setRotateCenter(true).setRotation(90);
    * draw.register();
    * </pre>
    *
    * @param rotateCenter whether the item should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * whether the rotation on this icon is about its middle.
    * <p>
    * False on an icon made directly and true on one from a builder, since only the
    * builder sets it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(20, 20).buildAndAdd();
    * draw.register();
    * Chat.log(`turns about the middle: ${icon.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this item should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * chooses whether the game's own decorations are drawn over this icon.
    * <p>
    * This is the whole of the set of things the game draws over an icon: the durability
    * or energy bar, the cooldown overlay, and either the stack count or the text of the
    * script's choosing.
    * <p>
    * The text is a separate thing from this flag, and which of the two ends up drawn
    * follows from how the icon was made rather than from this. The constructors leave
    * the text null, which is what asks for the stack count, while a builder starts it
    * as an empty string rather than null, which is a different thing to ask for and
    * draws an empty label in place of the count. An icon from a builder therefore needs
    * an {@code overlayText} or a {@code setOverlayText} call as well as this one to put
    * anything over it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
    * // a builder starts the text as an empty string, so it is set here to show a number
    * icon.setOverlayText("5");
    * icon.setOverlay(true);
    * draw.register();
    * </pre>
    *
    * @param overlay whether to draw the decorations
    * @return self for chaining.
    * @since 1.2.0
    */
    public Item setOverlay(boolean overlay) {
        this.overlay = overlay;
        return this;
    }

    /**
    * whether the game's decorations are drawn over this icon, which is to say the
    * durability or energy bar, the cooldown overlay, and the overlay text or item
    * count.
    * <p>
    * The source comment said only the bar and the text or count. Checked against the
    * game, the game's own decorations call also draws the cooldown overlay, so that is
    * a third thing this turns on, and it has been added here rather than taken out.
    * <p>
    * The bar only appears on a stack that has one, and the text only when there is text
    * or a count above one, so turning this on does not guarantee that anything is drawn
    * over a plain stack of one, and an icon from a builder draws an empty label rather
    * than the count until the text is set on it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
    * draw.register();
    * Chat.log(`overlay on: ${icon.shouldShowOverlay()}`);
    * </pre>
    *
    * @return {@code true}, if the overlay, i.e. the durability bar, and the overlay text or
    * item count should be shown, {@code false} otherwise.
    * @since 1.8.4
    */
    public boolean shouldShowOverlay() {
        return overlay;
    }

    /**
    * sets the text drawn over this icon, without turning the overlay on.
    * <p>
    * The text replaces the stack count rather than sitting beside it, so an icon with
    * this set shows this and not the count. Nothing is drawn until the overlay is on as
    * well, which is why this does not set it: a script can set the text and then decide
    * separately whether the bar should be there.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
    * icon.setOverlayText("5");
    * icon.setOverlay(true);
    * draw.register();
    * </pre>
    *
    * @param ovText the text to draw over the icon
    * @return self for chaining.
    * @since 1.2.0
    */
    public Item setOverlayText(String ovText) {
        this.ovText = ovText;
        return this;
    }

    /**
    * the text that would be drawn over this icon, or {@code null} for the stack count.
    * <p>
    * A constructor leaves this null and a builder starts it as an empty string, so the
    * two kinds of element answer this differently before anything is set. The icon does
    * not care what it is set to while the overlay is off.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
    * draw.register();
    * Chat.log(`overlay text is ${icon.getOverlayText()}`);
    * </pre>
    *
    * @return the overlay text of this item.
    * @since 1.8.4
    */
    public String getOverlayText() {
        return ovText;
    }

    /**
    * sets the number this icon is ordered against the other elements on its overlay,
    * lower drawn first.
    * <p>
    * Two icons drawn in the same place are ordered by this and by nothing else, and a
    * tie keeps the order they were added in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const under = draw.itemBuilder().item("minecraft:diamond").pos(0, 0).buildAndAdd();
    * const over = draw.itemBuilder().item("minecraft:emerald").pos(0, 0).buildAndAdd();
    * // both sit in the same spot, so this is what decides which one draws on top
    * over.setZIndex(1);
    * draw.register();
    * Chat.log(`${under.getZIndex()} under, ${over.getZIndex()} over`);
    * </pre>
    *
    * @param zIndex the new z-index for this item
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * the number this icon is ordered against the other elements on its overlay.
    * <p>
    * Zero is what an icon comes with, and the overlay sorts by this lowest first.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * icon.setZIndex(3);
    * draw.register();
    * Chat.log(`z-index ${icon.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * draws this icon, and is called by the overlay rather than by a script.
    * <p>
    * The element's own transform is put on first, so the position, the scale and the
    * turn are those of the icon as it lands rather than of the box it was placed by,
    * and the game then draws the item into it. When the overlay is on, the game's own
    * decorations are drawn through the same transform, so the count and the bar are
    * turned and scaled along with the icon.
    * <p>
    * An icon with nothing in it draws nothing and returns before touching the context.
    * The mouse arguments are not used: they are the real mouse on a screen and zero on a
    * plain overlay, and neither is looked at here.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * // the overlay calls render on it; a script does not
    * draw.register();
    * </pre>
    *
    * @param drawContext the graphics context to draw into
    * @param mouseX      the mouse x, not used by this element
    * @param mouseY      the mouse y, not used by this element
    * @param delta       the frame delta, not used by this element
    */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta, false);
    }

    @Override
    @DocletIgnore
    public void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta, true);
    }

    private void renderDamageBar3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        if (!item.isDamageableItem()) {
            return;
        }

        int maxDamage = item.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }

        int damage = item.getDamageValue();
        if (damage <= 0) {
            return;
        }

        float durability = Math.max(0.0F, (float) (maxDamage - damage) / (float) maxDamage);
        int barWidth = Mth.clamp(Math.round(13.0F * durability), 0, 13);
        int barColor = 0xFF000000 | Mth.hsvToRgb(durability / 3.0F, 1.0F, 1.0F);

        matrixStack.pushPose();
        //? if >=26.1 {
        /*matrixStack.translate(0, 0, OVERLAY_TEXT_Z_OFFSET);
        *///? }
        new Rect(2, 13, 15, 15, 0xFF000000, 0, 0).render3D(matrixStack, consumers, light, seeThrough, delta);
        if (barWidth > 0) {
            new Rect(2, 13, 2 + barWidth, 14, barColor, 0, 0).render3D(matrixStack, consumers, light, seeThrough, delta);
        }
        matrixStack.popPose();
    }

    @DocletIgnore
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta, boolean is3dRender) {
        if (item == null) {
            return;
        }

        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*var matrices = drawContext.pose();
        matrices.pushPose();
        *///?}

        // TODO: This looks like it wasn't properly updated between 1.21.5 and 1.21.7, why does this pass matrices?
        setupMatrix(matrices, x, y, (float) scale, rotation, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, rotateCenter);
        Font textRenderer = Minecraft.getInstance().font;
        if (is3dRender) {
            // The item model has a z offset (100, and 200 for its decorations) that must
            // be collapsed so the item lies in the surface plane: translate by
            // FLAT_ITEM_DEPTH_SCALE * 100, render, then undo. Keep the scale large enough
            // to avoid z-fighting for deep items like anvils. The 1.21.5+ Matrix3x2fStack
            // is 2D and has no z axis, so those items are already flat.
            //? if >1.21.5 {
            //? if >=26.1 {
            /*drawContext.item(item, x, y);
            *///? } else {
            drawContext.renderItem(item, x, y);
            //? }
            //?} else {
            /*matrices.translate(0, 0, -0.1f);
            matrices.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            drawContext.renderItem(item, x, y);
            matrices.scale(1, 1, 1 / FLAT_ITEM_DEPTH_SCALE);
            *///?}
        } else {
            //? if >=26.1 {
            /*drawContext.item(item, x, y);
            *///?} else {
            drawContext.renderItem(item, x, y);
            //?}
        }
        if (overlay) {
            // The decorations carry a z offset of 200; the PoseStack path pushes them in
            // front of the item. The 1.21.5+ 2D stack has no z axis, so nothing is needed.
            //? if <=1.21.5 {
            /*if (is3dRender) {
                matrices.translate(0, 0, -199.5);
            }
            *///?}
            //? if >=26.1 {
            /*drawContext.itemDecorations(mc.font, item, x, y, ovText);
            *///?} else {
            drawContext.renderItemDecorations(mc.font, item, x, y, ovText);
            //?}
        }

        //? if >1.21.5 {
        matrices.popMatrix();
        //?} else {
        /*matrices.popPose();
        *///?}
    }

    // Draws the item into the surface's world-space buffer source. Items cannot be
    // gizmos, so a surface draws them directly. The transform mirrors the 2D path:
    // cancel the surface's Y-flip, map the unit model to a 16px slot, flatten depth.
    @DocletIgnore
    @Override
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        if (item == null || item.isEmpty()) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.translate(x, y, 0);
        matrixStack.scale((float) scale, (float) scale, 1);
        if (rotateCenter) {
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-DEFAULT_ITEM_SIZE / 2d, -DEFAULT_ITEM_SIZE / 2d, 0);
        }

        //? if >=26.1 {
        /*if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
            ItemStackRenderState renderState = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.GUI, mc.level, mc.player, 0);

            SubmitNodeStorage storage = directItemStorage;
            if (storage == null) {
                storage = directItemStorage = new SubmitNodeStorage();
            } else {
                storage.clear();
            }

            matrixStack.pushPose();
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
            matrixStack.scale(1, -1, 1);
            matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
            matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            renderState.submit(matrixStack, storage, light, OverlayTexture.NO_OVERLAY, 0);
            matrixStack.popPose();

            SubmitNodeCollection collection = storage.order(0);
            // outlineColor is 0 above, so the feature renderer never dereferences this.
            DIRECT_ITEM_RENDERER.renderSolid(collection, bufferSource, null);
            DIRECT_ITEM_RENDERER.renderTranslucent(collection, bufferSource, null);
            // Item render types are fixed buffers that only flush on endBatch; flush now
            // so the overlay text draws on top of the item instead of behind it.
            bufferSource.endBatch();
        }
        *///? } else if >=1.21.10 {
        /*ItemStackRenderState renderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.GUI, mc.level, mc.player, 0);
        MixinItemRenderer itemRenderer = (MixinItemRenderer) mc.getItemRenderer();
        MixinItemStackRenderState stateAccessor = (MixinItemStackRenderState) (Object) renderState;
        ItemStackRenderState.LayerRenderState[] layers = stateAccessor.jsmacros$getLayers();
        int layerCount = stateAccessor.jsmacros$getActiveLayerCount();
        for (int i = 0; i < layerCount; i++) {
            MixinItemStackRenderStateLayer layerAccessor = (MixinItemStackRenderStateLayer) (Object) layers[i];
            matrixStack.pushPose();
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
            matrixStack.scale(1, -1, 1);
            matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
            matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            ItemTransform transform = layerAccessor.jsmacros$getTransform();
            if (transform != null) {
                transform.apply(false, matrixStack.last());
            }
            RenderType renderType = layerAccessor.jsmacros$getRenderType();
            List<BakedQuad> quads = layerAccessor.jsmacros$getQuads();
            if (renderType != null && quads != null && !quads.isEmpty()) {
                ItemStackRenderState.FoilType foilType = layerAccessor.jsmacros$getFoilType();
                itemRenderer.jsmacros$renderItem(ItemDisplayContext.GUI, matrixStack, consumers, light, OverlayTexture.NO_OVERLAY,
                        layerAccessor.jsmacros$getTintLayers(), quads, renderType,
                        foilType != null ? foilType : ItemStackRenderState.FoilType.NONE);
            }
            matrixStack.popPose();
        }
        *///? } else {
        matrixStack.pushPose();
        matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
        matrixStack.scale(1, -1, 1);
        matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
        matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
        mc.getItemRenderer().renderStatic(item, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, matrixStack, consumers, null, 0);
        matrixStack.popPose();
        //? }

        if (overlay) {
            renderDamageBar3D(matrixStack, consumers, light, seeThrough, delta);
            String text = ovText != null ? ovText : (item.getCount() > 1 ? String.valueOf(item.getCount()) : null);
            if (text != null) {
                float tx = DEFAULT_ITEM_SIZE + 1 - mc.font.width(text);
                float ty = 9;
                matrixStack.pushPose();
                //? if >=26.1 {
                /*matrixStack.translate(0, 0, OVERLAY_TEXT_Z_OFFSET);
                *///? }
                mc.font.drawInBatch(text, tx, ty, 0xFFFFFFFF, true, matrixStack.last().pose(), consumers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
                matrixStack.popPose();
            }
        }
        matrixStack.popPose();
    }

    /**
    * binds this icon to the overlay it belongs to, which is what the parent width and
    * height are then read from.
    * <p>
    * The element builders call this as they build, so an icon that has been built is
    * already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window, and {@code null} puts it back
    * to the window.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).build();
    * // a built icon is already bound, and can be bound again by hand
    * icon.setParent(draw);
    * draw.reAddElement(icon);
    * draw.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Item setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * the size of this icon as it is drawn, which is the 16 by 16 box put through the
    * scale.
    * <p>
    * There is no width to set on an item, so this is 16 multiplied by the scale and cut
    * to a whole number of pixels. A scale of one gives 16 and a scale of two gives 32.
    * The cut is toward zero, so a positive scale rounds down and a negative one, which
    * {@code setScale} allows on an element, rounds up. This is the number the align
    * methods measure.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * icon.setScale(2);
    * draw.register();
    * Chat.log(`drawn ${icon.getScaledWidth()} wide and ${icon.getScaledHeight()} tall`);
    * </pre>
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    /**
    * the width of the overlay this icon is on, or the window's width if it has not been
    * put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so an icon that
    * has not been added still aligns against the screen.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * draw.register();
    * Chat.log(`measured against ${icon.getParentWidth()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * the height of this icon as it is drawn, which is the 16 by 16 box put through the
    * scale.
    * <p>
    * The counterpart of {@code getScaledWidth}, and the same number, because an item
    * icon is always square.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * icon.setScale(2);
    * draw.register();
    * Chat.log(`drawn ${icon.getScaledHeight()} tall`);
    * </pre>
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    /**
    * the height of the overlay this icon is on, or the window's height if it has not
    * been put on one.
    * <p>
    * The counterpart of {@code getParentWidth}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * draw.register();
    * Chat.log(`measured against ${icon.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the x position of this icon, which is the left of its 16 by 16 box.
    * <p>
    * The same as {@code getX}: a scale makes the box bigger rather than moving it, so
    * the width the icon covers is {@code getScaledWidth} measured from here.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(40, 20).buildAndAdd();
    * draw.register();
    * Chat.log(`from ${icon.getScaledLeft()} to ${icon.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * the y position of this icon, which is the top of its 16 by 16 box.
    * <p>
    * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(40, 20).buildAndAdd();
    * draw.register();
    * Chat.log(`from ${icon.getScaledTop()} to ${icon.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * puts this icon at a position, which is the same as {@code setPos}.
    * <p>
    * Nothing is resized, since an item icon is a fixed 16 by 16 box whatever else is
    * true of it. This is what the align methods call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.itemBuilder().item("minecraft:diamond").pos(0, 0).buildAndAdd();
    * icon.moveTo(40, 20);
    * draw.register();
    * Chat.log(`moved to ${icon.getX()}, ${icon.getY()}`);
    * </pre>
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Item moveTo(int x, int y) {
        return setPos(x, y);
    }

    /**
    * the builder for an item icon, made by the overlay's own {@code itemBuilder}
    * method.
    * <p>
    * The item can be given in three ways, as a stack, as an id for a single item, or as
    * an id with a count, and the two id forms are typed slightly differently: the
    * one-argument form's annotation lets the namespace be left off while the
    * two-argument form's asks for a full id. Both go through the same parser at runtime,
    * so a bare name works on both and the difference is in the typing rather than in
    * what happens.
    * <p>
    * The position starts at zero, the stack starts empty and so draws nothing, the
    * overlay text starts as an empty string rather than null, the overlay starts off,
    * the scale starts at one, the rotation starts at zero and is not folded into a
    * single turn, and the rotation is about the middle of the icon until
    * {@code rotateCenter} says otherwise. That last one is the opposite of an icon made
    * directly.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
    * draw.register();
    * </pre>
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
    public static final class Builder extends RenderElementBuilder<Item> implements Alignable<Builder> {
        private int x = 0;
        private int y = 0;
        private ItemStackHelper itemStack = new ItemStackHelper(ItemStack.EMPTY);
        private String ovText = "";
        private boolean overlay = false;
        private double scale = 1;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int zIndex = 0;

        /**
        * makes a builder for item icons on one overlay.
        * <p>
        * A script does not call this directly: the overlay's own {@code itemBuilder}
        * method is what fills in the overlay.
        *
        * @param draw2D the overlay the icons will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * puts the icon at an x position, and leaves the y position alone.
        * <p>
        * Zero is where an element starts, and the icon is a fixed 16 by 16 box, so
        * nothing is resized by moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond").x(40).y(20).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param x the x position of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * the x position this builder will build the icon at.
        * <p>
        * Zero until an {@code x} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().x(40);
        * Chat.log(`will be built at x ${builder.getX()}`);
        * </pre>
        *
        * @return the x position of the item.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * puts the icon at a y position, and leaves the x position alone.
        * <p>
        * The counterpart of {@code x}, and the icon is a fixed box either way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond").y(20).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param y the y position of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * the y position this builder will build the icon at.
        * <p>
        * Zero until a {@code y} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().y(20);
        * Chat.log(`will be built at y ${builder.getY()}`);
        * </pre>
        *
        * @return the y position of the item.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * puts the icon at a position, and leaves the stack alone.
        * <p>
        * Both coordinates are set and nothing is resized, since an item icon is a fixed
        * 16 by 16 box whatever else is true of it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param x the x position of the item
        * @param y the y position of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
        * sets the stack to draw, from a helper.
        * <p>
        * The stack is copied here, so the icon and the helper do not share one and a
        * change to either is not a change to both. A null helper is ignored rather than
        * applied, which is the opposite of the same method on the icon itself, where
        * null clears it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const player = Player.getPlayer();
        * if (player !== null) {
        * draw.itemBuilder().item(player.getMainHand()).pos(10, 10).buildAndAdd();
        * draw.register();
        * }
        * </pre>
        *
        * @param item the item to draw
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder item(ItemStackHelper item) {
            if (item != null) {
                this.itemStack = item.copy();
            }
            return this;
        }

        /**
        * sets the item to draw from an id, as a single one.
        * <p>
        * The annotation on this one lets the namespace be left off, so a bare name works
        * and is filled in with {@code minecraft}. An id that resolves to nothing in the
        * game's item registry does not fail: that registry has a default entry and hands
        * that back, so a mistyped id gives the default item rather than nothing. The
        * stack is a fresh default instance rather than one with a count, so it is a
        * single item.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("diamond").pos(10, 10).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param id the id of the item to draw, with the namespace optional
        * @return self for chaining.
        * @since 1.8.4
        */
        @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
        public Builder item(String id) {
            this.itemStack = new ItemStackHelper(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id))
                    .getDefaultInstance());
            return this;
        }

        /**
        * sets the item to draw from an id and a count.
        * <p>
        * The annotation on this one asks for a full id, where the one-argument form
        * lets the namespace be left off. Both run the id through the same parser at
        * runtime, so a bare name is filled in either way, and both fall back to the
        * registry's default entry rather than failing on an id that resolves to nothing.
        * <p>
        * The count is not what the overlay puts over the icon on its own, because this
        * builder starts the overlay text as an empty string rather than null, and an
        * empty string is not the same as the null that asks for the stack count. An
        * {@code overlayText} call is what puts a number over the icon, and it turns the
        * overlay on at the same time.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param id    the id of the item to draw
        * @param count the stack size
        * @return self for chaining.
        * @since 1.8.4
        */
        @DocletReplaceParams("id: ItemId, count: int")
        public Builder item(String id, int count) {
            this.itemStack = new ItemStackHelper(id, count);
            return this;
        }

        /**
        * the stack this builder will build the icon with, as a copy.
        * <p>
        * A copy rather than the stack itself, so changing this does not change what will
        * be built. The icon that comes out of it has a getter of its own that does not
        * copy, so the two behave differently.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().item("minecraft:diamond", 5);
        * Chat.log(`will build a stack of ${builder.getItem().getCount()}`);
        * </pre>
        *
        * @return the item to be drawn.
        * @since 1.8.4
        */
        public ItemStackHelper getItem() {
            return itemStack.copy();
        }

        /**
        * This also sets the overlay to be shown.
        *
        * @param overlayText the overlay text
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder overlayText(String overlayText) {
            this.ovText = overlayText;
            this.overlay = true;
            return this;
        }

        /**
        * the overlay text this builder will build the icon with.
        * <p>
        * An empty string to begin with, which is not the same as null: the icons made
        * by the constructors start with null, which is what asks for the stack count.
        * An empty string asks for an empty label instead.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder();
        * Chat.log(`overlay text is "${builder.getOverlayText()}"`);
        * </pre>
        *
        * @return the overlay text.
        * @since 1.8.4
        */
        public String getOverlayText() {
            return ovText;
        }

        /**
        * chooses whether the game's own decorations are drawn over the icon this
        * builder builds.
        * <p>
        * The same flag as the element's {@code setOverlay}, and the same whole set of
        * things: the durability or energy bar, the cooldown overlay, and either the
        * stack count or the text of the script's choosing. Off until this or an
        * {@code overlayText} call says otherwise.
        * <p>
        * The text is a separate thing from this flag, and this does not set it. A builder
        * starts the text as an empty string rather than null, which is not the same as
        * the null the constructors would have given, so turning the overlay on like this
        * on its own draws an empty label rather than the stack count, and an
        * {@code overlayText} call is what puts a number there.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * // overlayVisible on its own would draw an empty label, so the text is set too
        * draw.itemBuilder().item("minecraft:diamond", 5).pos(10, 10).overlayText("5").overlayVisible(true).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param visible whether to draw the decorations
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder overlayVisible(boolean visible) {
            this.overlay = visible;
            return this;
        }

        /**
        * whether the icon this builder builds will have the game's decorations over it.
        * <p>
        * False to begin with, so a builder shows nothing over the icon until it is told
        * to, which is the opposite of the {@code add} methods, which take the overlay as
        * an argument.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder();
        * Chat.log(`builds with an overlay: ${builder.isOverlayVisible()}`);
        * </pre>
        *
        * @return {@code true} if the overlay should be visible, {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isOverlayVisible() {
            return overlay;
        }

        /**
        * scales the icon, and one is what a builder starts at.
        * <p>
        * The icon is a 16 by 16 box, so the drawn size is this multiplied by 16. A
        * scale of zero or less is refused here, which is stricter than the icon's own
        * setter, which refuses only zero and lets a negative scale through, so the same
        * number can be legal on one and not the other.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.itemBuilder().item("minecraft:diamond").pos(10, 10).scale(2).buildAndAdd();
        * draw.register();
        * Chat.log(`drawn ${icon.getScaledWidth()} wide`);
        * </pre>
        *
        * @param scale the scale of the item
        * @return self for chaining.
        * @throws IllegalArgumentException if the scale is zero or negative
        * @since 1.8.4
        */
        public Builder scale(double scale) {
            if (scale <= 0) {
                throw new IllegalArgumentException("Scale must be greater than 0");
            }
            this.scale = scale;
            return this;
        }

        /**
        * the scale this builder will build the icon at.
        * <p>
        * One until a {@code scale} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().scale(1.5);
        * Chat.log(`will be built at ${builder.getScale()} times the size`);
        * </pre>
        *
        * @return the scale of the item.
        * @since 1.8.4
        */
        public double getScale() {
            return scale;
        }

        /**
        * turns the icon by an angle in degrees.
        * <p>
        * Stored as given rather than folded into a single turn, so this reads back as
        * 450. The icon that comes out of it is made through a constructor, which does
        * not fold it either, so an icon built at 450 reads 450 as well.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond").pos(20, 20).rotation(45).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param rotation the rotation (clockwise) of the item in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * the rotation this builder will build the icon at, in degrees.
        * <p>
        * Zero until a {@code rotation} call says otherwise, and not folded into a
        * single turn.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().rotation(45);
        * Chat.log(`will be built turned ${builder.getRotation()} degrees`);
        * </pre>
        *
        * @return the rotation (clockwise) of the item in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * chooses whether the rotation is about the middle of the icon or about its
        * position.
        * <p>
        * True is what a builder starts at, which is the opposite of an icon made
        * directly, so the two turn differently until one of them is told otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.itemBuilder().item("minecraft:diamond").pos(20, 20).rotateCenter(false).buildAndAdd();
        * draw.register();
        * Chat.log(`turns about its position: ${!icon.isRotatingCenter()}`);
        * </pre>
        *
        * @param rotateCenter whether the item should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * whether the icon this builder builds will turn about its middle.
        * <p>
        * True until a {@code rotateCenter} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder();
        * Chat.log(`builds turning about the middle: ${builder.isRotatingCenter()}`);
        * </pre>
        *
        * @return {@code true} if this item should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * sets the number the icon will be ordered against the other elements on its
        * overlay, lower drawn first.
        * <p>
        * Zero is what a builder starts at, and a tie keeps the order the elements were
        * added in.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.itemBuilder().item("minecraft:diamond").pos(0, 0).zIndex(5).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param zIndex the z-index of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * the z-index this builder will build the icon at.
        * <p>
        * Zero until a {@code zIndex} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().zIndex(5);
        * Chat.log(`will be built at z-index ${builder.getZIndex()}`);
        * </pre>
        *
        * @return the z-index of the item.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the icon out of everything set on this builder, and binds it to the
        * overlay this builder was made from.
        * <p>
        * A new icon each time, so the same builder can build more than one. This is what
        * {@code build} and {@code buildAndAdd} call.
        * <p>
        * The overlay text is passed on as it stands, so a builder that has not set one
        * passes an empty string rather than the null the constructors would have given.
        *
        * @return the new icon.
        */
        @Override
        protected Item createElement() {
            return new Item(x, y, zIndex, itemStack, overlay, scale, rotation, ovText).setRotateCenter(rotateCenter)
                    .setParent(this.parent);
        }

        /**
        * the size this builder will build the icon at, which is 16 put through the scale.
        * <p>
        * Cut to a whole number of pixels, so a scale that does not land on a whole
        * number rounds down. This is the number the align methods measure.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().scale(2);
        * Chat.log(`will be built ${builder.getScaledWidth()} wide`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here, because a builder always has an overlay.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder();
        * Chat.log(`measuring against ${builder.getParentWidth()}`);
        * </pre>
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return this.parent.getWidth();
        }

        /**
        * the size this builder will build the icon at, which is 16 put through the scale.
        * <p>
        * The counterpart of {@code getScaledWidth}, and the same number, because an item
        * icon is always square.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().scale(2);
        * Chat.log(`will be built ${builder.getScaledHeight()} tall`);
        * </pre>
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder();
        * Chat.log(`measuring against ${builder.getParentHeight()}`);
        * </pre>
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return this.parent.getHeight();
        }

        /**
        * the x position this builder will build the icon at, which is the left of its
        * 16 by 16 box.
        * <p>
        * The same as {@code getX}: a scale makes the box bigger rather than moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * the y position this builder will build the icon at, which is the top of its
        * 16 by 16 box.
        * <p>
        * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * puts the icon at a position, which is the same as {@code pos}.
        * <p>
        * Nothing is resized, since an item icon is a fixed 16 by 16 box. This is what
        * the align methods call.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.itemBuilder().item("minecraft:diamond");
        * builder.moveTo(40, 20);
        * builder.buildAndAdd();
        * draw.register();
        * Chat.log(`will be built at ${builder.getX()}, ${builder.getY()}`);
        * </pre>
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y);
        }

    }

}
