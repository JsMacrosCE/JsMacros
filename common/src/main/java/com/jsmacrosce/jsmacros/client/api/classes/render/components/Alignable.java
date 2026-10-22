package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import java.util.Locale;

 /**
 * the alignment every element in this package shares, and what it is measured against.
 * <p>
 * There are two things to align to and each has its own set of methods. The plain
 * {@code alignHorizontally(String)} family is measured against the <em>parent</em>, which
 * is the overlay the element was added to, or the window when the element has no parent
 * of its own. The {@code alignHorizontally(Alignable, String)} family is measured against
 * another element that is passed in, which is how two elements are lined up with each
 * other. The vertical half of each works the same way.
 * <p>
 * Both families move the element rather than report anything, and both hand the element
 * back so a chain can carry on.
 * <p>
 * The {@code B} is what comes back, and each element class passes itself, so a
 * {@code Rect} aligns to a {@code Rect} and gives a {@code Rect} back. A builder does
 * the same for itself, so an element can be aligned before it is built.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
 * const label = draw.addText("centred", 0, 0, 0xFFFFFFFF, true);
 * // the text is put in the middle of the rectangle
 * label.align(panel, "center", "center");
 * draw.register();
 * </pre>
 *
 * @param <B> the builder class
 * @since 1.8.4
 */
public interface Alignable<B extends Alignable<B>> {

    /**
    * lines this element up against another one, with no offset.
    * <p>
    * The same as passing 0 as the offset, which is the other of the pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("right of it", 0, 0, 0xFFFFFFFF, true);
    * label.alignHorizontally(panel, "leftOnRight");
    * draw.register();
    * </pre>
    *
    * @param other     the element to align to
    * @param alignment the alignment to use
    * @return self for chaining.
    * @see #alignHorizontally(Alignable, String, int)
    * @since 1.8.4
    */
    default B alignHorizontally(Alignable<?> other, String alignment) {
        return alignHorizontally(other, alignment, 0);
    }

    /**
    * The alignment must be of the format
    * {@code [left|center|right|x%]On[left|center|right|x%]}. The input is case-insensitive.
    * The first alignment is for the element this method is called on and the second is for the
    * other element. As an example, {@code LeftOnCenter} would align the left side of this
    * element to the center of the other element.
    * <p>
    * The split between the two halves is the word {@code on} in the lowercased string,
    * so the string has to contain it or this throws rather than doing nothing, and there
    * are no spaces around it because a space makes either half miss its case. The second
    * half picks a point on the other element and the first moves this one to it, so
    * {@code RightOnLeft} puts this element just to the left of the other one.
    * <p>
    * Either half can use a supported word or an integer percentage from zero to one hundred.
    * Each half is parsed independently: {@code 50%OnLeft}, {@code leftOn50%} and
    * {@code 0%on0%} are all valid. The offset is added to the resulting aligned position.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("down from it", 0, 0, 0xFFFFFFFF, true);
    * // the text's left edge to the panel's centre, ten pixels to the right
    * label.alignHorizontally(panel, "leftOnCenter", 10);
    * draw.register();
    * </pre>
    *
    * @param other     the element to align to
    * @param alignment the alignment to use
    * @param offset    the offset to use
    * @return self for chaining.
    * @throws IllegalArgumentException if the two-part format or either half is unsupported
    * @throws NumberFormatException if a percentage half does not contain a whole number
    * @since 1.8.4
    */
    default B alignHorizontally(Alignable<?> other, String alignment, int offset) {
        String[] alignments = alignment.toLowerCase(Locale.ROOT).split("on", -1);
        if (alignments.length != 2) throw new IllegalArgumentException("Invalid alignment: " + alignment);
        String thisAlignment = alignments[0];
        String toAlignment = alignments[1];
        int alignToX;
        switch (toAlignment) {
            case "left":
                alignToX = other.getScaledLeft();
                break;
            case "center":
                alignToX = other.getScaledLeft() + other.getScaledWidth() / 2;
                break;
            case "right":
                alignToX = other.getScaledRight();
                break;
            default:
                int percent = parsePercentage(toAlignment);
                if (percent != -1) {
                    alignToX = other.getScaledLeft() + (other.getScaledWidth() * percent / 100);
                    break;
                }
                throw new IllegalArgumentException("Invalid alignment: " + alignment);
        }
        switch (thisAlignment) {
            case "left":
                moveToX(alignToX + offset);
                break;
            case "center":
                moveToX(alignToX - getScaledWidth() / 2 + offset);
                break;
            case "right":
                moveToX(alignToX - getScaledWidth() + offset);
                break;
            default:
                int percent = parsePercentage(thisAlignment);
                if (percent != -1) {
                    moveToX(alignToX - (getScaledWidth() * percent / 100) + offset);
                    break;
                }
                throw new IllegalArgumentException("Invalid alignment: " + alignment);
        }
        return (B) this;
    }

