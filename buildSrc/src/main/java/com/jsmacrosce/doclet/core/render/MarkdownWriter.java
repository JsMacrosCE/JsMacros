package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.FileHandler;
import com.jsmacrosce.MarkdownBuilder;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.ClassGroup;
import com.jsmacrosce.doclet.core.model.ClassDoc;
import com.jsmacrosce.doclet.core.model.DocBodyNode;
import com.jsmacrosce.doclet.core.model.DocComment;
import com.jsmacrosce.doclet.core.model.DocTag;
import com.jsmacrosce.doclet.core.model.DocTagKind;
import com.jsmacrosce.doclet.core.model.DocletModel;
import com.jsmacrosce.doclet.core.model.MemberDoc;
import com.jsmacrosce.doclet.core.model.MemberKind;
import com.jsmacrosce.doclet.core.model.PackageDoc;
import com.jsmacrosce.doclet.core.model.ParamDoc;
import com.jsmacrosce.doclet.core.model.TypeKind;
import com.jsmacrosce.doclet.core.model.TypeRef;
import com.jsmacrosce.doclet.core.util.DocBodyRenderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@DocletIgnore
public class MarkdownWriter {
    private static final String DEFAULT_CATEGORY = "Uncategorized";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, ClassDoc> classByQualifiedName = new HashMap<>();
    private final Map<String, List<ClassDoc>> classByName = new HashMap<>();
    private final Map<String, List<ClassDoc>> classByAlias = new HashMap<>();
    /** Package name → base Javadoc URL for external (non-project) types, e.g. "java.util" → "https://…". */
    private Map<String, String> externalPackages = Map.of();
    private String version;

    public MarkdownWriter() {
    }

    /**
     * Provides a map of Java package names to their external Javadoc base URLs
     * (as produced by the {@code -link} option).  Must be called before {@link #write}.
     */
    public void setExternalPackages(Map<String, String> externalPackages) {
        this.externalPackages = externalPackages == null ? Map.of() : externalPackages;
    }

