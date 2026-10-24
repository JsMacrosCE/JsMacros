package com.jsmacrosce.jsmacros.client.gui.overlays;

import com.jsmacrosce.jsmacros.client.util.PlatformCompat;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;
import com.jsmacrosce.wagyourgui.overlays.ConfirmOverlay;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;
import com.jsmacrosce.wagyourgui.overlays.TextPrompt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.Util;

/** Browses native Java files with optional selection and edit callbacks. Construct the overlay, set {@code root} before opening when needed, then open it through its parent to initialize its controls. The root is a navigation reference, not a filesystem boundary; relative paths may escape it.
 * <pre>
 * function openFileChooser(parent, font, directory, onSelect, onEdit) {
 *   const FileChooser = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.FileChooser');
 *   const chooser = new FileChooser(10, 10, 320, 180, font, directory, null, parent,
 *     JavaWrapper.methodToJava(onSelect), JavaWrapper.methodToJava(onEdit));
 *   chooser.root = directory;
 *   parent.openOverlay(chooser);
 *   return chooser;
 * }
 * </pre>
 * The caller supplies a listable native Java directory, native font, overlay parent and JavaScript callback functions.
 */
@DocletCategory("Screen and UI Elements")
public class FileChooser extends OverlayContainer {
    private File directory;
    private Component dirname;
    private File selected;
    /** Expected non-null navigation base used for the displayed relative path, parent-row boundary and failed-directory-creation fallback; initially the configured macro folder. It is not a security boundary, and changing it does not itself navigate or refresh the listing. */
    public File root = JsMacrosClient.clientCore.config.macroFolder;
    private final List<fileObj> files = new ArrayList<>();
    private final Consumer<File> setFile;
    private final Consumer<File> editFile;
    private int topScroll;

    /** Stores the overlay geometry, browser state and callbacks; controls are registered when the parent opens and initializes this overlay.
     * @param x left coordinate in GUI space
     * @param y top coordinate in GUI space
     * @param width width in GUI units
     * @param height height in GUI units
     * @param textRenderer non-null native font for the controls
     * @param directory non-null initial browse directory, listable or creatable during initialization
     * @param selected nullable file initially selected during initialization
     * @param parent non-null overlay host
     * @param setFile nullable callback invoked by the Select button when a selection is stored; null makes Select do nothing, and the overlay closes only after a normal callback return
     * @param editFile callback invoked by the Edit button with the stored selection; it is not null-checked and does not close the overlay
     */
    public FileChooser(int x, int y, int width, int height, Font textRenderer, File directory, File selected, IOverlayParent parent, Consumer<File> setFile, Consumer<File> editFile) {
        super(x, y, width, height, textRenderer, parent);
        this.setFile = setFile;
        this.directory = directory;
        this.selected = selected;
        this.editFile = editFile;
    }

