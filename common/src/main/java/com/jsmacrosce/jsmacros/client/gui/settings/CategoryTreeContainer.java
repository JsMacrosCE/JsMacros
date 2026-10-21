package com.jsmacrosce.jsmacros.client.gui.settings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Builds a settings-category tree whose nodes request category selection through a supplied parent.
 * The public constructor makes a head node. Add paths before calling {@code init()} on the head; the parent host registers the controls.
 * Paths added afterward are not initialized automatically, and repeated initialization does not remove earlier registered controls.
 * <pre>
 * function makeCategoryTree(parent, font) {
 *   const CategoryTree = Java.type('com.jsmacrosce.jsmacros.client.gui.settings.CategoryTreeContainer');
 *   const tree = new CategoryTree(10, 10, 160, 120, font, parent);
 *   tree.addCategory(Java.to(['settings', 'general'], 'java.lang.String[]'));
 *   tree.init();
 *   return tree;
 * }
 * </pre>
 * Here, {@code parent} and {@code font} are supplied by the caller.
 */
public class CategoryTreeContainer extends MultiElementContainer<ICategoryTreeParent> implements ICategoryTreeParent {
    /** Path segment for this node; the public head uses the {@code "HEAD"} sentinel, which is not forwarded as a path segment. */
    public final String category;
    /** Head scrollbar used for page-count updates; null before head initialization and normally null on child nodes.
     * Head initialization assigns a newly registered scrollbar; field reassignment alone does not replace the registered control. */
    public Scrollbar scroll;
    /** Initially empty mutable map from immediate path segments to child nodes, reused by {@code addCategory}.
     * External mutations do not initialize or register controls or establish parent links; layout sorts keys separately. */
    public Map<String, CategoryTreeContainer> children = new HashMap<>();
    private boolean showChildren;
    /** Expand/collapse control created during head initialization for a non-head node with children; null before initialization and normally on heads and leaves.
     * Field reassignment alone does not replace the registered control. */
    public Button expandBtn;
    /** Selection control created during head initialization for a non-head node; null beforehand and normally on the head.
     * Its label uses the category as a translation key; field reassignment alone does not replace the registered control. */
    public Button showBtn;
    /** Whether this node is the tree head: true for the public constructor and false for nodes created by {@code addCategory}.
     * Changing this routing, layout and initialization discriminator does not rebuild controls or establish a different parent. */
    public boolean isHead;
    /** Head vertical scroll offset in GUI units, initially zero and reset by head initialization.
     * Direct writes do not relayout; {@code onScrollbar} stores the integer part of height times its page argument and updates offsets. */
    public int topScroll;
    /** Stored row height in GUI units; the head starts at zero and new child nodes use the font line height plus three.
     * Direct writes do not resize existing controls or update offsets automatically. */
    public int btnHeight;

    private CategoryTreeContainer(int x, int y, int width, int height, Font textRenderer, ICategoryTreeParent parent, String category) {
        super(x, y, width, height, textRenderer, parent);
        this.isHead = false;
        this.showChildren = false;
        this.category = category;
        this.btnHeight = textRenderer.lineHeight + 3;
    }

    /** Stores geometry, font and parent for an initially expanded, empty head with category {@code "HEAD"} and zero row height.
     * Does not initialize or register controls; call {@code init()} on this head after adding paths.
     * @param x left coordinate in GUI space
     * @param y top coordinate in GUI space
     * @param width tree width in GUI units
     * @param height tree height in GUI units
     * @param textRenderer non-null font used to size child rows
     * @param parent non-null parent used for widget registration and category-selection requests
     */
    public CategoryTreeContainer(int x, int y, int width, int height, Font textRenderer, ICategoryTreeParent parent) {
        super(x, y, width, height, textRenderer, parent);
        this.isHead = true;
        this.showChildren = true;
        this.category = "HEAD";
        this.btnHeight = 0;
    }

    /**
     * Adds or reuses nodes along an ordered path relative to this node. New nodes are not initialized or registered.
     * A nonempty path added through the head updates offsets and any existing scrollbar page count; adding through a non-head does not.
     * An empty path returns immediately without layout updates. This operation does not select or expand nodes.
     * @param category ordered non-null path segments relative to this node
     * @return the terminal node, or this node for an empty path
     */
    public CategoryTreeContainer addCategory(String... category) {
        if (category.length > 0) {
            String[] childCategory = new String[category.length - 1];
            System.arraycopy(category, 1, childCategory, 0, childCategory.length);
            CategoryTreeContainer newChild = children.computeIfAbsent(category[0], (cat) -> new CategoryTreeContainer(x + (isHead ? 0 : btnHeight / 2), y, width - (isHead ? 8 : btnHeight / 2), btnHeight, textRenderer, this, cat)).addCategory(childCategory);
            if (isHead) {
                if (scroll != null) {
                    scroll.setScrollPages(children.values().stream().map(e -> e.height).reduce(Integer::sum).orElse(0) / (double) height);
                }
                updateOffsets();
            }
            return newChild;
        }
        return this;
    }