    public void write(DocletModel model, File outDir, String version, String mcVersion) throws IOException {
        this.version = version;
        indexClasses(model);
        Map<String, List<ClassDoc>> classCategories = groupByCategory(model, ClassGroup.Class);
        Map<String, List<ClassDoc>> eventCategories = groupByCategory(model, ClassGroup.Event);
        Map<String, List<ClassDoc>> libraryCategories = groupByCategory(model, ClassGroup.Library);
        SidebarData sidebarData = new SidebarData(
            version,
            mapToSidebarCategories(classCategories, version),
            mapToSidebarCategories(eventCategories, version),
            mapToSidebarCategories(libraryCategories, version)
        );

        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                File out = new File(outDir, classPath(clz) + ".md");
                File parent = out.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Failed to create package dir " + parent);
                }
                new FileHandler(out).write(renderClass(clz));
            }
        }

        writeGroupIndexes(model, outDir, version, mcVersion, classCategories, eventCategories, libraryCategories);
        writeSidebarData(outDir, sidebarData);
    }

    private String classPath(ClassDoc clz) {
        String pkgPath = clz.packageName().replace('.', '/');
        String namePath = clz.name().replace('$', '.');
        String basePath = pkgPath.isEmpty() ? namePath : pkgPath + "/" + namePath;
        String groupPrefix = groupPathPrefix(clz);
        return groupPrefix.isEmpty() ? basePath : groupPrefix + "/" + basePath;
    }

    private String groupPathPrefix(ClassDoc clz) {
        return switch (clz.group()) {
            case Event -> "events";
            case Class -> "classes";
            case Library -> "libraries";
        };
    }

    private void writeGroupIndexes(
        DocletModel model,
        File outDir,
        String version,
        String mcVersion,
        Map<String, List<ClassDoc>> classCategories,
        Map<String, List<ClassDoc>> eventCategories,
        Map<String, List<ClassDoc>> libraryCategories
    ) throws IOException {
        Map<ClassGroup, List<ClassDoc>> grouped = new java.util.HashMap<>();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                grouped.computeIfAbsent(clz.group(), key -> new ArrayList<>()).add(clz);
            }
        }
        grouped.values().forEach(list ->
            list.sort(Comparator.comparing(ClassDoc::qualifiedName, String.CASE_INSENSITIVE_ORDER))
        );

        new FileHandler(new File(outDir, "index.md"))
            .write(renderOverview(grouped, version, mcVersion));
        new FileHandler(new File(outDir, "libraries.md"))
            .write(renderGroupPage("Libraries", grouped.getOrDefault(ClassGroup.Library, List.of()), true, libraryCategories));
        new FileHandler(new File(outDir, "events.md"))
            .write(renderGroupPage("Events", grouped.getOrDefault(ClassGroup.Event, List.of()), true, eventCategories));
        new FileHandler(new File(outDir, "classes.md"))
            .write(renderGroupPage("Classes", grouped.getOrDefault(ClassGroup.Class, List.of()), false, classCategories));
    }

    private String renderOverview(Map<ClassGroup, List<ClassDoc>> grouped, String version, String mcVersion) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.frontmatter(Map.of("outline", "false"));
        md.heading(1, "JsMacros API Reference");
        md.paragraph(
            "Version: " + MarkdownBuilder.codeSpan(version) + "  \n"
            + "Minecraft: " + MarkdownBuilder.codeSpan(mcVersion)
        );
        md.bulletItem(MarkdownBuilder.link("Libraries", "./libraries.md") + " (" + grouped.getOrDefault(ClassGroup.Library, List.of()).size() + ")");
        md.bulletItem(MarkdownBuilder.link("Events", "./events.md") + " (" + grouped.getOrDefault(ClassGroup.Event, List.of()).size() + ")");
        md.bulletItem(MarkdownBuilder.link("Classes", "./classes.md") + " (" + grouped.getOrDefault(ClassGroup.Class, List.of()).size() + ")");
        md.paragraph("Use the sidebar to browse packages and classes.");
        return md.toString();
    }

    private String renderGroupPage(String title, List<ClassDoc> classes, boolean preferAlias, Map<String, List<ClassDoc>> categories) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.frontmatter(Map.of("outline", "false"));
        md.heading(1, title);
        if ((categories == null || categories.isEmpty()) && classes.isEmpty()) {
            md.paragraph("No entries found.");
            return md.toString();
        }
        if (categories != null && !categories.isEmpty()) {
            for (Map.Entry<String, List<ClassDoc>> entry : categories.entrySet()) {
                md.heading(3, entry.getKey());
                renderGroupEntries(entry.getValue(), preferAlias, md);
            }
            return md.toString();
        }
        renderGroupEntries(classes, preferAlias, md);
        return md.toString();
    }

    private void renderGroupEntries(List<ClassDoc> entries, boolean preferAlias, MarkdownBuilder md) {
        if (entries == null || entries.isEmpty()) {
            md.paragraph("No entries found.");
            return;
        }
        for (ClassDoc clz : entries) {
            String linkText = preferAlias && hasAlias(clz)
                ? clz.alias()
                : clz.qualifiedName();
            String item = MarkdownBuilder.link(linkText, "./" + classPath(clz) + ".md");
            if (preferAlias && hasAlias(clz) && !linkText.equals(clz.qualifiedName())) {
                item += " (" + MarkdownBuilder.codeSpan(clz.qualifiedName()) + ")";
            }
            md.bulletItem(item);
        }
    }

    private boolean hasAlias(ClassDoc clz) {
        return clz.alias() != null && !clz.alias().isBlank();
    }

    private Map<String, List<ClassDoc>> groupByCategory(DocletModel model, ClassGroup targetGroup) {
        Map<String, List<ClassDoc>> categories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if (clz.group() != targetGroup) {
                    continue;
                }
                String category = resolveCategory(clz, targetGroup);
                categories.computeIfAbsent(category, key -> new ArrayList<>()).add(clz);
            }
        }
        for (List<ClassDoc> list : categories.values()) {
            list.sort(Comparator.comparing(this::sidebarSortKey, String.CASE_INSENSITIVE_ORDER));
        }
        return categories;
    }

    private String resolveCategory(ClassDoc clz, ClassGroup targetGroup) {
        String category = clz.category();
        if (category != null && !category.isBlank()) {
            return category;
        }

        ClassDoc current = clz;
        while (current != null) {
            String parentQualifiedName = parentQualifiedName(current);
            if (parentQualifiedName == null) {
                break;
            }
            ClassDoc parent = classByQualifiedName.get(parentQualifiedName);
            if (parent == null) {
                break;
            }
            if (parent.group() == targetGroup && parent.category() != null && !parent.category().isBlank()) {
                return parent.category();
            }
            current = parent;
        }

        return DEFAULT_CATEGORY;
    }

    private String sidebarSortKey(ClassDoc clz) {
        return hasAlias(clz) ? clz.alias() : clz.name();
    }

    private List<SidebarCategory> mapToSidebarCategories(Map<String, List<ClassDoc>> categories, String version) {
        List<SidebarCategory> sections = new ArrayList<>();
        for (Map.Entry<String, List<ClassDoc>> entry : categories.entrySet()) {
            List<SidebarEntry> items = buildSidebarEntries(entry.getValue(), version);
            sections.add(new SidebarCategory(entry.getKey(), items));
        }
        return sections;
    }

    private List<SidebarEntry> buildSidebarEntries(List<ClassDoc> classes, String version) {
        Map<String, SidebarEntryBuilder> builders = new LinkedHashMap<>();
        for (ClassDoc clz : classes) {
            builders.put(clz.qualifiedName(), new SidebarEntryBuilder(clz));
        }

        List<SidebarEntryBuilder> roots = new ArrayList<>();
        for (SidebarEntryBuilder builder : builders.values()) {
            String parentQualifiedName = parentQualifiedName(builder.classDoc());
            SidebarEntryBuilder parent = parentQualifiedName == null ? null : builders.get(parentQualifiedName);
            if (parent == null) {
                roots.add(builder);
                continue;
            }
            parent.children().add(builder);
        }

        sortSidebarBuilders(roots);

        List<SidebarEntry> entries = new ArrayList<>();
        for (SidebarEntryBuilder root : roots) {
            entries.add(toSidebarEntry(root, version));
        }
        return entries;
    }

    @Nullable
    private String parentQualifiedName(ClassDoc clz) {
        String name = clz.name();
        int split = name.lastIndexOf('.');
        if (split < 0) {
            return null;
        }
        String parentName = name.substring(0, split);
        if (parentName.isBlank()) {
            return null;
        }
        return clz.packageName().isBlank() ? parentName : clz.packageName() + "." + parentName;
    }

    private void sortSidebarBuilders(List<SidebarEntryBuilder> builders) {
        builders.sort(Comparator.comparing(builder -> sidebarSortKey(builder.classDoc()), String.CASE_INSENSITIVE_ORDER));
        for (SidebarEntryBuilder builder : builders) {
            sortSidebarBuilders(builder.children());
        }
    }

    private SidebarEntry toSidebarEntry(SidebarEntryBuilder builder, String version) {
        String link = "/" + version + "/" + classPath(builder.classDoc());
        if (builder.children().isEmpty()) {
            return new SidebarItem(displayLabel(builder.classDoc()), link);
        }

        List<SidebarEntry> childEntries = new ArrayList<>();
        for (SidebarEntryBuilder child : builder.children()) {
            childEntries.add(toSidebarEntry(child, version));
        }
        return new SidebarNode(displayLabel(builder.classDoc()), link, childEntries);
    }

    private String displayLabel(ClassDoc clz) {
        if (hasAlias(clz)) {
            return clz.alias();
        }
        return clz.name();
    }

    private void writeSidebarData(File outDir, SidebarData data) throws IOException {
        new FileHandler(new File(outDir, "sidebar-data.json")).write(GSON.toJson(data));
    }

    private interface SidebarEntry {}
    private record SidebarItem(String text, String link) implements SidebarEntry {}
    private record SidebarNode(String name, String link, List<SidebarEntry> items) implements SidebarEntry {}
    private record SidebarEntryBuilder(ClassDoc classDoc, List<SidebarEntryBuilder> children) {
        private SidebarEntryBuilder(ClassDoc classDoc) {
            this(classDoc, new ArrayList<>());
        }
    }
    private record SidebarCategory(String name, List<SidebarEntry> items) {}
    private record SidebarData(String version, List<SidebarCategory> classes, List<SidebarCategory> events, List<SidebarCategory> libraries) {}

    private String renderClass(ClassDoc clz) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.frontmatter(Map.of("outline", "deep"));
        md.heading(1, displayTitle(clz));
        md.paragraph(wrapHtmlWithElemAndAttribs(clz.qualifiedName(), "span", "class=\"qualified-name\""));

        String desc = formatDescription(clz.docComment(), clz);
        String descText = desc.isEmpty() ? "TODO: No description supplied\n" : desc;
        if (clz.group() == ClassGroup.Library) {
            String accessName = clz.alias() == null || clz.alias().isEmpty() ? clz.name() : clz.alias();
            descText += "<br>Accessible in scripts via the global " + MarkdownBuilder.codeSpan(accessName) + " variable.";
        }
        md.paragraph(descText);

        // Skip constructors for libraries
        if (clz.group() != ClassGroup.Library) {
            renderMemberSection(md, clz, MemberKind.CONSTRUCTOR, "Constructors");
        }
        renderMemberSection(md, clz, MemberKind.FIELD, "Fields");
        renderMemberSection(md, clz, MemberKind.METHOD, "Methods");
        return md.toString();
    }

    private String displayTitle(ClassDoc clz) {
        if ((clz.group() == ClassGroup.Event || clz.group() == ClassGroup.Library) && hasAlias(clz)) {
            return clz.alias();
        }
        return clz.name();
    }

    private void renderMemberSection(MarkdownBuilder md, ClassDoc clz, MemberKind kind, String title) {
        List<MemberDoc> members = clz.members().stream()
            .filter(member -> member.kind() == kind)
            .toList();
        if (members.isEmpty()) {
            return;
        }
        md.heading(2, title);

        // Preserve declaration order while grouping by name so that overloads
        // stay adjacent to where the first overload appears in the source.
        LinkedHashMap<String, List<MemberDoc>> byName = new LinkedHashMap<>();
        for (MemberDoc member : members) {
            byName.computeIfAbsent(member.name(), k -> new ArrayList<>()).add(member);
        }

        for (List<MemberDoc> group : byName.values()) {
            renderMemberGroup(md, group, clz);
        }
    }

    /**
     * Renders a named group of one or more members (overloads) under a single
     * H3 heading followed by a unified HTML block.
     *
     * <p>Every member — whether alone or part of a true overload set — is
     * rendered identically: the H3 carries only the bare name (plus a
     * group-level anchor), and each member's signature and documentation body
     * live inside a self-contained {@code <div class="overload-item">} element.
     * Solo members therefore look exactly like individual overloads.
     *
     * <p>In addition to the group anchor on the H3, each {@code overload-item}
     * receives an {@code id} derived from the member's {@code anchorId} so that
     * deep links to a specific overload are possible.
     */
    private void renderMemberGroup(MarkdownBuilder md, List<MemberDoc> members, ClassDoc clz) {
        MemberDoc first = members.getFirst();
        String groupName = first.kind() == MemberKind.CONSTRUCTOR
            ? "new " + first.name()
            : first.name();
        // Use the first member's anchorId as the group anchor so that a field
        // and a same-named method produce distinct H3 anchors ("joinable" vs
        // "joinable-").  Fall back to the bare name only when anchorId is absent.
        String groupSuffix = (first.anchorId() != null && !first.anchorId().isBlank())
            ? first.anchorId()
            : first.name();
        String groupAnchor = clz.qualifiedName() + "_" + groupSuffix;
        md.heading(3, groupName, groupAnchor);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"overload-list\">\n");
        for (MemberDoc member : members) {
            // Per-overload anchor for direct deep-linking.
            String itemId = (member.anchorId() != null && !member.anchorId().isBlank())
                ? clz.qualifiedName() + "_" + member.anchorId()
                : null;
            html.append("<div class=\"overload-item\"");
            if (itemId != null) {
                html.append(" id=\"").append(itemId).append("\"");
            }
            html.append(">\n");
            html.append("<div class=\"overload-sig\">");
            html.append("<code>")
                .append(renderSignatureAsHtml(member, clz))
                .append("</code>");
            if (hasDeprecatedTag(member.docComment())) {
                html.append("<Badge type=\"danger\" text=\"deprecated\" />");
            }
            if (itemId != null) {
                html.append("<a class=\"overload-anchor\" href=\"#").append(itemId).append("\">#</a>");
            }
            html.append("</div>\n");
            String body = buildOverloadBody(member, clz);
            if (!body.isBlank()) {
                html.append("<div class=\"overload-body\">\n")
                    .append(body)
                    .append("</div>\n");
            }
            html.append("</div>\n");
        }
        html.append("</div>\n");
        md.raw("\n" + html);
    }

    /**
     * Builds the documentation body for a single overload as an HTML string.
     * Each piece of metadata (description, {@code @since}, {@code @deprecated},
     * parameter list, return, {@code @see}) is serialised to HTML directly so
     * it can be embedded inside the overload's container element.
     */
    private String buildOverloadBody(MemberDoc member, ClassDoc clz) {
        StringBuilder html = new StringBuilder();

        // All content inside buildOverloadBody is placed inside raw HTML blocks.
        // Use the HTML rendering variants so that links become <a href> elements —
        // Markdown link syntax ([text](url)) is not processed inside raw HTML by VitePress.
        String desc = formatDescriptionAsHtml(member.docComment(), clz);
        if (!desc.isEmpty()) {
            html.append("<p>").append(desc).append("</p>\n");
        }

        if (hasDeprecatedTag(member.docComment())) {
            String deprecated = getTagTextAsHtml(member.docComment(), DocTagKind.DEPRECATED, clz);
            html.append("<p><strong>Deprecated:</strong> ")
                .append(deprecated)
                .append("</p>\n");
        }

        // Parameters — render @param tag bodies via DocBodyRenderer so {@link} tags become <a> links.
        List<ParamDoc> params = member.params();
        Map<String, String> paramDescriptions = new HashMap<>();
        if (member.docComment() != null) {
            for (DocTag tag : member.docComment().tags()) {
                if (tag.kind() == DocTagKind.PARAM && tag.name() != null) {
                    paramDescriptions.put(tag.name(),
                        DocBodyRenderer.toHtml(tag.body(), link -> resolveLinkAsHtml(link, clz)));
                }
            }
        }
        boolean hasParamDocs = params.stream()
            .anyMatch(p -> paramDescriptions.containsKey(p.name()) && !paramDescriptions.get(p.name()).isBlank());
        if (hasParamDocs) {
            html.append("<p><strong>Parameters:</strong></p>\n<ul>\n");
            for (ParamDoc param : params) {
                String desc2 = paramDescriptions.getOrDefault(param.name(), "");
                if (desc2.isBlank()) {
                    continue;
                }
                html.append("<li><code>").append(param.name()).append("</code>: ")
                    .append(desc2).append("</li>\n");
            }
            html.append("</ul>\n");
        }

        // Returns
        if (member.kind() == MemberKind.METHOD) {
            String retTag = getTagTextAsHtml(member.docComment(), DocTagKind.RETURN, clz);
            if (!retTag.isEmpty()) {
                html.append("<p><strong>Returns:</strong> ")
                    .append(retTag)
                    .append("</p>\n");
            } else {
                TypeRef returnType = member.returnType();
                if (returnType != null && returnType.kind() != TypeKind.VOID) {
                    html.append("<p><strong>Returns:</strong> <code>")
                        .append(renderTypeAsHtml(returnType, clz))
                        .append("</code></p>\n");
                }
            }
        }

        // TODO: (docs) Make these badges inline with the name(?)
        String since = getTagTextAsHtml(member.docComment(), DocTagKind.SINCE, clz);
        // TODO: (docs) Migrate the since tags containing descriptions to a note or similar.
        // TODO: (docs) Verify all [citaiton needed] instances.
        // Can be "1.2.3", "1.2.3 [citation needed]", or sometimes "1.2.3 (TextHelper since 2.3.4)"
        if (!since.isEmpty()) {
            html.append("<p><strong>Since:</strong> ").append(since).append("</p>\n");
        }

        // See
        if (member.docComment() != null) {
            List<String> sees = new ArrayList<>();
            for (DocTag tag : member.docComment().tags()) {
                if (tag.kind() == DocTagKind.SEE && !tag.body().isEmpty()) {
                    sees.add(resolveSeeTag(tag, clz));
                }
            }
            if (!sees.isEmpty()) {
                html.append("<p><strong>See:</strong></p>\n<ul>\n");
                for (String see : sees) {
                    html.append("<li>").append(see).append("</li>\n");
                }
                html.append("</ul>\n");
            }
        }

        return html.toString();
    }

    /**
     * Wraps the given text in an HTML element with optional attributes, for embedding inside raw
     * HTML blocks. Use when the content is already fully rendered to HTML and just needs a
     * wrapper element.
     * @param inner the already-rendered HTML content to wrap
     * @param elem the HTML element name, e.g. "div", "p", "li"
     * @param attribs optional raw attributes to include in the opening tag, e.g. "class=\"foo\" id=\"bar\""
     * @return the combined HTML string with the wrapper element and attributes around the inner content
     */
    private String wrapHtmlWithElemAndAttribs(String inner, String elem, String attribs) {
        StringBuilder sb = new StringBuilder();
        if (attribs != null && !attribs.isBlank()) {
            attribs = " " + attribs.trim();
        } else {
            attribs = "";
        }
        sb.append("<").append(elem).append(attribs).append(">");
        sb.append(inner);
        sb.append("</").append(elem).append(">");
        return sb.toString();
    }
    
    /**
     * Wraps the given text in an HTML element with optional attributes, for embedding inside raw
     * HTML blocks. Use when the content is already fully rendered to HTML and just needs a
     * wrapper element.
     * @see #wrapHtmlWithElemAndAttribs(String, String, String) for the variant with attributes.
     * @param inner the already-rendered HTML content to wrap
     * @param elem the HTML element name, e.g. "div", "p", "li"
     * @return the combined HTML string with the wrapper element around the inner content
     */
    private String wrapHtmlWithElem(String inner, String elem) {
        return wrapHtmlWithElemAndAttribs(inner, elem, null);
    }

    /**
     * Renders the signature as an HTML fragment with linked type names, for use
     * inside {@code <code>} in the overload-sig block.
     *
     * <p>Type names become {@code <a href="...">} links (internal or external Javadoc).
     * Generic angle brackets are HTML-entity-escaped. Punctuation and keywords are
     * plain HTML-safe text. {@code} on the containing {@code <code>} element
     * tells Vue's template compiler to skip the element, so the {@code <a>} tags
     * are preserved as-is in the rendered output.
     *
     * <p>When a {@code @DocletReplaceParams} or {@code @DocletReplaceReturn}
     * annotation override is present the raw override string is HTML-escaped
     * (no link resolution is attempted for opaque overrides).
     */
    private String renderSignatureAsHtml(MemberDoc member, ClassDoc context) {
        if (member.kind() == MemberKind.FIELD) {
            return member.name() + ": " + renderTypeAsHtml(member.returnType(), context);
        }

        StringBuilder sb = new StringBuilder();
        if (member.kind() == MemberKind.CONSTRUCTOR) {
            sb.append(Syntax.keyword("new"));
            sb.append(Syntax.raw(" "));
        }
        sb.append(Syntax.method(member.name()));
        sb.append(Syntax.punctuation("("));
        List<ParamDoc> params = member.params();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(Syntax.punctuation(", "));
            }

            ParamDoc param = params.get(i);
            sb.append(renderTypeAsHtml(param.type(), context))
                .append(Syntax.raw(" "))
                .append(Syntax.parameter(param.name()));
        }
        sb.append(Syntax.punctuation(")"));
        if (member.kind() != MemberKind.CONSTRUCTOR) {
            sb.append(Syntax.punctuation(":"))
                .append(Syntax.raw(" "))
                .append(renderTypeAsHtml(member.returnType(), context));
        }
        return sb.toString();
    }

    /**
     * Returns the rendered inline HTML for a single tag of the given kind,
     * or an empty string when no matching tag is present.
     * Use for content placed inside raw HTML blocks (overload-body, &lt;p&gt;, &lt;li&gt;).
     */
    private String getTagTextAsHtml(DocComment comment, DocTagKind kind, ClassDoc context) {
        if (comment == null) {
            return "";
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == kind) {
                return DocBodyRenderer.toHtml(tag.body(), link -> resolveLinkAsHtml(link, context));
            }
        }
        return "";
    }

    private String formatDescription(DocComment comment, ClassDoc context) {
        if (comment == null) {
            return "";
        }
        List<DocBodyNode> nodes = comment.body().isEmpty() ? comment.summary() : comment.body();
        return DocBodyRenderer.toMarkdown(nodes, link -> resolveLink(link, context));
    }

    /**
     * Renders the description as inline HTML for embedding inside raw HTML blocks.
     */
    private String formatDescriptionAsHtml(DocComment comment, ClassDoc context) {
        if (comment == null) {
            return "";
        }
        List<DocBodyNode> nodes = comment.body().isEmpty() ? comment.summary() : comment.body();
        return DocBodyRenderer.toHtml(nodes, link -> resolveLinkAsHtml(link, context));
    }

    private boolean hasDeprecatedTag(DocComment comment) {
        if (comment == null) {
            return false;
        }
        return comment.tags().stream().anyMatch(tag -> tag.kind() == DocTagKind.DEPRECATED);
    }

    /**
     * Resolves a {@code @see} tag to an HTML {@code <a>} element wrapping a
     * {@code <code>} label. The tag body is expected to contain a single
     * {@link DocBodyNode.Link} node (as produced by {@link com.jsmacrosce.doclet.core.BasicDocCommentParser});
     * if not, a plain-text fallback is used.
     */
    private String resolveSeeTag(DocTag tag, ClassDoc context) {
        for (DocBodyNode node : tag.body()) {
            if (node instanceof DocBodyNode.Link link) {
                String label = "<code>" + linkLabel(link.signature()) + "</code>";
                LinkSignature parsed = parseSignature(link.signature());
                if (parsed == null) {
                    return label;
                }
                ClassDoc targetClass = resolveClass(parsed.className(), context);
                if (targetClass == null) {
                    return label;
                }
                String anchor = resolveMemberAnchor(targetClass, parsed);
                String url = buildLinkUrl(targetClass, anchor, context);
                return "<a href=\"" + url + "\">" + label + "</a>";
            }
            if (node instanceof DocBodyNode.Html html) {
                return html.raw();
            }
        }
        return DocBodyRenderer.toRawText(tag.body());
    }

    private void indexClasses(DocletModel model) {
        classByQualifiedName.clear();
        classByName.clear();
        classByAlias.clear();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                classByQualifiedName.put(clz.qualifiedName(), clz);
                addClassNameIndex(classByName, clz.name(), clz);
                String simpleName = simpleName(clz.name());
                if (!simpleName.equals(clz.name())) {
                    addClassNameIndex(classByName, simpleName, clz);
                }
                if (hasAlias(clz)) {
                    addClassNameIndex(classByAlias, clz.alias(), clz);
                }
            }
        }
    }

    private void addClassNameIndex(Map<String, List<ClassDoc>> index, String key, ClassDoc clz) {
        index.computeIfAbsent(key, name -> new ArrayList<>()).add(clz);
    }

    // -------------------------------------------------------------------------
    // HTML type rendering — produces linked HTML for a TypeRef tree.
    // Each named type component becomes an <a> to its docs page (internal or
    // external), and angle brackets are HTML-escaped so they render literally
    // inside a <code> block in Markdown/VitePress.
    // -------------------------------------------------------------------------

    /**
     * Renders a {@link TypeRef} as plain text with HTML-escaped angle brackets,
     * suitable for embedding in a {@code <code>} element without any hyperlinks.
     * Used for member signatures in the overload-sig block.
     */
    private String renderTypeAsText(TypeRef type) {
        if (type == null) {
            return "";
        }
        return switch (type.kind()) {
            case PRIMITIVE, VOID -> escapeHtml(type.name());
            case TYPEVAR -> {
                String base = escapeHtml(type.name());
                if (type.bounds() != null) {
                    base = base + " extends " + renderTypeAsText(type.bounds());
                }
                yield base;
            }
            case ARRAY -> renderTypeAsText(type.typeArgs().get(0)) + "[]";
            case WILDCARD -> {
                if (type.bounds() != null) {
                    yield "? extends " + renderTypeAsText(type.bounds());
                }
                yield "?";
            }
            case DECLARED -> {
                String name = escapeHtml(type.name());
                if (type.typeArgs().isEmpty()) {
                    yield name;
                }
                StringBuilder sb = new StringBuilder(name);
                sb.append("&lt;");
                for (int i = 0; i < type.typeArgs().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(renderTypeAsText(type.typeArgs().get(i)));
                }
                sb.append("&gt;");
                yield sb.toString();
            }
            case INTERSECTION -> renderTypeArgListAsText(type.typeArgs(), " &amp; ");
            case UNION -> renderTypeArgListAsText(type.typeArgs(), " | ");
            default -> escapeHtml(type.name());
        };
    }

    private String renderTypeArgListAsText(List<TypeRef> args, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(renderTypeAsText(args.get(i)));
        }
        return sb.toString();
    }

    /**
     * Renders a {@link TypeRef} as an HTML fragment where each declared type
     * name is wrapped in an {@code <a>} tag pointing to its documentation
     * (internal project page or external Javadoc).  Generic angle brackets are
     * emitted as {@code &lt;} / {@code &gt;} so they render correctly inside a
     * {@code <code>} element in the Markdown output.
     */
    private String renderTypeAsHtml(TypeRef type, @Nullable ClassDoc context) {
        if (type == null) {
            return "";
        }
        return switch (type.kind()) {
            case PRIMITIVE, VOID -> Syntax.primative(escapeHtml(type.name()));
            case TYPEVAR -> {
                // Type variable: show its name; if it has a bound render "T extends Bound"
                StringBuilder sb = new StringBuilder();
                sb.append(Syntax.typeVar(escapeHtml(type.name())));
                if (type.bounds() != null) {
                    sb.append(Syntax.raw(" "))
                        .append(Syntax.keyword("extends"))
                        .append(Syntax.raw(" "))
                        .append(renderTypeAsHtml(type.bounds(), context));
                }
                yield sb.toString();
            }
            case ARRAY -> renderTypeAsHtml(type.typeArgs().get(0), context) + Syntax.punctuation("[]");
            case WILDCARD -> {
                StringBuilder sb = new StringBuilder();
                sb.append(Syntax.wildcard("?"));
                
                if (type.bounds() != null) {
                    sb.append(Syntax.raw(" "))
                        .append(Syntax.keyword("extends"))
                        .append(Syntax.raw(" "))
                        .append(renderTypeAsHtml(type.bounds(), context));
                }

                yield sb.toString();
            }
            case DECLARED -> {
                StringBuilder sb = new StringBuilder();
                String linked = linkTypeName(type, context);
                sb.append(linked);
                if (type.typeArgs().isEmpty()) {
                    yield sb.toString();
                }

                sb.append(Syntax.punctuation("&lt;"));
                for (int i = 0; i < type.typeArgs().size(); i++) {
                    if (i > 0) {
                        sb.append(Syntax.punctuation(", "));
                    }
                    sb.append(renderTypeAsHtml(type.typeArgs().get(i), context));
                }
                sb.append(Syntax.punctuation("&gt;"));
                yield sb.toString();
            }
            case INTERSECTION -> renderTypeArgListAsHtml(type.typeArgs(), Syntax.raw(" ") + Syntax.punctuation("&amp;") + Syntax.raw(" "), context);
            case UNION -> renderTypeArgListAsHtml(type.typeArgs(), Syntax.raw(" ") + Syntax.punctuation("&amp;") + Syntax.raw(" "), context);
            default -> Syntax.raw(escapeHtml(type.name()));
        };
    }

    private String renderTypeArgListAsHtml(List<TypeRef> args, String separator, @Nullable ClassDoc context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(renderTypeAsHtml(args.get(i), context));
        }
        return sb.toString();
    }

    /**
     * Returns the display name of a declared type wrapped in an {@code <a>} tag
     * when a link target can be resolved, or just the plain name if not.
     * Angle brackets in the display name are escaped to &lt;/&gt; so they render
     * correctly when embedded inside HTML {@code <code>} elements.
     */
    private String linkTypeName(TypeRef type, @Nullable ClassDoc context) {
        String displayName = escapeHtml(type.name());
        ResolvedType resolved = resolveTypeUrl(type, context);
        if (resolved == null || resolved.url() == null) {
            return Syntax.type(displayName);
        }

        StringBuilder attribs = new StringBuilder();
        attribs.append("href=\"").append(resolved.url()).append("\"");
        if (resolved.isExternal()) {
            attribs.append(" target=\"_blank\"");
            attribs.append(" rel=\"noopener noreferrer\"");
        }

        return Syntax.linkedType(wrapHtmlWithElemAndAttribs(
            displayName,
            "a",
            attribs.toString()
        ));
    }

    /** Escapes < and > to HTML entities for safe embedding in HTML code elements. */
    private String escapeHtml(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Resolves the URL for a declared type.
     * <ol>
     *   <li>Checks the internal project index (classByQualifiedName).</li>
     *   <li>Falls back to the external Javadoc package index.</li>
     * </ol>
     * Returns {@code null} when no link can be determined.
     */
    private ResolvedType resolveTypeUrl(TypeRef type, @Nullable ClassDoc context) {
        // 1. Internal project class?
        ClassDoc internal = classByQualifiedName.get(type.qualifiedName());
        if (internal != null) {
            return new ResolvedType(type.qualifiedName(), buildLinkUrl(internal, null, context), false);
        }

        // 2. External Javadoc?
        String externalUrl = resolveExternalTypeUrl(type.qualifiedName());
        if (externalUrl != null) {
            return new ResolvedType(type.qualifiedName(), externalUrl, true);
        }

        return null;
    }

    private record ResolvedType(String qualifiedName, String url, boolean isExternal) {}

    /**
     * Looks up a type's package in {@link #externalPackages} and builds the
     * Javadoc URL for that type.  Returns {@code null} when the package is not
     * in the external index.
     *
     * <p>The {@code -link} option stores entries in the form
     * {@code "baseUrl/index.html?java/util/"} for package {@code java.util}.
     * We append the simple class name ({@code List.html}) to produce the final
     * URL: {@code "baseUrl/index.html?java/util/List.html"}.
     */
    @Nullable
    private String resolveExternalTypeUrl(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        String baseUrl = externalPackages.get(packageName);
        if (baseUrl == null) {
            return null;
        }
        // baseUrl is stored as e.g. "https://…/api/index.html?java/util/"
        // Appending "List.html" gives "https://…/api/index.html?java/util/List.html".
        // Strip any trailing slash first so we always end up with exactly one separator.
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/" + simpleName + ".html";
    }

    // -------------------------------------------------------------------------
    // Link resolution — called from the DocBodyRenderer linkResolver lambda.
    // -------------------------------------------------------------------------

    /**
     * Resolves a {@link DocBodyNode.Link} to a Markdown link string.
     * Falls back to a plain label when the target cannot be found.
     */
    private String resolveLink(DocBodyNode.Link link, ClassDoc context) {
        String sig = link.signature();

        // Check for simple Java type aliases first (String, Boolean, boxed numbers).
        String mapped = DocBodyRenderer.mapSimpleLinkSignature(sig);
        if (mapped != null) {
            return mapped;
        }

        LinkSignature parsed = parseSignature(sig);
        if (parsed == null) {
            String label = link.label() != null ? link.label() : linkLabel(sig);
            // If the bare sig looks like a signature, wrap it in a code span.
            if (DocBodyRenderer.looksLikeSignature(sig)) {
                return MarkdownBuilder.codeSpan(DocBodyRenderer.convertSignature(sig));
            }
            return label;
        }
        ClassDoc targetClass = resolveClass(parsed.className(), context);
        if (targetClass == null) {
            String label = link.label() != null ? link.label() : linkLabel(sig);
            return label;
        }
        String anchor = resolveMemberAnchor(targetClass, parsed);
        String url = buildLinkUrl(targetClass, anchor, context);
        String label = link.label() != null ? link.label() : linkLabel(sig);
        return "[" + label + "](" + url + ")";
    }

    /**
     * Resolves a {@link DocBodyNode.Link} to an HTML {@code <a>} element string.
     * Used when rendering inside raw HTML blocks (overload-body, param lists, etc.)
     * where Markdown link syntax is not processed by VitePress.
     */
    private String resolveLinkAsHtml(DocBodyNode.Link link, ClassDoc context) {
        String sig = link.signature();

        String mapped = DocBodyRenderer.mapSimpleLinkSignature(sig);
        if (mapped != null) {
            return "<code>" + escapeHtml(mapped) + "</code>";
        }

        LinkSignature parsed = parseSignature(sig);
        String label = link.label() != null ? link.label() : linkLabel(sig);
        if (parsed == null) {
            if (DocBodyRenderer.looksLikeSignature(sig)) {
                return "<code>" + escapeHtml(DocBodyRenderer.convertSignature(sig)) + "</code>";
            }
            return escapeHtml(label);
        }
        ClassDoc targetClass = resolveClass(parsed.className(), context);
        if (targetClass == null) {
            return escapeHtml(label);
        }
        String anchor = resolveMemberAnchor(targetClass, parsed);
        String url = buildLinkUrl(targetClass, anchor, context);
        return "<a href=\"" + url + "\">" + escapeHtml(label) + "</a>";
    }

    private String buildLinkUrl(ClassDoc targetClass, String anchor, ClassDoc context) {
        // Anchors on the page are qualified: "qualifiedName_anchorId" (e.g. "com.example.Foo_bar-String-").
        String qualifiedAnchor = anchor != null ? targetClass.qualifiedName() + "_" + anchor : null;
        if (context != null && targetClass.qualifiedName().equals(context.qualifiedName())) {
            return qualifiedAnchor == null ? "#" : "#" + qualifiedAnchor;
        }

        String url;
        if (context == null) {
            url = "/" + version + "/" + classPath(targetClass);
        } else {
            Path from = Path.of(classPath(context));
            Path to = Path.of(classPath(targetClass));
            Path fromParent = from.getParent();
            url = (fromParent == null ? to : fromParent.relativize(to)).toString().replace(File.separatorChar, '/');
            if (url.isBlank()) {
                url = "#";
            }
        }
        if (qualifiedAnchor != null) {
            url = url + "#" + qualifiedAnchor;
        }
        return url;
    }

    @Nullable
    private ClassDoc resolveClass(String name, ClassDoc context) {
        if (name == null || name.isBlank()) {
            return context;
        }
        String cleaned = name.startsWith("Packages.") ? name.substring("Packages.".length()) : name;
        int genericIndex = cleaned.indexOf('<');
        if (genericIndex != -1) {
            cleaned = cleaned.substring(0, genericIndex).trim();
        }
        ClassDoc direct = classByQualifiedName.get(cleaned);
        if (direct != null) {
            return direct;
        }
        List<ClassDoc> matches = new ArrayList<>();
        List<ClassDoc> nameMatches = classByName.get(cleaned);
        if (nameMatches != null) {
            matches.addAll(nameMatches);
        }
        List<ClassDoc> aliasMatches = classByAlias.get(cleaned);
        if (aliasMatches != null) {
            matches.addAll(aliasMatches);
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1 || context == null) {
            return matches.getFirst();
        }
        for (ClassDoc match : matches) {
            if (match.packageName().equals(context.packageName())) {
                return match;
            }
        }
        return matches.getFirst();
    }

    @Nullable
    private LinkSignature parseSignature(String signature) {
        if (signature == null) {
            return null;
        }
        String trimmed = signature.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String classPart = null;
        String memberPart = null;
        int hashIndex = trimmed.indexOf('#');
        if (hashIndex == 0) {
            memberPart = trimmed.substring(1);
        } else if (hashIndex > 0) {
            classPart = trimmed.substring(0, hashIndex);
            memberPart = trimmed.substring(hashIndex + 1);
        } else {
            classPart = trimmed;
        }
        String memberName = null;
        List<String> params = null;
        if (memberPart != null && !memberPart.isBlank()) {
            int parenIndex = memberPart.indexOf('(');
            if (parenIndex != -1 && memberPart.endsWith(")")) {
                memberName = memberPart.substring(0, parenIndex);
                String paramBody = memberPart.substring(parenIndex + 1, memberPart.length() - 1);
                params = splitParams(paramBody);
            } else {
                memberName = memberPart;
            }
        }
        return new LinkSignature(classPart, memberName, params);
    }

    private List<String> splitParams(String paramBody) {
        List<String> params = new ArrayList<>();
        if (paramBody == null || paramBody.isBlank()) {
            return params;
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < paramBody.length(); i++) {
            char ch = paramBody.charAt(i);
            if (ch == '<') {
                depth++;
            } else if (ch == '>' && depth > 0) {
                depth--;
            }
            if (ch == ',' && depth == 0) {
                params.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            params.add(current.toString().trim());
        }
        return params;
    }

    @Nullable
    private String resolveMemberAnchor(ClassDoc clz, LinkSignature signature) {
        if (signature.memberName() == null) {
            return null;
        }
        List<MemberDoc> members = new ArrayList<>();
        for (MemberDoc member : clz.members()) {
            if (member.name().equals(signature.memberName())) {
                members.add(member);
            }
        }
        if (members.isEmpty() && signature.memberName().equals(clz.name())) {
            for (MemberDoc member : clz.members()) {
                if (member.kind() == MemberKind.CONSTRUCTOR) {
                    members.add(member);
                }
            }
        }
        if (members.isEmpty()) {
            return null;
        }
        List<String> params = signature.paramTypes();
        if (params == null) {
            return members.size() == 1 ? members.getFirst().anchorId() : null;
        }
        for (MemberDoc member : members) {
            if (paramsMatch(member, params)) {
                return member.anchorId();
            }
        }
        return null;
    }

    private boolean paramsMatch(MemberDoc member, List<String> params) {
        if (member.params().size() != params.size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            String expected = normalizeParamType(params.get(i));
            ParamDoc param = member.params().get(i);
            String actual = normalizeParamType(memberParamTypeName(param));
            if (!expected.equals(actual)) {
                return false;
            }
        }
        return true;
    }

    private String memberParamTypeName(ParamDoc param) {
        return memberParamTypeName(param.type(), param.varArgs());
    }

    private String memberParamTypeName(TypeRef type, boolean varArgs) {
        String base = type.kind() == TypeKind.ARRAY
            ? memberParamTypeName(type.typeArgs().getFirst(), false) + "[]"
            : type.name();
        if (varArgs && type.kind() != TypeKind.ARRAY) {
            base = base + "[]";
        }
        return base;
    }

    private String normalizeParamType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("?")) {
            return "?";
        }
        int arrayDepth = 0;
        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(0, normalized.length() - 2).trim();
            arrayDepth++;
        }
        if (normalized.endsWith("...")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
            arrayDepth++;
        }
        int genericIndex = normalized.indexOf('<');
        if (genericIndex != -1) {
            normalized = normalized.substring(0, genericIndex).trim();
        }
        normalized = stripPackageName(normalized);
        return normalized + "[]".repeat(Math.max(0, arrayDepth));
    }

    private String stripPackageName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        if (!name.contains(".")) {
            return name;
        }
        String[] parts = name.split("\\.");
        int firstTypeIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                firstTypeIndex = i;
                break;
            }
        }
        if (firstTypeIndex == -1) {
            return parts[parts.length - 1];
        }
        StringBuilder builder = new StringBuilder();
        for (int i = firstTypeIndex; i < parts.length; i++) {
            if (!builder.isEmpty()) {
                builder.append(".");
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    private String linkLabel(String signature) {
        if (signature == null) {
            return "";
        }
        String cleaned = signature.trim();
        if (cleaned.startsWith("Packages.")) {
            cleaned = cleaned.substring("Packages.".length());
        }
        int hashIndex = cleaned.indexOf('#');
        if (hashIndex == 0) {
            return cleaned.substring(1);
        }
        if (hashIndex > 0) {
            String classPart = cleaned.substring(0, hashIndex);
            String memberPart = cleaned.substring(hashIndex + 1);
            String classLabel = stripPackageName(classPart);
            if (classLabel.isBlank()) {
                return memberPart;
            }
            return classLabel + "." + memberPart;
        }
        return stripPackageName(cleaned);
    }

    private String simpleName(String name) {
        int idx = name.lastIndexOf('.');
        return idx == -1 ? name : name.substring(idx + 1);
    }

    private record LinkSignature(String className, String memberName, List<String> paramTypes) {}

    /**
     * 
     */
    private class Syntax {
        /**
         * Wraps the given text in an HTML element with optional attributes, for embedding inside raw
         * HTML blocks. Use when the content is already fully rendered to HTML and just needs a
         * wrapper element.
         * @param inner the already-rendered HTML content to wrap
         * @param elem the HTML element name, e.g. "div", "p", "li"
         * @param attribs optional raw attributes to include in the opening tag, e.g. "class=\"foo\" id=\"bar\""
         * @return the combined HTML string with the wrapper element and attributes around the inner content
         */
        private static String wrapHtmlWithElemAndAttribs(String inner, String elem, String attribs) {
            StringBuilder sb = new StringBuilder();
            if (attribs != null && !attribs.isBlank()) {
                attribs = " " + attribs.trim();
            } else {
                attribs = "";
            }
            sb.append("<").append(elem).append(attribs).append(">");
            sb.append(inner);
            sb.append("</").append(elem).append(">");
            return sb.toString();
        }
        
        /**
         * Wraps the given text in an HTML element with optional attributes, for embedding inside raw
         * HTML blocks. Use when the content is already fully rendered to HTML and just needs a
         * wrapper element.
         * @see #wrapHtmlWithElemAndAttribs(String, String, String) for the variant with attributes.
         * @param inner the already-rendered HTML content to wrap
         * @param elem the HTML element name, e.g. "div", "p", "li"
         * @return the combined HTML string with the wrapper element around the inner content
         */
        private static String wrapHtmlWithElem(String inner, String elem) {
            return wrapHtmlWithElemAndAttribs(inner, elem, null);
        }
        
        /**
         * Wraps the given content in a <span> with class "syntax-token-{type}", for syntax highlighting.
         * @param type the token type, e.g. "keyword", "class-name", "punctuation"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String tok(String type, String content) {
            return wrapHtmlWithElemAndAttribs(content, "span", "class=\"syntax-token-" + type + "\"");
        }

        /**
         * Returns the given content wrapped in a "keyword" token span.
         * Ex. "new", "extends", "implements"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String keyword(String content) {
            return tok("keyword", content);
        }

        /**
         * Returns the given content wrapped in a "method" token span.
         * Ex. method names, constructor names, field names
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String method(String content) {
            return tok("method", content);
        }

        /**
         * Returns the given content wrapped in a "class" token span.
         * Ex. class names, interface names, enum names, annotation names
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String clazz(String content) {
            return tok("class", content);
        }

        /**
         * Returns the given content wrapped in a "type" token span.
         * Ex. type parameters, type variable names
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String type(String content) {
            return tok("type", content);
        }

        /**
         * Returns the given content wrapped in a "linked-type" token span.
         * Ex. type parameters, type variable
         * @param content the already-rendered <a> wrapped HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String linkedType(String content) {
            return tok("linked-type", content);
        }

        /**
         * Returns the given content wrapped in a "parameter" token span.
         * Ex. parameter names in method signatures
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String parameter(String content) {
            return tok("parameter", content);
        }

        /**
         * Returns the given content wrapped in a "punctuation" token span.
         * Ex. "(", ")", "{", "}", "[", "]", ";", ",", ".", "::", ":", "<", ">"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String punctuation(String content) {
            return tok("punctuation", content);
        }

        /**
         * Returns the given content wrapped in a "wildcard" token span.
         * Ex. "?"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String wildcard(String content) {
            return tok("wildcard", content);
        }

        /**
         * Returns the given content wrapped in a "primative" token span.
         * Ex. "?"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String primative(String content) {
            return tok("primative", content);
        }

        /**
         * Returns the given content wrapped in a "typevar" token span.
         * Ex. type variable names like "T" and "E"
         * @param content the already-rendered HTML content to wrap
         * @return the combined HTML string with the <span> wrapper and token class around the inner content
         */
        public static String typeVar(String content) {
            return tok("typevar", content);
        }

        /**
         * Returns the given raw HTML string wrapped in a <span> element, for embedding inside
         * Shiki-highlighted code blocks where the content is already fully rendered to HTML
         * and just needs a wrapper element.
         * @param html the raw HTML string to wrap
         * @return the combined HTML string with the <span> wrapper around the inner content
         */
        public static String raw(String html) {
            return html;
        }
    }
}
