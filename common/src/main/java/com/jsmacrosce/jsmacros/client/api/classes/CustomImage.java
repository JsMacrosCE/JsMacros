package com.jsmacrosce.jsmacros.client.api.classes;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An image a script can draw on and hand to the game as a texture.
 * <p>
 * Every drawing call here draws into an ordinary {@code BufferedImage} rather than onto the screen,
 * and the game's texture only picks that image up when {@link #update()} is called. So drawing
 * something is not the same as showing it: the drawing calls say what the image should look like
 * and {@code update()} is what puts it on screen. Calling it after each change is wasted work, since
 * it re-uploads the whole image, so it belongs at the end of a batch of changes or after each
 * frame of an animation.
 * <p>
 * A new image registers itself with the game's texture manager under an identifier of the form
 * {@code minecraft:jsmimage/whatever}, which is the name it was given with {@code jsmimage/} in
 * front of it and the default {@code minecraft} namespace in front of that. That identifier, and
 * not the bare prefixed name, is what the two dimensional drawing calls want when a texture has to
 * be named rather than handed over, and what {@link #getIdentifier()} hands back. Two images with
 * the same name share that identifier, so the second one takes the first one's place rather than
 * living beside it.
 * <p>
 * A script gets one through {@link #createWidget(int, int, String)} for a blank one or
 * {@link #createWidget(String, String)} for one loaded from a file, and the raw class is reached
 * with {@code Java.type}.
 * example:
 * <pre>
 * // a blank image, drawn on and then uploaded. The upload is the last
 * // step rather than part of the drawing, so a batch of changes costs one
 * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
 * const img = CustomImage.createWidget(16, 16, "badge");
 * img.setGraphicsColor(0xFF5555);
 * img.fillRect(0, 0, 16, 16);
 * img.setGraphicsColor(0xFFFFFF);
 * img.drawString(2, 11, "hi");
 * img.update();
 * Chat.log(`${img.getIdentifier()} is ${img.getWidth()} by ${img.getHeight()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class CustomImage {

    /**
     * every image made so far, keyed by its identifier.
     * <p>
     * The key is exactly what {@link #getIdentifier()} returns, so a script can find an image it
     * made earlier rather than keeping the reference. That is not the bare {@code jsmimage/}
     * prefixed name: the name goes into the default {@code minecraft} namespace, so an image made
     * under the name {@code overlay} is filed under {@code minecraft:jsmimage/overlay} and a
     * lookup by {@code jsmimage/overlay} finds nothing at all. Building the key from the image
     * rather than spelling it out is the way to avoid that, which is what the example does.
     * <p>
     * An entry is added by every construction and nothing ever removes one, so a script
     * that makes images in a loop grows this for as long as the game runs. Making a second image
     * under a name already here replaces that entry rather than adding a second one, and the older
     * image's texture is replaced along with it.
     * <p>
     * A lookup that finds nothing gives {@code null} rather than {@code undefined}, since it is
     * this Java map answering, so the result is checked against {@code null}.
     * example:
     * <pre>
     * // a named image can be found again later rather than kept in a
     * // variable, which is what a plugin with several scripts wants.
     * // The key is the whole identifier, namespace and all, so it is
     * // built from the name rather than guessed at, and a lookup that
     * // misses gives null rather than undefined
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const made = CustomImage.createWidget(16, 16, "overlay");
     * const found = CustomImage.IMAGES.get(`minecraft:jsmimage/${made.getName()}`);
     * if (found !== null) {
     *   Chat.log(`found the overlay at ${found.getWidth()} by ${found.getHeight()}`);
     * }
     * </pre>
     *
     * @since 1.8.4
     */
    public static final Map<String, CustomImage> IMAGES = new HashMap<>();

    private static final String PREFIX = "jsmimage/";
    private static int currentId = 0;

    private final BufferedImage image;
    private final Graphics2D graphics;
    private final String name;
    private final DynamicTexture texture;
    private final ResourceLocation identifier;

    /**
     * makes an image from an existing one, under a name nobody chose.
     * <p>
     * The name is the count of how many images have been made before it, starting at zero, so two
     * images made this way never share an identifier. That count is global rather than per image,
     * so an image made by any other script has moved it along. This is the same as the two argument
     * form with that number as the name.
     * example:
     * <pre>
     * // an image from an existing one, under a name nobody chose
     * const BufferedImage = Java.type("java.awt.image.BufferedImage");
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const raw = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
     * const img = new CustomImage(raw);
     * Chat.log(`this one is ${img.getName()}`);
     * </pre>
     *
     * @param image the image to draw on and to hand to the game
     * @throws NullPointerException if {@code image} is {@code null}
     * @since 1.8.4
     */
    public CustomImage(BufferedImage image) {
        this(image, String.valueOf(currentId));
    }

    /**
     * makes an image from an existing one under a name of the script's choosing.
     * <p>
     * The name becomes the identifier with a {@code jsmimage/} prefix in front of it and the
     * default {@code minecraft} namespace in front of that, so the name {@code overlay} gives
     * {@code minecraft:jsmimage/overlay} and a name with a namespace of its own comes out
     * doubled. The image is uploaded once here rather than left for a later {@link #update()}, so
     * it is on the game by the time this returns. A name already in use takes the place of
     * whatever was registered under it.
     * example:
     * <pre>
     * // a named image, made from an image the script already has. It is
     * // uploaded once here, so there is no update to call for it to show
     * const BufferedImage = Java.type("java.awt.image.BufferedImage");
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const raw = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
     * const img = new CustomImage(raw, "panel");
     * Chat.log(`${img.getName()} is ${img.getIdentifier()}`);
     * </pre>
     *
     * @param image the image to draw on and to hand to the game
     * @param name  the name to register it under, without the {@code jsmimage/} prefix
     * @throws NullPointerException if {@code image} is {@code null}
     * @since 1.8.4
     */
    public CustomImage(BufferedImage image, String name) {
        this.image = image;
        this.graphics = image.createGraphics();
        this.name = name;
        this.texture = createTexture(image, PREFIX + name);
        identifier = ResourceLocation.parse(PREFIX + name);
        Minecraft.getInstance().getTextureManager().register(identifier, texture);
        update();
        currentId++;
        IMAGES.put(identifier.toString(), this);
    }

    /**
     * The name this image was made with, without the prefix.
     * <p>
     * This is the name that was given, not the identifier the game knows it by, so it has neither
     * the {@code jsmimage/} on the front nor the {@code minecraft:} namespace in front of that.
     * An image made without a name reports the count it was given rather than anything
     * meaningful.
     * example:
     * <pre>
     * // the name is what was given, the identifier is that with a
     * // jsmimage/ prefix and the default minecraft namespace in front
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "marker");
     * Chat.log(`${img.getName()} is ${img.getIdentifier()}`);
     * </pre>
     *
     * @return the name of this image.
     * @since 1.8.4
     */
    public String getName() {
        return name;
    }

    /**
     * The image can be used with the drawImage methods to draw it onto this image.
     * <p>
     * The path is relative to the jsMacros config folder, so a script names a file inside the
     * config folder rather than an absolute one. Two things give {@code null} rather than an
     * image: a file that cannot be read, whose failure is written to the script log, and a file
     * that reads without complaint but is not an image at all, which is silent. So a {@code null}
     * here does not mean the file was missing.
     * <p>
     * The result is a whole separate image, so it can be drawn from, scaled from and kept on its
     * own. It is a {@code BufferedImage} rather than one of these, so it is not registered with the
     * game and has no name of its own.
     * example:
     * <pre>
     * // a file from the config folder, drawn onto this image. A null
     * // result is a file that could not be read or is not an image, and
     * // is worth checking for rather than drawing from
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(32, 32, "canvas");
     * const source = img.loadImage("images/logo.png");
     * if (source !== null) {
     *   img.drawImage(source, 0, 0, 32, 32);
     *   img.update();
     * }
     * </pre>
     *
     * @param path the path to the image, relative to the jsMacros config folder
     * @return an image from the given path, or {@code null} if the file could not be read or is
     *         not an image
     * @since 1.8.4
     */
    @Nullable
    public BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(JsMacrosClient.clientCore.config.configFolder.toPath().resolve(path).toFile());
        } catch (IOException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return null;
    }

    /**
     * Loads the image from the given path and returns a subimage of it from the given positions.
     * The image can be used with the drawImage methods to draw it onto this image.
     * <p>
     * The path and the {@code null} are the same as for the one argument form: a file that cannot
     * be read or is not an image gives {@code null} rather than failing here. The crop is taken
     * from the top left corner of the file, and asking for a region that runs off the edge of it
     * is a failure from the image library rather than something this checks.
     * <p>
     * A subimage shares its data with the image it came from rather than copying it, so drawing on
     * the result also draws on the whole. That is what makes a sprite sheet cheap to work with and
     * is worth knowing before using one as a scratch surface.
     * example:
     * <pre>
     * // one sprite out of a sheet, drawn at a larger size. The subimage
     * // shares data with the whole file, so drawing on it draws on the
     * // file too, which is fine for reading and not for scribbling on
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "sprite");
     * const icon = img.loadImage("images/sheet.png", 16, 0, 16, 16);
     * if (icon !== null) {
     *   img.drawImage(icon, 0, 0, 16, 16);
     *   img.update();
     * }
     * </pre>
     *
     * @param path   the path to the image, relative to the jsMacros config folder
     * @param x      the x position to get the subimage from
     * @param y      the y position to get the subimage from
     * @param width  the width of the subimage
     * @param height the height of the subimage
     * @return the cropped image from the given path, or {@code null} if the file could not be read
     *         or is not an image
     * @throws java.awt.image.RasterFormatException if the region runs outside the image it is
     *         being taken from
     * @since 1.8.4
     */
    public BufferedImage loadImage(String path, int x, int y, int width, int height) {
        BufferedImage image = loadImage(path);
        if (image != null) {
            image = image.getSubimage(x, y, width, height);
        }
        return image;
    }

    /**
     * Updates the texture to be drawn with the contents of this image. Any changes made to this
     * image will only be displayed after calling this method. The method must not be called after
     * each change, but rather when the image is finished being changed.
     * <p>
     * The whole image is copied over rather than the part that changed, so a call costs the same
     * however little was drawn since the last one. The upload is arranged on the client's own
     * thread and this waits for it to finish, so the game is up to date by the time it returns.
     * <p>
     * Reaching the image by other routes does not need this. The pixel and drawing calls below all
     * change the image, and a new image is uploaded once as it is made, so this is only for the
     * changes made after that.
     * example:
     * <pre>
     * // a whole animation drawn in one pass, with a single upload at the
     * // end of it rather than one per change
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "frame");
     * for (let i = 3; i > 0; i--) {
     *   img.setGraphicsColor(0x000000);
     *   img.clearRect(0, 0, 16, 16);
     *   img.setGraphicsColor(0xFF0000);
     *   img.fillRect((3 - i) * 4, 0, 4, 16);
     * }
     * img.update();
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage update() {
        try {
            final Semaphore semaphore = new Semaphore(0);
            Minecraft.getInstance().execute(() -> {
                texture.upload();
                updateTexture();
                semaphore.release();
            });
            semaphore.acquire();
        } catch (InterruptedException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return this;
    }

    /**
     * @since 1.8.4 Copies every pixel of the internal BufferedImage to the
     * NativeImageBackedTexture.
     */
    private void updateTexture() {
        NativeImage ni = texture.getPixels();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                ni.setPixel(x, y, image.getRGB(x, y));
            }
        }
        texture.upload();
    }

    /**
     * Saves this image to the given path. The file will be saved as a png.
     * <p>
     * The path is relative to the jsMacros config folder and the name is the file name without an
     * extension, since {@code .png} is added to it. An existing file at that name is written over.
     * A failure to write is written to the script log rather than raised, so this returns normally
     * either way and there is no way to tell from the return whether the file was written.
     * <p>
     * What is written is the image as it is now, not what the game is showing, so a save after a
     * change but before the {@link #update()} still writes the changed pixels.
     * example:
     * <pre>
     * // saved under the config folder as images/badge.png. A failure to
     * // write is logged rather than raised, so there is nothing to check
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "saved");
     * img.setGraphicsColor(0x00FF00);
     * img.fillRect(0, 0, 8, 8);
     * img.update();
     * img.saveImage("images", "badge");
     * </pre>
     *
     * @param path     the path to the image, relative to the jsMacros config folder
     * @param fileName the file name of the image, without the extension
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage saveImage(String path, String fileName) {
        try {
            File file = JsMacrosClient.clientCore.config.configFolder.toPath().resolve(path).resolve(fileName + ".png").toFile();
            if (!file.exists()) {
                if (!file.mkdirs() && !file.createNewFile()) {
                    JsMacrosClient.clientCore.profile.logError(new RuntimeException("Could not create file: " + file.getAbsolutePath()));
                    return this;
                }
            }
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return this;
    }

    /**
     * The identifier should be used with any buttons and textures in the draw2D and other classes,
     * which require an identifier.
     * <p>
     * This is the name this image was made with, with {@code jsmimage/} in front of it and the
     * default {@code minecraft} namespace in front of that, so the name {@code cursor} reads back
     * as {@code minecraft:jsmimage/cursor}. It is the key this image is also filed under in
     * {@link #IMAGES}, which is why {@code IMAGES.has(identifier)} is true for an image that is
     * live. Two images made under the same name give the same identifier, so this on its own does
     * not tell two of them apart.
     * example:
     * <pre>
     * // the identifier is the name with a jsmimage/ prefix and the
     * // default minecraft namespace, which is also the key this image
     * // is filed under
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "cursor");
     * Chat.log(`${img.getIdentifier()} is in the map: ${CustomImage.IMAGES.has(img.getIdentifier())}`);
     * </pre>
     *
     * @return the identifier of this image.
     * @since 1.8.4
     */
    public String getIdentifier() {
        return identifier.toString();
    }

    /**
     * The width is a constant and will not change.
     *
     * This is the width of the underlying image, which is fixed when the image is made and
     * cannot be changed afterwards, since there is no way to resize the image itself.
     * Drawing outside it is simply lost rather than refused.
     * <p>
     * This is a count of pixels, so an image of sixteen by sixteen is sixteen by sixteen
     * whatever resolution the screen happens to be at.
     * example:
     * <pre>
     * // a fixed size, and anything drawn outside it is simply gone
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "sized");
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 32, 32);
     * img.update();
     * Chat.log(`only ${img.getWidth()} of that was kept`);
     * </pre>
     *
     * @return the width of this image.
     * @since 1.8.4
     */
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * The height is a constant and will not change.
     *
     * This is the height of the underlying image, which is fixed when the image is made and
     * cannot be changed afterwards, since there is no way to resize the image itself. Drawing
     * outside it is simply lost rather than refused.
     * example:
     * <pre>
     * // the height is as fixed as the width
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 4, "sized");
     * Chat.log(`${img.getWidth()} by ${img.getHeight()}`);
     * </pre>
     *
     * @return the height of this image.
     * @since 1.8.4
     */
    public int getHeight() {
        return image.getHeight();
    }

    /**
     * This is the image itself rather than a copy of it, so drawing on what comes back draws on
     * this one and vice versa. It is the same object the pixel and drawing calls work on, so a
     * change made through any of them is picked up by the next {@link #update()}. It is not
     * registered with the game and has no name of its own.
     * example:
     * <pre>
     * // the image itself rather than a copy, so the game's own image class
     * // can be used for anything these drawing calls do not cover
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "raw");
     * const raw = img.getImage();
     * raw.setRGB(0, 0, 0xFF00FF00);
     * img.update();
     * Chat.log(`the pixel is now ${img.getPixel(0, 0).toString(16)}`);
     * </pre>
     *
     * @return the internal BufferedImage of this image, which all updates are made to.
     * @since 1.8.4
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * The color is in the ARGB format.
     *
     * The value has an alpha channel at the top, so a fully opaque pixel comes back as
     * {@code 0xff} followed by the red, green and blue bytes. A position outside the image is
     * a failure from the image library rather than something this checks.
     * example:
     * <pre>
     * // an opaque red pixel, which is ff followed by ff, 00 and 00
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(2, 2, "pixels");
     * img.setPixel(0, 0, 0xFFFF0000);
     * img.update();
     * Chat.log(`read back ${img.getPixel(0, 0).toString(16)}`);
     * </pre>
     *
     * @param x the x position to get the color from
     * @param y the y position to get the color from
     * @return the color at the given position.
     * @since 1.8.4
     */
    public int getPixel(int x, int y) {
        return image.getRGB(x, y);
    }

    /**
     * The color is in the ARGB format.
     *
     * The value is read as it is given, alpha included, so a pixel that was partly
     * transparent becomes what that alpha says. This changes the image but not the texture the
     * game is showing, so a {@link #update()} is what makes it visible.
     * <p>
     * This is the pixel route rather than the graphics one, and the two do not go through the
     * same state: a colour set by {@link #setGraphicsColor(int)} is what the drawing calls use,
     * while this one changes a single pixel outright. A position outside the image is a
     * failure from the image library rather than something this checks.
     * example:
     * <pre>
     * // a half transparent pixel, which is the alpha byte and then the
     * // three colour bytes. Nothing is on screen until update
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(2, 2, "pixels");
     * img.setPixel(1, 1, 0x80FF8800);
     * img.update();
     * </pre>
     *
     * @param x    the x position to set the color at
     * @param y    the y position to set the color at
     * @param argb the ARGB value to set the pixel to
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setPixel(int x, int y, int argb) {
        image.setRGB(x, y, argb);
        return this;
    }

    /**
     * This is the plain form, which draws the whole of {@code img} at the size asked for rather
     * than at its own, so scaling is what the four position and size arguments are for. The
     * destination is this image and the change is not on screen until {@link #update()}.
     * <p>
     * A source that is itself a subimage shares data with the image it came from, so this
     * draws from the whole of that rather than from the crop alone.
     * example:
     * <pre>
     * // a whole image drawn at a chosen size, which is what makes a
     * // source larger or smaller than it was
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(32, 32, "canvas");
     * const source = img.loadImage("images/logo.png");
     * if (source !== null) {
     *   img.drawImage(source, 0, 0, 32, 32);
     *   img.update();
     * }
     * </pre>
     *
     * @param img    the image to draw onto this image
     * @param x      the x position to draw the image at
     * @param y      the y position to draw the image at
     * @param width  the width of the image to draw
     * @param height the height of the image to draw
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawImage(Image img, int x, int y, int width, int height) {
        graphics.drawImage(img, x, y, width, height, null);
        return this;
    }

    /**
     * This is the form that takes part of a source rather than all of it, and the six positions and
     * sizes fall into two groups: the first four are where the result goes on this image and the
     * last four are which part of the source it comes from.
     * <p>
     * The {@code img} argument is not what the part is taken from. The part is taken from this
     * image, and the argument is passed on to the underlying call without being used for the
     * region, so a draw meant to copy from another image copies from this one instead. That is
     * a defect in the method rather than something a script can work around, and
     * {@link #drawImage(Image, int, int, int, int)} is the form that does take a source from
     * elsewhere.
     * example:
     * <pre>
     * // this form takes the region from this image rather than from the
     * // source passed in, so it copies within the image. The source
     * // argument is not what the region is read from
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "cropped");
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 2, 2);
     * img.drawImage(img.getImage(), 4, 4, 2, 2, 0, 0, 2, 2);
     * img.update();
     * </pre>
     *
     * @param img          the image to draw onto this image
     * @param x            the x position to draw the image at
     * @param y            the y position to draw the image at
     * @param width        the width of the image to draw
     * @param height       the height of the image to draw
     * @param sourceX      the x position of the subimage to draw
     * @param sourceY      the y position of the subimage to draw
     * @param sourceWidth  the width of the subimage to draw
     * @param sourceHeight the height of the subimage to draw
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawImage(Image img, int x, int y, int width, int height, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        graphics.drawImage(image, x, y, x + width, y + height, sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight, null);
        return this;
    }

    /**
     * The color is a rgb value which is used for draw and fill operations.
     *
     * The value has {@code 0xff} in the alpha position whatever was set, because the drawing
     * colour this keeps is always opaque, so the three colour bytes are all that matter. It is
     * the colour the drawing calls are using, and has nothing to do with individual pixels.
     * example:
     * <pre>
     * // a red drawing colour, which reads back as ffff0000 whatever the
     * // alpha byte of the value that set it was
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(2, 2, "colour");
     * img.setGraphicsColor(0xFF0000);
     * Chat.log(`drawing colour is ${img.getGraphicsColor().toString(16)}`);
     * </pre>
     *
     * @return the graphics current rgb color.
     * @since 1.8.4
     */
    public int getGraphicsColor() {
        return graphics.getColor().getRGB();
    }

    /**
     * The color is a rgb value which is used for draw and fill operations.
     *
     * This is the colour the drawing calls use for their outlines and their fills, and it is
     * separate from the background colour that {@link #clearRect(int, int, int, int)} uses, so
     * the two do not have to be kept in step. The alpha byte of the value is ignored and the
     * colour comes out opaque, so passing an alpha of zero still gives an opaque drawing
     * colour. It stays in effect until it is set again.
     * example:
     * <pre>
     * // the drawing colour, which is what the fill and outline calls use.
     * // The alpha byte is ignored, so this one is opaque
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "colour");
     * img.setGraphicsColor(0x3366FF);
     * img.drawRect(0, 0, 4, 4);
     * img.update();
     * </pre>
     *
     * @param color the rgb color to use for graphics operations
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setGraphicsColor(int color) {
        graphics.setColor(new Color(color));
        return this;
    }

    /**
     * This moves where the following drawing calls put things rather than moving anything
     * already drawn, so it is a way of drawing a shape at an offset rather than a way of
     * shifting the image. It adds to any earlier move rather than replacing it, and there is
     * no call to undo it short of starting a new image.
     * example:
     * <pre>
     * // everything after the move is drawn from the new origin, and
     * // nothing already drawn moves
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "moved");
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 2, 2);
     * img.translate(4, 4);
     * img.fillRect(0, 0, 2, 2);
     * img.update();
     * </pre>
     *
     * @param x the x position of the origin point
     * @param y the y position of the origin point
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage translate(int x, int y) {
        graphics.translate(x, y);
        return this;
    }

    /**
     * This narrows the current clip rather than replacing it, so calling it twice leaves only
     * what is inside both rectangles, and a rectangle that does not overlap what is already
     * clipped leaves nothing to draw into at all. The clip is in the same space as the drawing
     * calls, so a {@link #translate(int, int)} moves it too.
     * example:
     * <pre>
     * // a clip that narrows rather than replaces, so a second one leaves
     * // only what is inside both
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "clipped");
     * img.clipRect(0, 0, 4, 4);
     * img.clipRect(2, 2, 4, 4);
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 8, 8);
     * img.update();
     * </pre>
     *
     * @param x      the x coordinate of the rectangle to intersect the clip with
     * @param y      the y coordinate of the rectangle to intersect the clip with
     * @param width  the width of the rectangle to intersect the clip with
     * @param height the height of the rectangle to intersect the clip with
     * @return self for chaining.
     */
    public CustomImage clipRect(int x, int y, int width, int height) {
        graphics.clipRect(x, y, width, height);
        return this;
    }

    /**
     * This replaces the clip outright, so a second call undoes the narrowing a
     * {@link #clipRect(int, int, int, int)} did. Once a clip is set there is no call to take
     * it away again, so a script that wants the whole image back should not have set one.
     * example:
     * <pre>
     * // a clip that replaces, so this one is not narrowed by anything set
     * // before it
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "clipped");
     * img.clipRect(0, 0, 2, 2);
     * img.setClip(4, 4, 4, 4);
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 8, 8);
     * img.update();
     * </pre>
     *
     * @param x      the x coordinate of the new clip rectangle
     * @param y      the y coordinate of the new clip rectangle
     * @param width  the width of the new clip rectangle
     * @param height the height of the new clip rectangle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setClip(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        graphics.setClip(rect);
        return this;
    }

    /**
     * This undoes a {@link #setXorMode(int)}, so after it the drawing calls paint over what is
     * there rather than combining with it. It takes no colour because restoring the ordinary
     * painting is the whole of what it does.
     * example:
     * <pre>
     * // back to ordinary painting after an xor section
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "mode");
     * img.setXorMode(0xFFFFFF);
     * img.setPaintMode();
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 4, 4);
     * img.update();
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setPaintMode() {
        graphics.setPaintMode();
        return this;
    }

    /**
     * In this mode the drawing calls combine what is there with the given colour rather than
     * covering it, which is how a hole or a highlight is cut out of something already drawn.
     * It stays in effect until a {@link #setPaintMode()} puts it back, so every drawing call in
     * between is in this mode rather than only the next one.
     * example:
     * <pre>
     * // an xor section, which cuts through rather than covering, and
     * // stays in effect until setPaintMode
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "mode");
     * img.setGraphicsColor(0x000000);
     * img.fillRect(0, 0, 4, 4);
     * img.setXorMode(0xFFFFFF);
     * img.fillRect(1, 1, 2, 2);
     * img.setPaintMode();
     * img.update();
     * </pre>
     *
     * @param color the color to use for the xor operation
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setXorMode(int color) {
        graphics.setXORMode(new Color(color));
        return this;
    }

    /**
     * This is a rectangle rather than an array, holding the left, top, width and height of the
     * clip. It is {@code null} when no clip is set at all, which is the state a new image
     * starts in, so a script that has not set one has nothing here to read.
     * example:
     * <pre>
     * // null until a clip is set, and a rectangle once one is. The width
     * // and height come straight out of the image library, which was not
     * // read here: an OpenJDK Graphics2D reports a negative extent for a
     * // clip narrowed to nothing, but that is its behaviour and this
     * // example was not run to confirm it
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "clipped");
     * Chat.log(`before: ${img.getClipBounds()}`);
     * img.setClip(2, 3, 4, 5);
     * const bounds = img.getClipBounds();
     * if (bounds !== null) {
     *   Chat.log(`after: ${bounds.getWidth()} by ${bounds.getHeight()}`);
     * }
     * </pre>
     *
     * @return an array with the bounds of the current clip.
     * @since 1.8.4
     */
    public Rectangle getClipBounds() {
        return graphics.getClipBounds();
    }

    /**
     * This copies within this one image, so it is how a region is duplicated or moved rather than
     * how something outside is brought in. The two offsets are relative to the area being
     * copied, so {@code 0, 0} draws the area over itself and leaves it unchanged. Overlapping
     * areas are safe, the copy being taken before anything is written.
     * example:
     * <pre>
     * // a copy inside the same image, moved by the two offsets, which
     * // are relative to the area being copied
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "copied");
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 2, 2);
     * img.copyArea(0, 0, 2, 2, 4, 4);
     * img.update();
     * </pre>
     *
     * @param x      the x position to copy from
     * @param y      the y position to copy from
     * @param width  the width of the area to copy
     * @param height the height of the area to copy
     * @param dx     the offset to the x position to copy to
     * @param dy     the offset to the y position to copy to
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage copyArea(int x, int y, int width, int height, int dx, int dy) {
        graphics.copyArea(x, y, width, height, dx, dy);
        return this;
    }

    /**
     * The line is one pixel wide and is drawn in the colour set by
     * {@link #setGraphicsColor(int)}, and it runs between the two points inclusive, so a line
     * whose ends are the same is a single pixel rather than nothing.
     * example:
     * <pre>
     * // a diagonal in the drawing colour, one pixel wide
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "line");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawLine(0, 0, 7, 7);
     * img.update();
     * </pre>
     *
     * @param x1 the first x position of the line
     * @param y1 the first y position of the line
     * @param x2 the second x position of the line
     * @param y2 the second y position of the line
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawLine(int x1, int y1, int x2, int y2) {
        graphics.drawLine(x1, y1, x2, y2);
        return this;
    }

    /**
     * This is the outline only, so the inside of the rectangle is left as it was rather than
     * being filled. The width and height are the outside ones, so the outline is drawn inside
     * them rather than straddling the edge. A width or height of one gives a vertical or
     * horizontal line rather than a box, and the colour is the drawing colour.
     * example:
     * <pre>
     * // an outline, with the inside left as it was
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "outline");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawRect(1, 1, 6, 6);
     * img.update();
     * </pre>
     *
     * @param x      the x position of the rectangle
     * @param y      the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawRect(int x, int y, int width, int height) {
        graphics.drawRect(x, y, width, height);
        return this;
    }

    /**
     * This fills the whole rectangle including its edge, unlike
     * {@link #drawRect(int, int, int, int)} which only draws the outline. The colour is the one
     * set by {@link #setGraphicsColor(int)} and nothing of what was underneath survives.
     * example:
     * <pre>
     * // a filled rectangle, edge included, in the drawing colour
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(8, 8, "filled");
     * img.setGraphicsColor(0x00FF00);
     * img.fillRect(0, 0, 4, 4);
     * img.update();
     * </pre>
     *
     * @param x      the x position of the rectangle
     * @param y      the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fillRect(int x, int y, int width, int height) {
        graphics.fillRect(x, y, width, height);
        return this;
    }

    /**
     * This puts the graphics background colour into the rectangle rather than the drawing colour,
     * so it is the one filling call whose colour is not the one
     * {@link #setGraphicsColor(int)} set. Nothing in this class sets that background, so on a
     * new image it is transparent and this is how a region is made see through. The overload
     * taking a colour is the one that says which colour to use.
     * example:
     * <pre>
     * // cleared to the background, which on a new image is transparent
     * // whatever the drawing colour is
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "cleared");
     * img.setGraphicsColor(0xFF0000);
     * img.fillRect(0, 0, 4, 4);
     * img.clearRect(0, 0, 2, 2);
     * img.update();
     * </pre>
     *
     * @param x      the x position of the rectangle
     * @param y      the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage clearRect(int x, int y, int width, int height) {
        graphics.clearRect(x, y, width, height);
        return this;
    }

    /**
     * This is the form that says which colour to clear to, and it puts the background back
     * afterwards, so the background is left as it was rather than being changed for whatever is
     * drawn later. Nothing else in this class reads the background, so a script that only ever
     * clears with a colour never needs to set one.
     * example:
     * <pre>
     * // cleared to a colour of the call's own choosing, which leaves the
     * // background as it was afterwards
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(4, 4, "cleared");
     * img.clearRect(0, 0, 2, 2, 0x0000FF);
     * img.update();
     * </pre>
     *
     * @param x      the x position of the rectangle
     * @param y      the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param color  the rgb color to fill the rectangle with
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage clearRect(int x, int y, int width, int height, int color) {
        Color cached = graphics.getBackground();
        graphics.setBackground(new Color(color));
        graphics.clearRect(x, y, width, height);
        graphics.setBackground(cached);
        return this;
    }

    /**
     * This is the outline only, as {@link #drawRect(int, int, int, int)} is, and the two arc
     * arguments set how far the corners are rounded: they are the diameter of the arc rather
     * than its radius, so half that value is the radius, and a corner sharper than the rectangle
     * itself is pulled in to fit. An arc of zero on both is a plain rectangle.
     * example:
     * <pre>
     * // an outline with rounded corners, where the two arc values are
     * // diameters rather than radii
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "rounded");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawRoundRect(0, 0, 16, 16, 8, 8);
     * img.update();
     * </pre>
     *
     * @param x         the x position to draw the rectangle at
     * @param y         the y position to draw the rectangle at
     * @param width     the width of the rectangle
     * @param height    the height of the rectangle
     * @param arcWidth  the horizontal diameter of the arc at the four corners
     * @param arcHeight the vertical diameter of the arc at the four corners
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        return this;
    }

    /**
     * This fills the rounded rectangle including its edge, unlike
     * {@link #drawRoundRect(int, int, int, int, int, int)} which only draws the outline. The
     * two arc arguments are the diameter of the corner arc rather than its radius, and the
     * colour is the drawing colour.
     * example:
     * <pre>
     * // a filled rounded rectangle, edge included
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "rounded");
     * img.setGraphicsColor(0xFFAA00);
     * img.fillRoundRect(0, 0, 16, 16, 8, 8);
     * img.update();
     * </pre>
     *
     * @param x         the x position to draw the rectangle at
     * @param y         the y position to draw the rectangle at
     * @param width     the width of the rectangle
     * @param height    the height of the rectangle
     * @param arcWidth  the horizontal diameter of the arc at the four corners
     * @param arcHeight the vertical diameter of the arc at the four corners
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        return this;
    }

    /**
     * This draws the outline of a rectangle that looks like it has depth, by drawing a second
     * offset outline in the background colour. The flag decides which way it looks: raised puts
     * the light edge on the top and left, etched puts it on the bottom and right. The line is
     * the drawing colour and the shadow is the background, so a background that has been set to
     * something else changes the effect.
     * example:
     * <pre>
     * // an outline that looks raised, with the light edge on the top and
     * // left and the shadow in the background colour
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "raised");
     * img.setGraphicsColor(0xFFFFFF);
     * img.draw3DRect(1, 1, 12, 12, true);
     * img.update();
     * </pre>
     *
     * @param x      the x position to draw the 3D rectangle at
     * @param y      the y position to draw the 3D rectangle at
     * @param width  the width of the 3D rectangle
     * @param height the height of the 3D rectangle
     * @param raised whether the rectangle should be raised above the surface or etched into the
     *               surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage draw3DRect(int x, int y, int width, int height, boolean raised) {
        graphics.draw3DRect(x, y, width, height, raised);
        return this;
    }

    /**
     * This fills a rectangle that looks like it has depth, as
     * {@link #draw3DRect(int, int, int, int, boolean)} does for the outline, and the flag
     * decides the same way round. The fill is the drawing colour and the edge that gives the
     * effect is the background colour, so a background that has been set to something else
     * changes it.
     * example:
     * <pre>
     * // a filled rectangle that looks raised rather than etched
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "raised");
     * img.setGraphicsColor(0x3366CC);
     * img.fill3DRect(1, 1, 12, 12, true);
     * img.update();
     * </pre>
     *
     * @param x      the x position to draw the 3D rectangle at
     * @param y      the y position to draw the 3D rectangle at
     * @param width  the width of the 3D rectangle
     * @param height the height of the 3D rectangle
     * @param raised whether the rectangle should be raised above the surface or etched into the
     *               surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fill3DRect(int x, int y, int width, int height, boolean raised) {
        graphics.fill3DRect(x, y, width, height, raised);
        return this;
    }

    /**
     * This is the outline only, as {@link #drawRect(int, int, int, int)} is, and the oval fills
     * the rectangle given, so a square one is a circle and a wide one is a wide oval. The colour
     * is the drawing colour.
     * example:
     * <pre>
     * // an oval outline filling the rectangle it is given, so a square
     * // rectangle gives a circle
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "oval");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawOval(0, 0, 16, 16);
     * img.update();
     * </pre>
     *
     * @param x      the x position to draw the oval at
     * @param y      the y position to draw the oval at
     * @param width  the width of the oval
     * @param height the height of the oval
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawOval(int x, int y, int width, int height) {
        graphics.drawOval(x, y, width, height);
        return this;
    }

    /**
     * This fills the oval including its edge, unlike {@link #drawOval(int, int, int, int)} which
     * only draws the outline, and it fills the rectangle given rather than being round itself.
     * The colour is the drawing colour.
     * example:
     * <pre>
     * // a filled oval filling the rectangle it is given
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "oval");
     * img.setGraphicsColor(0x00CCFF);
     * img.fillOval(0, 0, 16, 16);
     * img.update();
     * </pre>
     *
     * @param x      the x position to draw the oval at
     * @param y      the y position to draw the oval at
     * @param width  the width of the oval
     * @param height the height of the oval
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fillOval(int x, int y, int width, int height) {
        graphics.fillOval(x, y, width, height);
        return this;
    }

    /**
     * This is the outline of part of an oval, taken from the rectangle given as the whole oval is.
     * The angles are in degrees, {@code 0} is at the three o'clock position, and a positive
     * {@code arcAngle} goes anticlockwise, so a quarter of an oval is an arc angle of 90. Only
     * the line is drawn; nothing is filled.
     * example:
     * <pre>
     * // a quarter of an oval as a line, where 0 is at three o'clock and
     * // a positive angle goes anticlockwise
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "arc");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawArc(0, 0, 16, 16, 0, 90);
     * img.update();
     * </pre>
     *
     * @param x          the x position to draw the arc at
     * @param y          the y position to draw the arc at
     * @param width      the width of the arc
     * @param height     the height of the arc
     * @param startAngle the beginning angle
     * @param arcAngle   the angular extent of the arc, relative to the start angle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
        return this;
    }

    /**
     * This is the filled version of {@link #drawArc(int, int, int, int, int, int)}: the same
     * section of the same oval, closed off and filled, with the same angle conventions of
     * {@code 0} at three o'clock and a positive angle going anticlockwise. The colour is the
     * drawing colour.
     * example:
     * <pre>
     * // the filled version of the same quarter oval, closed off at both
     * // ends of the arc
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "arc");
     * img.setGraphicsColor(0xFFCC00);
     * img.fillArc(0, 0, 16, 16, 0, 90);
     * img.update();
     * </pre>
     *
     * @param x          the x position to draw the arc at
     * @param y          the y position to draw the arc at
     * @param width      the width of the arc
     * @param height     the height of the arc
     * @param startAngle the beginning angle
     * @param arcAngle   the angular extent of the arc, relative to the start angle
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
        return this;
    }

    /**
     * The x and y array must have the same length and order for the points.
     *
     * This is a polyline rather than a closed shape, so the last point is joined back to the first
     * only if the arrays make it so. The two arrays are read position by position, so the x of
     * one point is the y of the same point; where the two arrays differ in length only the
     * shorter one decides how many points there are.
     * example:
     * <pre>
     * // an open polyline, which is a polygon without the closing edge.
     * // The two arrays are read position by position
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "polyline");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawPolygonLine([0, 8, 15], [15, 0, 15]);
     * img.update();
     * </pre>
     *
     * @param pointsX an array of all x positions of the points in the polygon
     * @param pointsY an array of all y positions of the points in the polygon
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawPolygonLine(int[] pointsX, int[] pointsY) {
        graphics.drawPolyline(pointsX, pointsY, Math.min(pointsX.length, pointsY.length));
        return this;
    }

    /**
     * The x and y array must have the same length and order for the points.
     *
     * This is a closed shape, so the last point is joined back to the first whether or not the
     * arrays say so, and only the outline is drawn. The two arrays are read position by
     * position, so the x of one point is the y of the same point, and where the two arrays
     * differ in length only the shorter one decides how many points there are. Fewer than three
     * points is not a shape and draws nothing.
     * example:
     * <pre>
     * // a closed polygon outline, which is the polyline plus the closing
     * // edge back to the first point
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "polygon");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawPolygon([8, 15, 0], [0, 15, 15]);
     * img.update();
     * </pre>
     *
     * @param pointsX an array of all x positions of the points in the polygon
     * @param pointsY an array of all y positions of the points in the polygon
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawPolygon(int[] pointsX, int[] pointsY) {
        graphics.drawPolygon(pointsX, pointsY, Math.min(pointsX.length, pointsY.length));
        return this;
    }

    /**
     * The x and y array must have the same length and order for the points.
     *
     * This fills the closed shape the points make, unlike
     * {@link #drawPolygon(int[], int[])} which only draws its outline, and it is the whole
     * interior that is filled rather than just the edge. The two arrays are read position by
     * position and where they differ in length only the shorter one decides how many points
     * there are.
     * example:
     * <pre>
     * // the filled version of the same closed shape
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "polygon");
     * img.setGraphicsColor(0x88CCFF);
     * img.fillPolygon([8, 15, 0], [0, 15, 15]);
     * img.update();
     * </pre>
     *
     * @param pointsX an array of all x positions of the points in the polygon
     * @param pointsY an array of all y positions of the points in the polygon
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage fillPolygon(int[] pointsX, int[] pointsY) {
        graphics.fillPolygon(pointsX, pointsY, Math.min(pointsX.length, pointsY.length));
        return this;
    }

    /**
     * This uses the image library's own font rather than the game's, and the y coordinate is the
     * <i>baseline</i> of the text rather than its top, so a string sits above the point given.
     * The colour is the drawing colour, and the height of a line of text is whatever that font
     * makes it rather than anything this controls.
     * example:
     * <pre>
     * // text drawn in the image library's own font, where the y given is
     * // the baseline rather than the top of the text
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(32, 16, "label");
     * img.setGraphicsColor(0xFFFFFF);
     * img.drawString(1, 12, "hello");
     * img.update();
     * </pre>
     *
     * @param x    the x position to draw the string at
     * @param y    the y position to draw the string at
     * @param text the text to draw
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage drawString(int x, int y, String text) {
        graphics.drawString(text, x, y);
        return this;
    }

    /**
     * This measures a string in the image library's own font, which is the same font
     * {@link #drawString(int, int, String)} draws with, and it does not need the text to have
     * been drawn or even this image to have been made. It is a count of pixels and is not the
     * same as the width the game measures a text component at, which is a different font.
     * example:
     * <pre>
     * // the width of a string in the image font, which is a different
     * // measurement from the width the game reports for a text component
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(64, 16, "measured");
     * img.setGraphicsColor(0xFFFFFF);
     * Chat.log(`hello is ${img.getStringWidth("hello")} pixels wide here`);
     * </pre>
     *
     * @param toAnalyze the string to analyze
     * @return the width of the string for the current font in pixels
     * @since 1.8.4
     */
    public int getStringWidth(String toAnalyze) {
        Font font = graphics.getFont();
        FontMetrics metrics = graphics.getFontMetrics(font);
        return metrics.stringWidth(toAnalyze);
    }

    private static DynamicTexture createTexture(BufferedImage image, String name) {
        AtomicReference<DynamicTexture> texture = new AtomicReference<>();
        try {
            final Semaphore semaphore = new Semaphore(0);
            Minecraft.getInstance().execute(() -> {
                texture.set(new DynamicTexture(name, image.getWidth(), image.getHeight(), true));
                semaphore.release();
            });
            semaphore.acquire();
        } catch (InterruptedException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return texture.get();
    }

    /**
     * makes a blank image of the given size.
     * <p>
     * The image starts fully transparent, so a new one is blank however big it is, and the name is
     * what it is registered with the game under rather than a number. This is the quickest way to
     * get something to draw on, and the size is fixed from here on since nothing here resizes the
     * image.
     * example:
     * <pre>
     * // a blank image of a chosen size, drawn on and uploaded
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget(16, 16, "badge");
     * img.setGraphicsColor(0xFF5555);
     * img.fillRect(0, 0, 16, 16);
     * img.update();
     * </pre>
     *
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @param name   the name to register it under, without the {@code jsmimage/} prefix
     * @return a new blank image
     * @throws java.lang.IllegalArgumentException if either size is zero or negative, or if the two
     *         together are larger than the image library will allocate
     * @since 1.8.4
     */
    public static CustomImage createWidget(int width, int height, String name) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        return new CustomImage(img, name);
    }

    /**
     * makes an image from a file in the config folder.
     * <p>
     * The path is relative to the jsMacros config folder and the image is whatever size the file
     * is, so unlike {@link #createWidget(int, int, String)} the size is not the script's to pick.
     * The name is what it is registered with the game under, which is not the file name.
     * <p>
     * A file that cannot be read gives {@code null} here. A file that reads without complaint but
     * is not an image does not, and goes on to the constructor with nothing to draw, which fails
     * there instead; so a {@code null} here means the file could not be read rather than that it
     * was the wrong kind of file.
     * example:
     * <pre>
     * // an image from a file in the config folder, under a name of the
     * // script's choosing rather than the file's own
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * const img = CustomImage.createWidget("images/logo.png", "logo");
     * if (img !== null) {
     *   Chat.log(`the logo is ${img.getWidth()} by ${img.getHeight()}`);
     *   img.update();
     * }
     * </pre>
     *
     * @param path the path to the image, relative to the jsMacros config folder
     * @param name the name to register it under, without the {@code jsmimage/} prefix
     * @return a new image holding what the file holds, or {@code null} if the file could not be read
     * @since 1.8.4
     */
    @Nullable
    public static CustomImage createWidget(String path, String name) {
        try {
            File file = JsMacrosClient.clientCore.config.configFolder.toPath().resolve(path).toFile();
            return new CustomImage(ImageIO.read(file), name);
        } catch (IOException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return null;
    }

    /**
     * Minecraft textures use an ABGR format for some reason.
     *
     * This swaps the red and blue bytes of a colour and leaves the green and the alpha alone, so
     * the result is the same colour read with red and blue the wrong way round. It is a plain
     * arithmetic helper that touches no image, so a script can use it on a value it computed
     * itself as well as on one read out of a pixel.
     * <p>
     * The note about the game's format being the other way round is why this exists, so the
     * value going in is an ordinary colour and the value coming out is the one the game's own
     * pixel layout wants.
     * example:
     * <pre>
     * // red and blue swapped, green and alpha left alone, which is the
     * // same colour read the other way round
     * const CustomImage = Java.type("com.jsmacrosce.jsmacros.client.api.classes.CustomImage");
     * Chat.log(`ff112233 flips to ${CustomImage.nativeARGBFlip(0xff112233).toString(16)}`);
     * </pre>
     *
     * @param argb the argb color to transform
     * @return the abgr argb for the given argb color.
     * @since 1.8.4
     */
    public static int nativeARGBFlip(int argb) {
        return ((argb & 0x000000FF) << 16) | ((argb & (0x00FF0000)) >> 16) | (argb & 0xFF00FF00);
    }

}