    /** Rebuilds the listing for {@code dir}, attempting to create it when absent. Requires an initialized scrollbar and usable root; old rows are removed before filesystem checks.
     * Failed creation recursively attempts {@code root}; there is no cycle guard if that directory cannot be created either.
     * The heading is relative to the absolute root path, not a canonical containment check. Entries are sorted directories first, then by name.
     * A null directory listing is not treated as empty; unchecked filesystem or path failures can leave a partial rebuild.
     * Existing selection is retained. Row additions update scrollbar page count, which can reset the offset through its callback.
     * @param dir non-null directory to browse, listable or creatable and with a path compatible with the root for relativization
     */
    public void setDir(File dir) {
        for (fileObj f : files) {
            this.removeWidget(f.btn);
        }
        files.clear();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                setDir(root);
                return;
            }
        }

        this.directory = dir;
        this.dirname = Component.literal("./" + root.getAbsoluteFile().toPath().relativize(dir.getAbsoluteFile().toPath()).toString().replaceAll("\\\\", "/"));

        if (!this.directory.equals(root)) {
            addFile(this.directory.getParentFile(), "..");
        }

        List<File> files = new ArrayList<>(Arrays.asList(directory.listFiles()));
        files.sort(new sortFile());
        for (File f : files) {
            addFile(f);
        }
    }

    /** Navigates into a directory without clearing the stored file selection, or stores a non-directory file as the selection, then updates row highlighting against the supplied file.
     * Does not require a listed or existing file, invoke either callback, confirm a choice or close the overlay.
     * @param f file or directory to navigate to or select
     */
    public void selectFile(File f) {
        if (f.isDirectory()) {
            this.setDir(f);
        } else {
            this.selected = f;
        }
        for (fileObj fi : files) {
            if (f.equals(fi.file)) {
                fi.btn.setColor(0x7FFFFFFF);
            } else {
                fi.btn.setColor(0);
            }
        }
    }

    /** Clears local tracking, registers new controls and a scrollbar, resets the row origin to {@code y + 13}, then rebuilds the directory listing.
     * Opening through the parent calls this. The listing rebuild separately removes old file rows from the parent, but repeated initialization leaves prior non-row controls registered.
     * A non-null initial selection is passed to {@code selectFile}, so a directory selection can navigate again; initialization does not notify the selection or edit consumers.
     * Filesystem failures can leave partially initialized controls.
     */
    @Override
    public void init() {
        super.init();
        int w = width - 4;
        topScroll = y + 13;
        this.addRenderableWidget(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("X"), (btn) -> this.close()));
        scroll = this.addRenderableWidget(new Scrollbar(x + width - 10, y + 13, 8, height - 28, 0, 0xFF000000, 0xFFFFFFFF, 2, this::onScrollbar));

        this.addRenderableWidget(new Button(x + w * 5 / 6 + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.select"), (btn) -> {
            if (this.selected != null && this.setFile != null) {
                this.setFile.accept(this.selected);
                this.close();
            }
        }));

        this.addRenderableWidget(new Button(x + w * 4 / 6 + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("selectWorld.edit"), (btn) -> {
            if (this.selected != null) {
                editFile.accept(selected);
            }
        }));

        this.addRenderableWidget(new Button(x + w * 3 / 6 + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.rename"), (btn) -> {
            if (selected != null) {
                this.openOverlay(new TextPrompt(x + width / 2 - 100, y + height / 2 - 50, 200, 100, textRenderer, Component.translatable("jsmacrosce.filename"), selected.getName(), this, (str) -> {
                    File f = new File(directory, str);
                    if (selected.renameTo(f)) {
                        this.setDir(directory);
                        this.selectFile(f);
                    }
                }));
            }
        }));

        this.addRenderableWidget(new Button(x + w * 2 / 6 + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("selectWorld.delete"), (btn) -> {
            if (this.selected != null && this.selected.isFile()) {
                fileObj f = null;
                for (fileObj fi : files) {
                    if (fi.file.equals(this.selected)) {
                        f = fi;
                        break;
                    }
                }
                if (f != null) {
                    confirmDelete(f);
                }
            }
        }));

        this.addRenderableWidget(new Button(x + w / 6 + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.new"), (btn) -> this.openOverlay(new TextPrompt(x + width / 2 - 100, y + height / 2 - 50, 200, 100, textRenderer, Component.translatable("jsmacrosce.filename"), "", this, (str) -> {
            if (str.trim().equals("")) {
                return;
            }

            // has extension
            File f = new File(directory, str);
            if (JsMacrosClient.clientCore.extensions.getExtensionForFile(f) == null) {
                str += "." + JsMacrosClient.clientCore.extensions.getHighestPriorityExtension().defaultFileExtension();
                f = new File(directory, str);
            }
            try {
                f.createNewFile();
                this.setDir(directory);
                this.selectFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }))));

        this.addRenderableWidget(new Button(x + 2, y + height - 14, w / 6, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.openfolder"), (btn) -> {
            PlatformCompat.openFile(directory);
        }));

        this.setDir(directory);
        if (selected != null) {
            this.selectFile(selected);
        }
    }

    /** Adds a row using the file's name as its literal button label. Requires initialized controls.
     * @param f non-null native file or directory represented by the row
     */
    public void addFile(File f) {
        addFile(f, f.getName());
    }

    /** Appends and registers a row with the supplied literal label, using five columns and twelve-GUI-unit rows. Clicking it calls {@code selectFile}; duplicate entries are not filtered.
     * Requires an initialized scrollbar: the row is appended and registered before the scrollbar page-count update, so failures can leave partial effects.
     * The page-count update can change scrollbar state and notify its callback, repositioning rows.
     * @param f file or directory represented by the row
     * @param btnText literal button label
     */
    public void addFile(File f, String btnText) {
        fileObj file = new fileObj(f, new Button(x + 3 + (files.size() % 5 * (width - 12) / 5), topScroll + (files.size() / 5 * 12), (width - 12) / 5, 12, textRenderer, 0, 0, 0x7FFFFFFF, f.isDirectory() ? 0xFFFFFF00 : 0xFFFFFFFF, Component.literal(btnText), (btn) -> {
            selectFile(f);
        }));
        file.btn.visible = topScroll + (files.size() / 5 * 12) >= y + 13 && topScroll + (files.size() / 5 * 12) <= y + height - 27;
        files.add(file);
        this.addRenderableWidget(file.btn);
        scroll.setScrollPages((Math.ceil(files.size() / 5D) * 12) / (double) Math.max(1, height - 39));
    }

    /** Repositions and resizes existing rows in five columns of twelve-GUI-unit rows using current geometry and scroll position.
     * A row is visible when its top lies between {@code y + 13} and {@code y + height - 27}, inclusive.
     * Does not change active flags, create controls, rebuild the listing or update scrollbar page count. */
    public void updateFilePos() {
        for (int i = 0; i < files.size(); ++i) {
            fileObj f = files.get(i);
            f.btn.visible = topScroll + (i / 5 * 12) >= y + 13 && topScroll + (i / 5 * 12) <= y + height - 27;
            f.btn.setPos(x + 3 + (i % 5 * (width - 12) / 5), topScroll + (i / 5 * 12), (width - 12) / 5, 12);
        }
    }

    /** Opens a confirmation overlay that calls {@code delete(f)} only when confirmed; it does not delete immediately.
     * Captures the mutable row record, not a snapshot of its fields. This method does not verify listing membership or require a regular file.
     * @param f row record captured by the confirmation callback
     */
    public void confirmDelete(fileObj f) {
        this.openOverlay(new ConfirmOverlay(x + width / 2 - 100, y + height / 2 - 50, 200, 100, textRenderer, Component.translatable("jsmacrosce.confirmdeletefile"), this, (conf) -> {
            delete(f);
        }));
    }

    /** Removes the row from tracking and the parent, then attempts {@code File.delete()} and repositions remaining rows.
     * The delete result is ignored: a false result does not restore the row. Unchecked failures interrupt the remaining steps without rollback.
     * Does not confirm, verify membership, require a regular file or recursively delete a directory. Selection and scrollbar page count are not updated.
     * @param f row record whose button and file are used
     */
    public void delete(fileObj f) {
        removeWidget(f.btn);
        files.remove(f);
        f.file.delete();
        updateFilePos();
    }

    /** Sets the row origin to {@code y + 13 - (int) (page * (height - 27))} and repositions rows on every call.
     * The page argument is not clamped; this changes row positions and visibility, not active flags, directory contents, page count or the scrollbar thumb.
     * @param page scrollbar amount in multiples of {@code height - 27}
     */
    public void onScrollbar(double page) {
        topScroll = y + 13 - (int) (page * (height - 27));
        int i = 0;
        for (fileObj fi : files) {
            fi.btn.visible = topScroll + (i / 5 * 12) >= y + 13 && topScroll + (i / 5 * 12) <= y + height - 27;
            fi.btn.setPos(x + 3 + (i % 5 * (width - 12) / 5), topScroll + (i / 5 * 12), (width - 12) / 5, 12);
            ++i;
        }
    }

    /** Submits this chooser's background, directory heading and separators, then delegates widget and child-overlay rendering before submitting popups for truncated hovered buttons. Popup eligibility uses each button's stored hover state and text-fit state; it does not check visibility or active state, and multiple buttons can qualify. Call after initialization so the directory heading is set.
     * @param drawContext graphics context receiving the drawing operations
     * @param mouseX GUI-space mouse x-coordinate forwarded to widgets and used for popup placement
     * @param mouseY GUI-space mouse y-coordinate forwarded to widgets and used for popup placement
     * @param delta frame value forwarded to the superclass
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        renderBackground(drawContext);

        //? if >=26.1 {
        /*drawContext.textWithWordWrap(textRenderer, this.dirname, x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        *///? } else {
        drawContext.drawWordWrap(textRenderer, this.dirname, x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        //? }

        drawContext.fill(x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        drawContext.fill(x + 2, y + height - 15, x + width - 2, y + height - 14, 0xFFFFFFFF);
//        textRenderer.draw(, mouseX, mouseY, color, shadow, matrix, vertexConsumers, seeThrough, backgroundColor, light)
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }

        for (AbstractWidget b : ImmutableList.copyOf(this.buttons)) {
            if (b instanceof Button && ((Button) b).hovering && ((Button) b).cantRenderAllText()) {
                // border
                int width = textRenderer.width(b.getMessage());
                drawContext.fill(mouseX - 3, mouseY, mouseX + width + 3, mouseY + 1, 0x7F7F7F7F);
                drawContext.fill(mouseX + width + 2, mouseY - textRenderer.lineHeight - 3, mouseX + width + 3, mouseY, 0x7F7F7F7F);
                drawContext.fill(mouseX - 3, mouseY - textRenderer.lineHeight - 3, mouseX - 2, mouseY, 0x7F7F7F7F);
                drawContext.fill(mouseX - 3, mouseY - textRenderer.lineHeight - 4, mouseX + width + 3, mouseY - textRenderer.lineHeight - 3, 0x7F7F7F7F);

                // fill
                drawContext.fill(mouseX - 2, mouseY - textRenderer.lineHeight - 3, mouseX + width + 2, mouseY, 0xFF000000);
                //? if >=26.1 {
                /*drawContext.text(textRenderer, b.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
                *///? } else {
                drawContext.drawString(textRenderer, b.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
                //? }
            }
        }
    }

    /** Mutable pair associating a caller-supplied native Java file reference with a project button. Constructing or changing a pair does not register it with a chooser; changing {@code file} also does not change the callback captured by an existing row button.
     * <pre>
     * function makeRowRecord(file, button) {
     *   const Row = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.FileChooser$fileObj');
     *   return new Row(file, button);
     * }
     * </pre>
     */
    @DocletCategory("Screen and UI Elements")
    public static class fileObj {
        /** Native Java file reference used for row lookup and deletion; reassigning it does not rewire an existing row button's callback. */
        public File file;
        /** Button reference used by the chooser for row layout and removal; reassigning it does not replace the registered widget. */
        public Button btn;

        /** Stores the supplied references without copying, validating, or registering them.
         * @param file native Java file reference for row lookup and deletion; expected non-null in normal row use
         * @param btn project button reference for row layout and removal; expected non-null in normal row use
         */
        public fileObj(File file, Button btn) {
            this.file = file;
            this.btn = btn;
        }

    }

    /** Comparator used by the chooser to order directories before other entries, then entries by case-sensitive lexicographic basename.
     * <pre>
     * function compareFiles(a, b) {
     *   const Sorter = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.FileChooser$sortFile');
     *   return new Sorter().compare(a, b);
     * }
     * </pre>
     */
    @DocletCategory("Screen and UI Elements")
    public static class sortFile implements Comparator<File> {
        /** Compares entries by directory status and then case-sensitive basename; it does not compare full paths.
         * @param a first native Java file entry
         * @param b second native Java file entry
         * @return negative when {@code a} sorts first, positive when {@code b} sorts first, or zero for equal directory status and basename even when paths differ
         */
        @Override
        public int compare(File a, File b) {
            if (a.isDirectory() ^ b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            } else {
                return a.getName().compareTo(b.getName());
            }
        }

    }

}