    /**
    * lines this element up within its parent, with no offset.
    * <p>
    * The same as passing 0 as the offset, which is the other of the pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("in the middle", 0, 0, 0xFFFFFFFF, true);
    * label.alignHorizontally("center");
    * draw.register();
    * </pre>
    *
    * @param alignment the alignment to use
    * @return self for chaining.
    * @see #alignHorizontally(String, int)
    * @since 1.8.4
    */
    default B alignHorizontally(String alignment) {
        return alignHorizontally(alignment, 0);
    }

    /**
    * Possible alignments are {@code left}, {@code center}, {@code right} or {@code y%} where y
    * is a number between 0 and 100.
    * <p>
    * Measured against the parent, which is the overlay the element was added to, or the
    * window when it has no parent. {@code left} is the parent's left edge,
    * {@code center} centres the element in the parent and {@code right} puts the
    * element's right edge on the parent's right edge. A percentage is a share of the
    * space the element does not fill, so {@code 0%} is flush left and {@code 100%} is
    * flush right, and a halfway element is at its midpoint either way.
    * <p>
    * The offset is added to whichever of those comes out, and it moves along the
    * element rather than moving the edge it is aligned to.
    * <p>
    * A string that is none of these is left alone: nothing moves and nothing is thrown,
    * which is the one place in this interface that is not checked. A percentage-looking
    * string whose number is not a whole number is the one exception, and that throws
    * {@link NumberFormatException}. The form measuring against another element also accepts
    * percentages, with each alignment half parsed separately.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("three quarters across", 0, 0, 0xFFFFFFFF, true);
    * label.alignHorizontally("75%", -5);
    * draw.register();
    * </pre>
    *
    * @param alignment the alignment to use
    * @param offset    the offset to use
    * @return self for chaining.
    * @throws NumberFormatException if the string ends in {@code %} and the number in it
    * is not a whole number
    * @since 1.8.4
    */
    default B alignHorizontally(String alignment, int offset) {
        int parentWidth = getParentWidth();
        int width = getScaledWidth();

        switch (alignment.toLowerCase(Locale.ROOT)) {
            case "left":
                moveToX(offset);
                break;
            case "center":
                moveToX((parentWidth - width) / 2 + offset);
                break;
            case "right":
                moveToX(parentWidth - width + offset);
                break;
            default:
                int percent = parsePercentage(alignment);
                if (percent != -1) {
                    moveToX((parentWidth - width) * percent / 100 + offset);
                }
                break;
        }
        return (B) this;
    }

    /**
    * lines this element up against another one vertically, with no offset.
    * <p>
    * The same as passing 0 as the offset, which is the other of the pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("above it", 0, 0, 0xFFFFFFFF, true);
    * label.alignVertically(panel, "bottomOnTop");
    * draw.register();
    * </pre>
    *
    * @param other     the element to align to
    * @param alignment the alignment to use
    * @return self for chaining.
    * @see #alignVertically(Alignable, String, int)
    * @since 1.8.4
    */
    default B alignVertically(Alignable<?> other, String alignment) {
        return alignVertically(other, alignment, 0);
    }

