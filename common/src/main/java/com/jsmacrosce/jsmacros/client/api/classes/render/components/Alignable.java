package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import java.util.Locale;

 /**
 * Provides fluent horizontal and vertical alignment for render elements. Parent-relative
 * methods use the overlay parent, or the window when there is no parent; overloads taking
 * another {@code Alignable} position this element against that element. Alignment methods
 * move the element and return its concrete self type.
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
    * Aligns this element horizontally against {@code other} with zero offset.
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
    * Aligns this element horizontally against {@code other}. The case-insensitive alignment
    * has the form {@code [left|center|right|x%]On[left|center|right|x%]}; the first part
    * selects this element's anchor and the second selects the anchor on {@code other}.
    * Percentages are whole numbers from 0 to 100. The offset is added to this element's
    * resulting x position. Unlike parent-relative alignment, malformed alignments throw.
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
    * Aligns this element horizontally within its parent with zero offset.
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
    * Aligns this element horizontally within its parent using {@code left}, {@code center},
    * {@code right}, or an integer percentage from 0 to 100. Percentages position the element
    * within the remaining parent width after its own width; the offset is added to its x
    * position. Unsupported strings do nothing; a string ending in {@code %} with a
    * non-integer number throws {@link NumberFormatException}. Out-of-range percentages do nothing.
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
    * Aligns this element vertically against {@code other} with zero offset.
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
    * Aligns this element vertically against {@code other}. The case-insensitive alignment
    * has the form {@code [top|center|bottom|y%]On[top|center|bottom|y%]}; the first part
    * selects this element's anchor and the second selects the anchor on {@code other}.
    * Percentages are whole numbers from 0 to 100. The offset is added to this element's
    * resulting y position. Malformed alignments throw.
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
    * Aligns this element vertically within its parent with zero offset.
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
    * Aligns this element vertically within its parent using {@code top}, {@code center},
    * {@code bottom}, or an integer percentage from 0 to 100.
    * <p>
    * Measured against the parent, which is the overlay the element was added to, or the
    * window when it has no parent. {@code top} is the parent's top edge, {@code center}
    * centres the element in the parent and {@code bottom} puts the element's bottom edge
    * on the parent's bottom edge. A percentage is a share of the space the element does
    * not fill, so {@code 0%} is at the top and {@code 100%} is at the bottom.
    * <p>
    * A percentage positions the element within the space remaining after its height;
    * the offset is added to the resulting y position. Unsupported strings do nothing.
    * A string ending in {@code %} with a non-integer number throws
    * {@link NumberFormatException}; out-of-range integers are ignored.
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
    * Aligns this element horizontally and vertically within its parent, with zero offset
    * on both axes.
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
    * Aligns this element horizontally and vertically within its parent, applying an
    * independent offset on each axis.
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
    * Aligns this element horizontally and vertically against {@code other}, with zero
    * offset on both axes.
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
    * Aligns this element horizontally and vertically against {@code other}, applying an
    * independent offset on each axis.
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
    * Sets this element's position. Implementations determine how its dimensions are
    * preserved; the single-axis helpers use this method while retaining the other axis.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    * @since 1.8.4
    */
    B moveTo(int x, int y);

    /**
    * Moves this element to {@code x}, retaining its current top coordinate.
    *
    * @param x the new x position
    * @return self for chaining.
    * @since 1.8.4
    */
    default B moveToX(int x) {
        return moveTo(x, getScaledTop());
    }

    /**
    * Moves this element to {@code y}, retaining its current left coordinate.
    *
    * @param y the new y position
    * @return self for chaining.
    * @since 1.8.4
    */
    default B moveToY(int y) {
        return moveTo(getScaledLeft(), y);
    }

    /**
    * Returns the element's width after scaling, truncated to an integer; alignment uses
    * this width.
    *
    * @return the scaled width of the element.
    * @since 1.8.4
    */
    int getScaledWidth();

    /**
    * Returns the width used as the reference by parent-relative alignment.
    *
    * @return the width of the parent element.
    * @since 1.8.4
    */
    int getParentWidth();

    /**
    * Returns this element's height after scaling, truncated to an integer; alignment uses
    * this height.
    *
    * @return the scaled height of the element.
    * @since 1.8.4
    */
    int getScaledHeight();

    /**
    * Returns the height used as the reference by parent-relative alignment.
    *
    * @return the height of the parent element.
    * @since 1.8.4
    */
    int getParentHeight();

    /**
    * Returns this element's left edge, used as its x position by alignment methods.
    *
    * @return the position of the scaled element's left side.
    * @since 1.8.4
    */
    int getScaledLeft();

    /**
    * Returns this element's top edge, used as its y position by alignment methods.
    *
    * @return the position of the scaled element's top side.
    * @since 1.8.4
    */
    int getScaledTop();

    /**
    * Returns the right edge, calculated as the left edge plus the scaled width.
    *
    * @return the position of the scaled element's right side.
    * @since 1.8.4
    */
    default int getScaledRight() {
        return getScaledLeft() + getScaledWidth();
    }

    /**
    * Returns the bottom edge, calculated as the top edge plus the scaled height.
    *
    * @return the position of the scaled element's bottom side.
    * @since 1.8.4
    */
    default int getScaledBottom() {
        return getScaledTop() + getScaledHeight();
    }

    /**
    * Parses an integer percentage with a trailing {@code %}. Values outside 0–100 and
    * strings without the suffix return {@code -1}; a suffixed non-integer throws
    * {@link NumberFormatException}.
    *
    * @param string the string to parse
    * @return the percentage as a whole number from 0 to 100, or {@code -1} if the string
    * has no {@code %} on the end or the number in front of it is outside that range.
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