    /** Forwards a selection request to the parent; non-head nodes prepend their own segment, while the head forwards the supplied array unchanged.
     * An empty path requests the current non-head node's full path, without the head sentinel. No local selection state,
     * path validation, node creation or expansion is performed; the receiving parent decides how to handle the request.
     * @param category ordered relative path segments to append after this node; the head forwards this path unchanged
     */
    @Override
    public void selectCategory(String... category) {
        if (isHead) {
            parent.selectCategory(category);
            return;
        }
        String[] childCategory = new String[category.length + 1];
        childCategory[0] = this.category;
        System.arraycopy(category, 0, childCategory, 1, category.length);
        parent.selectCategory(childCategory);
    }

    /** Recomputes node layout and existing button positions and visibility from the head.
     * Child rows are laid out in sorted path-key order; collapsed subtrees do not advance the visible layout. Buttons are visible only when their full row is within the head viewport and every ancestor is expanded. This does not create controls, resize node buttons or change their active flags, or consult the container visibility flag.
     * Child rows begin at the head y-coordinate minus its scroll offset; child-node heights are recomputed while the head keeps its viewport height. If present, the head scrollbar's page count is updated from the computed layout height. This can change the scrollbar's size, active and visible states;
     * a page count below one resets it and notifies its callback, which can reset the head offset and relayout again.
     */
    public void updateOffsets() {
        if (isHead) {
            int maxHeight = updateOffsets(y, y, y + height, showChildren);
            if (scroll != null) {
                scroll.setScrollPages((maxHeight + 4) / (double) height);
            }
        } else {
            getHead().updateOffsets();
        }
    }

    private int updateOffsets(int y, int minShow, int maxShow, boolean parentShowChildren) {
        this.y = y;
        if (this.expandBtn != null) {
            this.expandBtn.setY(y);
            if (y < minShow || y + btnHeight > maxShow) {
                this.expandBtn.visible = false;
            } else {
                this.expandBtn.visible = parentShowChildren;
            }
        }
        if (this.showBtn != null) {
            this.showBtn.setY(y);
            if (y < minShow || y + btnHeight > maxShow) {
                this.showBtn.visible = false;
            } else {
                this.showBtn.visible = parentShowChildren;
            }
        }
        if (!this.isHead) {
            this.height = btnHeight;
        }
        int top = isHead ? y - topScroll : y + btnHeight;
        Iterator<CategoryTreeContainer> iterator = children.keySet().stream().sorted().map(e -> children.get(e)).iterator();
        while (iterator.hasNext()) {
            CategoryTreeContainer child = iterator.next();

            int incr = child.updateOffsets(top, minShow, maxShow, showChildren && parentShowChildren);
            if (showChildren && parentShowChildren) {
                if (!this.isHead) {
                    this.height += child.height;
                }
                top += incr;
            }
        }
        return top - y + topScroll;
    }

    /** Initializes the tree from its head, registering a scrollbar and controls for the existing descendants with the parent.
     * Resets the head scroll offset and lays out the controls. Calling this again registers additional controls without clearing tracking or removing previously registered ones.
     * Existing node expansion flags are retained and no category-selection request is made. A non-head call throws a runtime exception before doing any work.
     */
    @Override
    public void init() {
        if (this.isHead) {
            topScroll = 0;
            scroll = addRenderableWidget(new Scrollbar(x + width - 8, y, 8, height, 0, 0xFF000000, 0xFFFFFFFF, 2, this::onScrollbar));
            for (CategoryTreeContainer child : children.values()) {
                child.initChild(showChildren);
            }
            updateOffsets();
        } else {
            throw new RuntimeException("attempted to init non-head of category tree");
        }
    }

    private void initChild(boolean show) {
        if (children.size() > 0) {
            expandBtn = addRenderableWidget(new Button(x, y, btnHeight, btnHeight, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(">"), (btn) -> this.toggleExpand()));
            expandBtn.visible = show;
        }
        showBtn = addRenderableWidget(new Button(x + btnHeight, y, width - btnHeight, btnHeight, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable(category), (btn) -> this.selectCategory()));
        showBtn.visible = show;
        showBtn.horizCenter = false;

        for (CategoryTreeContainer child : children.values()) {
            child.initChild(showChildren);
        }
    }

    private void toggleExpand() {
        showChildren = !showChildren;
        expandBtn.setMessage(Component.literal(showChildren ? "<" : ">"));
        updateOffsets();
    }

    private CategoryTreeContainer getHead() {
        if (!this.isHead) {
            return ((CategoryTreeContainer) parent).getHead();
        }
        return this;
    }

    /** Stores {@code (int) (height * page)} as this node's scroll offset and relayouts only when that offset changes.
     * Intended for the head scrollbar: a non-head call changes that node's offset but delegates relayout to the head without changing the head offset.
     * This does not set the thumb to the requested page. Relayout updates the scrollbar page count, which can reset the thumb
     * and notify this callback again with a zero offset when the content occupies less than one page.
     * @param page scroll amount in viewport-height units; for example, {@code 0.5} is half the viewport height. The argument is not clamped directly; a scrollbar page-count reset during relayout can replace the resulting head offset with zero.
     */
    public void onScrollbar(double page) {
        int newTopScroll = (int) (height * page);
        if (newTopScroll != topScroll) {
            topScroll = newTopScroll;
            updateOffsets();
        }
    }

    /** This rendering hook submits no content; the host renders registered widgets separately.
     * @param drawContext graphics context, unused
     * @param mouseX mouse x-coordinate in GUI space, unused
     * @param mouseY mouse y-coordinate in GUI space, unused
     * @param delta caller-supplied frame value, unused
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }

    }

}