    /**
    * The alignment must be of the format
    * {@code [top|center|bottom|y%]On[top|center|bottom|y%]}. The input is case-insensitive.
    * The first alignment is for the element this method is called on and the second is for the
    * other element. As an example, {@code BottomOnTop} would align the bottom side of this
    * element to the top of the other element. Thus, the element would be placed above the
    * other one.
    * <p>
    * As with the horizontal pair, the two halves are split on the word {@code on} in the
    * lowercased string, so the string has to contain it and there are no spaces around
    * it. The second half picks a point down the other element and the first moves this
    * one to it.
    * <p>
    * Either half can use a supported word or an integer percentage from zero to one hundred.
    * Each half is parsed independently, so {@code bottomOn50%} and {@code 50%OnTop} are valid.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("in the middle of it", 0, 0, 0xFFFFFFFF, true);
    * label.alignVertically(panel, "centerOnCenter");
    * draw.register();
    * </pre>
    *
    * @param other     the element to align to
    * @param alignment the alignment to use
    * @param offset    the offset to use
    * @return self for chaining.
    * @throws IllegalArgumentException if the two-part format or either half is unsupported
    * @throws NumberFormatException if a percentage half does not contain a whole number
    * @since 1.8.4
    */
    default B alignVertically(Alignable<?> other, String alignment, int offset) {
        String[] alignments = alignment.toLowerCase(Locale.ROOT).split("on", -1);
        if (alignments.length != 2) throw new IllegalArgumentException("Invalid alignment: " + alignment);
        String thisAlignment = alignments[0];
        String toAlignment = alignments[1];
        int alignToY;
        switch (toAlignment) {
            case "top":
                alignToY = other.getScaledTop();
                break;
            case "center":
                alignToY = other.getScaledTop() + other.getScaledHeight() / 2;
                break;
            case "bottom":
                alignToY = other.getScaledBottom();
                break;
            default:
                int percent = parsePercentage(toAlignment);
                if (percent != -1) {
                    alignToY = other.getScaledTop() + (other.getScaledHeight() * percent / 100);
                    break;
                }
                throw new IllegalArgumentException("Invalid alignment: " + alignment);
        }
        switch (thisAlignment) {
            case "top":
                moveToY(alignToY + offset);
                break;
            case "center":
                moveToY(alignToY - getScaledHeight() / 2 + offset);
                break;
            case "bottom":
                moveToY(alignToY - getScaledHeight() + offset);
                break;
            default:
                int percent = parsePercentage(thisAlignment);
                if (percent != -1) {
                    moveToY(alignToY - (getScaledHeight() * percent / 100) + offset);
                    break;
                }
                throw new IllegalArgumentException("Invalid alignment: " + alignment);
        }
        return (B) this;
    }

    /**
    * lines this element up within its parent vertically, with no offset.
    * <p>
    * The same as passing 0 as the offset, which is the other of the pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("half way down", 0, 0, 0xFFFFFFFF, true);
    * label.alignVertically("center");
    * draw.register();
    * </pre>
    *
    * @param alignment the alignment to use
    * @return self for chaining.
    * @see #alignVertically(String, int)
    * @since 1.8.4
    */
    default B alignVertically(String alignment) {
        return alignVertically(alignment, 0);
    }

