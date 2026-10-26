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
 * Drawing calls change the in-memory image; call {@link #update()} to upload those pixels to its
 * game texture. Updates upload the whole image, so they can be batched after several drawing calls.
 * <p>
 * Images register under {@code minecraft:jsmimage/<name>}; {@link #getIdentifier()} returns that
 * identifier. Creating another image with the same name registers it under the same identifier.
 * <p>
 * A script gets one through {@link #createWidget(int, int, String)} for a blank one or
 * {@link #createWidget(String, String)} for one loaded from a file, and the class is available
 * through the {@code ScriptClasses.CustomImage} type namespace.
 * <pre>
 * // Draw several changes before uploading the image once.
 * const img = ScriptClasses.CustomImage.createWidget(16, 16, "badge");
 * img.setGraphicsColor(Color.rgb(0xFF, 0x55, 0x55));
 * img.fillRect(0, 0, 16, 16);
 * img.setGraphicsColor(Color.rgb(0xFF, 0xFF, 0xFF));
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
     * Images created by this class, keyed by their full resource identifier (the value returned by
     * {@link #getIdentifier()}). A missing key returns {@code null}; creating an image with an
     * existing identifier replaces that map entry.
     * <pre>
     * // Look up an image by its full identifier.
     * const made = ScriptClasses.CustomImage.createWidget(16, 16, "overlay");
     * const found = ScriptClasses.CustomImage.IMAGES.get(`minecraft:jsmimage/${made.getName()}`);
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
     * Makes an image from an existing one, using the next automatically assigned numeric name.
     * The counter is shared by all instances.
     *
     * @param image the image to draw on and to hand to the game
     * @throws NullPointerException if {@code image} is {@code null}
     * @since 1.8.4
     */
    public CustomImage(BufferedImage image) {
        this(image, String.valueOf(currentId));
    }

    /**
     * Makes an image from an existing one under the given name.
     * <p>
     * The name is appended to {@code jsmimage/} and parsed as a resource identifier, with the
     * default {@code minecraft} namespace. Invalid identifier paths are rejected. The constructor
     * registers and uploads the image before returning; an existing identifier is reused.
     *
     * @param image the image to draw on and to hand to the game
     * @param name  the identifier-path name to register it under, without the {@code jsmimage/} prefix
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
     * Returns the name supplied at construction, without the {@code jsmimage/} prefix. For
     * automatically named images, this is the assigned numeric name.
     *
     * @return the name of this image.
     * @since 1.8.4
     */
    public String getName() {
        return name;
    }

    /**
     * Loads an image from a path relative to the JsMacros config folder. Returns {@code null} if
     * reading fails or no image reader recognizes the file; I/O exceptions are logged. The result
     * is an ordinary {@code BufferedImage}, not a registered game texture.
     * <pre>
     * // Load an image file and draw it when recognized.
     * const img = ScriptClasses.CustomImage.createWidget(32, 32, "canvas");
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
     * Loads a file relative to the JsMacros config folder and returns the requested crop as a
     * {@link BufferedImage}. A file that cannot be read or is not an image returns {@code null},
     * as in {@link #loadImage(String)}. Coordinates are measured from the image's top-left; an
     * invalid crop, including one outside the image, throws {@link java.awt.image.RasterFormatException}.
     * The returned subimage shares pixel data with the loaded image.
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
     * Uploads this image's pixels to its texture so changes become visible. The full image is
     * copied and uploaded; batch drawing changes and call this once afterward. The upload is
     * scheduled on the client thread and this method waits for it unless the waiting thread is
     * interrupted. A new image is uploaded during construction.
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
     * Copies every pixel of the internal {@link BufferedImage} into the texture's native pixels
     * and uploads them.
     * @since 1.8.4
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
     * Saves the current pixels as a PNG under the JsMacros config folder. The path is relative
     * to that folder, {@code .png} is appended to {@code fileName}, and an existing file is
     * overwritten. I/O failures are logged rather than thrown; the returned value does not
     * indicate whether writing succeeded. This saves the in-memory image, whether or not
     * {@link #update()} has uploaded recent changes.
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
                if ((file.getParentFile() != null && !file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) || !file.createNewFile()) {
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
     * Returns this image's registered resource identifier, formed by prefixing its name with
     * {@code jsmimage/} in the default {@code minecraft} namespace. Images with the same name
     * have the same identifier.
     *
     * @return the identifier of this image.
     * @since 1.8.4
     */
    public String getIdentifier() {
        return identifier.toString();
    }

    /**
     * Returns the image width in pixels. It is fixed when the image is created.
     *
     * @return the width of this image.
     * @since 1.8.4
     */
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * Returns the image height in pixels. It is fixed when the image is created.
     *
     * @return the height of this image.
     * @since 1.8.4
     */
    public int getHeight() {
        return image.getHeight();
    }

    /**
     * Returns the mutable internal image, not a copy. Changes made through it affect the pixels
     * used by this {@link CustomImage} and are included by the next {@link #update()}.
     *
     * @return the internal BufferedImage of this image, which all updates are made to.
     * @since 1.8.4
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns the pixel at {@code (x, y)} as an ARGB integer. Coordinates outside the image are
     * rejected by the underlying image implementation.
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
     * Sets one pixel in the in-memory image from an ARGB integer; call {@link #update()} to upload
     * the change to its texture. This does not change the graphics drawing color. Coordinates
     * outside the image are rejected by the underlying image implementation.
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
     * Draws the entire source image into this image at the specified destination size, scaling it
     * as needed. Changes remain in memory until {@link #update()} uploads them.
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
     * Draws the source rectangle {@code (sourceX, sourceY, sourceWidth, sourceHeight)} into the
     * destination rectangle {@code (x, y, width, height)} on this image. The destination is scaled
     * as needed; changes remain in memory until {@link #update()} uploads them.
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
        graphics.drawImage(img, x, y, x + width, y + height, sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight, null);
        return this;
    }

    /**
     * Returns the current graphics drawing color as an ARGB integer. The color is opaque and is
     * separate from pixel values.
     *
     * @return the graphics current rgb color.
     * @since 1.8.4
     */
    public int getGraphicsColor() {
        return graphics.getColor().getRGB();
    }

    /**
     * Sets the opaque graphics drawing color used by drawing operations. The alpha bits of
     * {@code color} are ignored; this does not set the background used by {@link #clearRect(int, int, int, int)}.
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
     * Translates the graphics origin for subsequent drawing operations. The translation is
     * cumulative; it does not move existing pixels.
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
     * Intersects the current clip with the specified rectangle, restricting subsequent drawing to
     * the intersection. The clip follows the graphics transform, including translations.
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
     * Replaces the current clip with the specified rectangle, rather than intersecting it with
     * the current clip as {@link #clipRect(int, int, int, int)} does.
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
     * Restores ordinary paint mode after {@link #setXorMode(int)}.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CustomImage setPaintMode() {
        graphics.setPaintMode();
        return this;
    }

    /**
     * Sets XOR paint mode with {@code color} as the XOR color. The mode remains active until
     * {@link #setPaintMode()} is called.
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
     * Returns the bounds of the current clip, or {@code null} if no clip is set.
     *
     * @return the current clip bounds, or {@code null} if no clip is set.
     * @since 1.8.4
     */
    public Rectangle getClipBounds() {
        return graphics.getClipBounds();
    }

    /**
     * Copies a region of this image to another location in the same image. {@code dx} and
     * {@code dy} are offsets from the source region's origin; overlapping source and destination
     * regions are supported.
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
     * Draws a one-pixel line in the current graphics color.
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
     * Draws a rectangle outline in the current graphics color.
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
     * Fills a rectangle in the current graphics color.
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
     * Clears the rectangle using the graphics background color, which is separate from the
     * drawing color. Use the overload with a {@code color} argument to choose the background
     * color for this operation.
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
     * Clears the rectangle using {@code color}, then restores the previous graphics background.
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
     * Draws a rounded-rectangle outline; {@code arcWidth} and {@code arcHeight} specify the corner
     * arc diameters.
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
     * Fills a rounded rectangle using the drawing color; {@code arcWidth} and {@code arcHeight}
     * specify the corner arc diameters.
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
     * Draws a 3D-effect rectangle outline. The drawing color supplies the line and the graphics
     * background supplies the shading; {@code raised} selects raised or etched shading.
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
     * Fills a 3D-effect rectangle using the drawing color; the graphics background supplies the
     * shading, and {@code raised} selects raised or etched shading.
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
     * Draws the outline of an oval bounded by the specified rectangle, using the drawing color.
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
     * Fills an oval bounded by the specified rectangle using the drawing color.
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
     * Draws an unfilled arc of the oval bounded by the specified rectangle. Angles are in degrees;
     * {@code 0} is at three o'clock and positive angles proceed anticlockwise.
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
     * Fills the arc segment of the bounded oval using the drawing color. Angles follow
     * {@link #drawArc(int, int, int, int, int, int)}: {@code 0} is at three o'clock and positive
     * angles proceed anticlockwise.
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
     * Draws an open polyline through corresponding entries of the coordinate arrays. If their
     * lengths differ, entries beyond the shorter array are ignored; the last point is not joined
     * to the first unless the supplied points do so.
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
     * Draws a closed polygon outline through corresponding entries of the coordinate arrays.
     * If their lengths differ, entries beyond the shorter array are ignored.
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
     * Fills the polygon defined by corresponding entries of the coordinate arrays. If their
     * lengths differ, entries beyond the shorter array are ignored.
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
     * Draws text in the image graphics' current font and drawing color. The y coordinate is the
     * text baseline, not its top.
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
     * Returns the pixel width of a string in this image graphics' current font. The string need
     * not have been drawn first.
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
     * Creates a transparent image with the specified dimensions. Its name is used for game
     * registration, and this API does not resize it.
     *
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @param name   the name to register it under, without the {@code jsmimage/} prefix
     * @return a new blank image
     * @throws java.lang.IllegalArgumentException if either dimension is non-positive or the image
     *         cannot be allocated
     * @since 1.8.4
     */
    public static CustomImage createWidget(int width, int height, String name) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        return new CustomImage(img, name);
    }

    /**
     * Loads an image from a path relative to the JsMacros config folder. The loaded dimensions
     * come from the file; {@code name} is its game registration name, not the file name. Logs an
     * error and returns {@code null} if reading fails or no image reader recognizes the file.
     *
     * @param path the path to the image, relative to the jsMacros config folder
     * @param name the name to register it under, without the {@code jsmimage/} prefix
     * @return a new image, or {@code null} if the file is unreadable or has an unsupported format
     * @since 1.8.4
     */
    @Nullable
    public static CustomImage createWidget(String path, String name) {
        try {
            File file = JsMacrosClient.clientCore.config.configFolder.toPath().resolve(path).toFile();
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                JsMacrosClient.clientCore.profile.logError(new RuntimeException("Could not read image: " + file.getAbsolutePath()));
            }

            return image == null ? null : new CustomImage(image, name);
        } catch (IOException e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        return null;
    }

    /**
     * Returns {@code argb} with its red and blue bytes exchanged; the alpha and green bytes are
     * unchanged. This arithmetic helper does not modify an image.
     *
     * @param argb the ARGB value to transform
     * @return the value with its red and blue bytes exchanged
     * @since 1.8.4
     */
    public static int nativeARGBFlip(int argb) {
        return ((argb & 0x000000FF) << 16) | ((argb & (0x00FF0000)) >> 16) | (argb & 0xFF00FF00);
    }

}