    /**
    * Possible alignments are {@code top}, {@code center}, {@code bottom} or {@code x%} where x
    * is a number between 0 and 100.
    * <p>
    * Measured against the parent, which is the overlay the element was added to, or the
    * window when it has no parent. {@code top} is the parent's top edge, {@code center}
    * centres the element in the parent and {@code bottom} puts the element's bottom edge
    * on the parent's bottom edge. A percentage is a share of the space the element does
    * not fill, so {@code 0%} is at the top and {@code 100%} is at the bottom.
    * <p>
    * As with the horizontal pair, a string that is none of these is left alone and
    * nothing is thrown, while a percentage-looking string whose number is not a whole
    * number throws {@link NumberFormatException}. A percentage is read the way the
    * words are here; the form measuring against another element also accepts percentages.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("a third of the way down", 0, 0, 0xFFFFFFFF, true);
    * label.alignVertically("33%");
    * draw.register();
    * </pre>
    *
    * @param alignment the alignment to use
    * @param offset    the offset to use
    * @return self for chaining.
    * @throws NumberFormatException if the string ends in {@code %} and the number in it
    * is not a whole number
    * @since 1.8.4
    */
    default B alignVertically(String alignment, int offset) {
        int parentHeight = getParentHeight();
        int height = getScaledHeight();

        switch (alignment.toLowerCase(Locale.ROOT)) {
            case "top":
                moveToY(offset);
                break;
            case "center":
                moveToY((parentHeight - height) / 2 + offset);
                break;
            case "bottom":
                moveToY(parentHeight - height + offset);
                break;
            default:
                int percent = parsePercentage(alignment);
                if (percent != -1) {
                    moveToY((parentHeight - height) * percent / 100 + offset);
                }
                break;
        }
        return (B) this;
    }

    /**
    * aligns both ways at once within the parent, with no offset on either.
    * <p>
    * The horizontal one runs first, so the vertical offset is applied to the position
    * the horizontal one chose.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("in the middle", 0, 0, 0xFFFFFFFF, true);
    * label.align("center", "center");
    * draw.register();
    * </pre>
    *
    * @param horizontal the horizontal alignment to use
    * @param vertical   the vertical alignment to use
    * @return self for chaining.
    * @see #align(String, int, String, int)
    * @since 1.8.4
    */
    default B align(String horizontal, String vertical) {
        return align(horizontal, 0, vertical, 0);
    }

    /**
    * aligns both ways at once within the parent, with an offset on each.
    * <p>
    * The horizontal one runs first and the vertical one lands on the result, so the two
    * offsets are independent of each other.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("middle, nudged", 0, 0, 0xFFFFFFFF, true);
    * label.align("center", 4, "center", -4);
    * draw.register();
    * </pre>
    *
    * @param horizontal       the horizontal alignment to use
    * @param horizontalOffset the horizontal offset to use
    * @param vertical         the vertical alignment to use
    * @param verticalOffset   the vertical offset to use
    * @return self for chaining.
    * @see #alignHorizontally(String, int)
    * @see #alignVertically(String, int)
    * @since 1.8.4
    */
    default B align(String horizontal, int horizontalOffset, String vertical, int verticalOffset) {
        return alignHorizontally(horizontal, horizontalOffset).alignVertically(vertical, verticalOffset);
    }

    /**
    * aligns both ways at once against another element, with no offset on either.
    * <p>
    * The horizontal one runs first and the vertical one lands on the result, as with the
    * parent-relative pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("in the middle of it", 0, 0, 0xFFFFFFFF, true);
    * label.align(panel, "centerOnCenter", "centerOnCenter");
    * draw.register();
    * </pre>
    *
    * @param other      the element to align to
    * @param horizontal the horizontal alignment to use
    * @param vertical   the vertical alignment to use
    * @return self for chaining.
    * @see #align(Alignable, String, int, String, int)
    * @since 1.8.4
    */
    default B align(Alignable<?> other, String horizontal, String vertical) {
        return align(other, horizontal, 0, vertical, 0);
    }

    /**
    * aligns both ways at once against another element, with an offset on each.
    * <p>
    * The horizontal one runs first and the vertical one lands on the result, as with the
    * parent-relative pair.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const panel = draw.addRect(0, 0, 200, 100, 0x80000000);
    * const label = draw.addText("middle of it, nudged", 0, 0, 0xFFFFFFFF, true);
    * label.align(panel, "centerOnCenter", 0, "centerOnCenter", 0);
    * draw.register();
    * </pre>
    *
    * @param other            the element to align to
    * @param horizontal       the horizontal alignment to use
    * @param horizontalOffset the horizontal offset to use
    * @param vertical         the vertical alignment to use
    * @param verticalOffset   the vertical offset to use
    * @return self for chaining.
    * @see #alignHorizontally(Alignable, String, int)
    * @see #alignVertically(Alignable, String, int)
    * @since 1.8.4
    */
    default B align(Alignable<?> other, String horizontal, int horizontalOffset, String vertical, int verticalOffset) {
        return alignHorizontally(other, horizontal, horizontalOffset).alignVertically(other, vertical, verticalOffset);
    }

    /**
    * puts this element at a position, which for most of them also resizes it to keep the
    * size it already has.
    * <p>
    * This is the one method of the group a class has to supply, and everything else here
    * is built on it: the two single-axis methods keep the other axis as it is, and the
    * align methods are written in terms of those. An element whose two corners can be in
    * either order, such as a rectangle, keeps its current width and height and puts them
    * back from the new position.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(0, 0, 100, 50, 0xFFFF0000);
    * // the same size, at a new corner
    * rect.moveTo(20, 30);
    * draw.register();
    * </pre>
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    * @since 1.8.4
    */
    B moveTo(int x, int y);

    /**
    * moves this element along the x axis and leaves the y axis alone.
    * <p>
    * The y position is read back first and handed to {@link #moveTo(int, int)}, so this
    * is the vertical one and the other left alone.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("moved sideways", 0, 0, 0xFFFFFFFF, true);
    * label.moveToX(40);
    * draw.register();
    * Chat.log(`x is ${label.getX()} and y is still ${label.getY()}`);
    * </pre>
    *
    * @param x the new x position
    * @return self for chaining.
    * @since 1.8.4
    */
    default B moveToX(int x) {
        return moveTo(x, getScaledTop());
    }

    /**
    * moves this element along the y axis and leaves the x axis alone.
    * <p>
    * The x position is read back first and handed to {@link #moveTo(int, int)}, so this
    * is the horizontal one and the other left alone.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("moved down", 0, 0, 0xFFFFFFFF, true);
    * label.moveToY(40);
    * draw.register();
    * Chat.log(`y is ${label.getY()} and x is still ${label.getX()}`);
    * </pre>
    *
    * @param y the new y position
    * @return self for chaining.
    * @since 1.8.4
    */
    default B moveToY(int y) {
        return moveTo(getScaledLeft(), y);
    }

    /**
    * the width of this element as it is drawn, which is the plain width put through
    * the scale.
    * <p>
    * This is what the align methods measure, so an element that is scaled is aligned on
    * the size it appears at rather than the size it was given. The result is cut to a
    * whole number of pixels, so it rounds down.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("scaled", 0, 0, 0xFFFFFFFF, true);
    * label.setScale(2);
    * Chat.log(`drawn ${label.getScaledWidth()} wide, out of ${label.getWidth()} unscaled`);
    * </pre>
    *
    * @return the scaled width of the element.
    * @since 1.8.4
    */
    int getScaledWidth();

    /**
    * the width of the parent element, which is what the parent-relative align methods
    * measure against.
    * <p>
    * An element that was added to an overlay gets that overlay's width. An element with
    * no parent of its own gets the width of the window after the GUI scale, which is
    * the same space its coordinates are in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("measured against this", 0, 0, 0xFFFFFFFF, true);
    * Chat.log(`the parent is ${label.getParentWidth()} wide`);
    * </pre>
    *
    * @return the width of the parent element.
    * @since 1.8.4
    */
    int getParentWidth();

    /**
    * the height of this element as it is drawn, which is the plain height put through
    * the scale.
    * <p>
    * The counterpart of {@link #getScaledWidth()}, and cut to a whole number of pixels
    * the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("scaled", 0, 0, 0xFFFFFFFF, true);
    * label.setScale(2);
    * Chat.log(`drawn ${label.getScaledHeight()} tall, out of ${label.getHeight()} unscaled`);
    * </pre>
    *
    * @return the scaled height of the element.
    * @since 1.8.4
    */
    int getScaledHeight();

    /**
    * the height of the parent element, which is what the parent-relative align methods
    * measure against.
    * <p>
    * The counterpart of {@link #getParentWidth()}, and the window's height after the GUI
    * scale for an element with no parent of its own.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("measured against this", 0, 0, 0xFFFFFFFF, true);
    * Chat.log(`the parent is ${label.getParentHeight()} tall`);
    * </pre>
    *
    * @return the height of the parent element.
    * @since 1.8.4
    */
    int getParentHeight();

    /**
    * the left edge of this element, which is the position the align methods move.
    * <p>
    * The two corners of an element can be given in either order, and this is the
    * smaller of the two, so an element drawn left to right and one drawn right to left
    * report the same left edge.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(100, 0, 20, 50, 0xFFFF0000);
    * // the corners were the other way round, and the left edge is the smaller one
    * Chat.log(`left edge is ${rect.getScaledLeft()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side.
    * @since 1.8.4
    */
    int getScaledLeft();

    /**
    * the top edge of this element, which is the position the align methods move.
    * <p>
    * The counterpart of {@link #getScaledLeft()} and the smaller of the two y corners.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(0, 100, 50, 20, 0xFFFF0000);
    * Chat.log(`top edge is ${rect.getScaledTop()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side.
    * @since 1.8.4
    */
    int getScaledTop();

    /**
    * the right edge of this element, worked out from the left edge and the width.
    * <p>
    * Nothing sets this directly, so it always follows the other two: an element moved or
    * resized has a right edge that follows it. That makes it the word the two-element
    * align methods read for a {@code right} half.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setWidth(50);
    * Chat.log(`right edge is ${rect.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's right side.
    * @since 1.8.4
    */
    default int getScaledRight() {
        return getScaledLeft() + getScaledWidth();
    }

    /**
    * the bottom edge of this element, worked out from the top edge and the height.
    * <p>
    * The counterpart of {@link #getScaledRight()}, and the same arrangement: nothing
    * sets it, so it follows the other two.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setHeight(20);
    * Chat.log(`bottom edge is ${rect.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's bottom side.
    * @since 1.8.4
    */
    default int getScaledBottom() {
        return getScaledTop() + getScaledHeight();
    }

    /**
    * Parse the string containing a percentage of the form {@code x%} and return its value.
    * <p>
    * A whole number from 0 to 100 with a {@code %} on the end. Only two things come
    * back as {@code -1}: a whole number that is outside that range, and a string with
    * no {@code %} on the end at all. Anything else that does end in {@code %} is
    * handed straight to {@link Integer#parseInt(String)}, so a decimal such as
    * {@code 50.5%} and a leading space such as {@code " 50%"} throw rather than
    * reading as no percentage. The {@code +} sign is accepted, because it is part of
    * the number and not part of the percentage.
    * <p>
    * This is what the align methods in this interface fall back on, and it is a static
    * method, so it can be read on the interface itself.
    * example:
    * <pre>
    * const Alignable = Java.type("com.jsmacrosce.jsmacros.client.api.classes.render.components.Alignable");
    * Chat.log(`${Alignable.parsePercentage("50%")} and ${Alignable.parsePercentage("101%")}`);
    * try {
    *   Alignable.parsePercentage("50.5%");
    * } catch (e) {
    *   // a decimal is not a whole number, so this throws rather than giving -1
    *   Chat.log(`50.5% threw: ${e}`);
    * }
    * </pre>
    *
    * @param string the string to parse
    * @return the percentage as a whole number from 0 to 100, or {@code -1} if the string
    * has no {@code %} on the end or the number in front of it is outside that range.
    * @throws NumberFormatException if the string ends in {@code %} and what is in front
    * of it is not a whole number, such as {@code abc%}
    * @since 1.8.4
    */
    static int parsePercentage(String string) {
        if (string.endsWith("%")) {
            int percent = Integer.parseInt(string.substring(0, string.length() - 1));
            if (percent >= 0 && percent <= 100) {
                return percent;
            }
        }
        return -1;
    }

}
