package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.FileHandler;
import com.jsmacrosce.MarkdownBuilder;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
                String category = clz.category();
                if (category == null || category.isBlank()) {
                    category = DEFAULT_CATEGORY;
                }
                categories.computeIfAbsent(category, key -> new ArrayList<>()).add(clz);
            }
        }
        for (List<ClassDoc> list : categories.values()) {
            list.sort(Comparator.comparing(this::sidebarSortKey, String.CASE_INSENSITIVE_ORDER));
        }
        return categories;
    }

    private String sidebarSortKey(ClassDoc clz) {
        return hasAlias(clz) ? clz.alias() : clz.name();
    }

    private List<SidebarCategory> mapToSidebarCategories(Map<String, List<ClassDoc>> categories, String version) {
        List<SidebarCategory> sections = new ArrayList<>();
        for (Map.Entry<String, List<ClassDoc>> entry : categories.entrySet()) {
            List<SidebarItem> items = new ArrayList<>();
            for (ClassDoc clz : entry.getValue()) {
                String link = "/" + version + "/" + classPath(clz);
                items.add(new SidebarItem(displayLabel(clz), link));
            }
            sections.add(new SidebarCategory(entry.getKey(), items));
        }
        return sections;
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

    private record SidebarItem(String text, String link) {}
    private record SidebarCategory(String name, List<SidebarItem> items) {}
    private record SidebarData(String version, List<SidebarCategory> classes, List<SidebarCategory> events, List<SidebarCategory> libraries) {}

    private String renderClass(ClassDoc clz) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.frontmatter(Map.of("outline", "deep"));
        md.heading(1, displayTitle(clz));
        md.paragraph(clz.qualifiedName());

        String desc = formatDescription(clz.docComment(), clz);
        String descText = desc.isEmpty() ? "TODO: No description supplied\n" : desc;
        if (clz.group() == ClassGroup.Library) {
            String accessName = clz.alias() == null || clz.alias().isEmpty() ? clz.name() : clz.alias();
            descText += "\nAccessible in scripts via the global " + MarkdownBuilder.codeSpan(accessName) + " variable.";
        }
        md.paragraph(descText);

        renderMemberSection(md, clz, MemberKind.CONSTRUCTOR, "Constructors");
        renderMemberSection(md, clz, MemberKind.FIELD, "Fields");
        renderMemberSection(md, clz, MemberKind.METHOD, "Methods");
        return md.toString();
    }

    private String displayTitle(ClassDoc clz) {
        if (clz.group() == ClassGroup.Event && hasAlias(clz)) {
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
            // v-pre suppresses Vue template compilation so <a href> links inside <code> work.
            html.append("<code v-pre>").append(renderSignatureAsHtml(member)).append("</code>");
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
                        .append(renderTypeAsHtml(returnType))
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
     * Renders the signature as a plain-text HTML-safe string for embedding
     * inside a {@code <code>} element.  Type names are rendered without {@code <a>}
     * links (to avoid Vue template-compiler issues with mixed-content {@code <code>}
     * blocks); generic angle brackets are HTML-entity-escaped.
     *
     * <p>When a {@code @DocletReplaceParams} or {@code @DocletReplaceReturn}
     * annotation override is present the raw override string is HTML-escaped for
     * safe embedding.
     */
    private String renderSignature(MemberDoc member) {
        if (member.kind() == MemberKind.FIELD) {
            return member.name() + ": " + renderTypeAsText(member.returnType());
        }

        StringBuilder sb = new StringBuilder();
        if (member.kind() == MemberKind.CONSTRUCTOR) {
            sb.append("new ");
        }
        sb.append(member.name()).append("(");
        if (member.replaceParams() != null && !member.replaceParams().isBlank()) {
            // Opaque override — escape angle brackets for safe HTML embedding.
            sb.append(escapeHtml(member.replaceParams()));
        } else {
            List<ParamDoc> params = member.params();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                ParamDoc param = params.get(i);
                sb.append(renderTypeAsText(param.type())).append(" ").append(param.name());
            }
        }
        sb.append(")");
        if (member.kind() != MemberKind.CONSTRUCTOR) {
            sb.append(": ");
            if (member.replaceReturn() != null && !member.replaceReturn().isBlank()) {
                sb.append(escapeHtml(member.replaceReturn()));
            } else {
                sb.append(renderTypeAsText(member.returnType()));
            }
        }
        return sb.toString();
    }

    /**
     * Renders the signature as an HTML fragment with linked type names, for use
     * inside {@code <code v-pre>} in the overload-sig block.
     *
     * <p>Type names become {@code <a href="...">} links (internal or external Javadoc).
     * Generic angle brackets are HTML-entity-escaped. Punctuation and keywords are
     * plain HTML-safe text. {@code v-pre} on the containing {@code <code>} element
     * tells Vue's template compiler to skip the element, so the {@code <a>} tags
     * are preserved as-is in the rendered output.
     *
     * <p>When a {@code @DocletReplaceParams} or {@code @DocletReplaceReturn}
     * annotation override is present the raw override string is HTML-escaped
     * (no link resolution is attempted for opaque overrides).
     */
    private String renderSignatureAsHtml(MemberDoc member) {
        if (member.kind() == MemberKind.FIELD) {
            return member.name() + ": " + renderTypeAsHtml(member.returnType());
        }

        StringBuilder sb = new StringBuilder();
        if (member.kind() == MemberKind.CONSTRUCTOR) {
            sb.append("new ");
        }
        sb.append(member.name()).append("(");
        List<ParamDoc> params = member.params();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            ParamDoc param = params.get(i);
            sb.append(renderTypeAsHtml(param.type())).append(" ").append(param.name());
        }
        sb.append(")");
        if (member.kind() != MemberKind.CONSTRUCTOR) {
            sb.append(": ").append(renderTypeAsHtml(member.returnType()));
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
    private String renderTypeAsHtml(TypeRef type) {
        if (type == null) {
            return "";
        }
        return switch (type.kind()) {
            case PRIMITIVE, VOID -> escapeHtml(type.name());
            case TYPEVAR -> {
                // Type variable: show its name; if it has a bound render "T extends Bound"
                String base = escapeHtml(type.name());
                if (type.bounds() != null) {
                    base = base + " extends " + renderTypeAsHtml(type.bounds());
                }
                yield base;
            }
            case ARRAY -> renderTypeAsHtml(type.typeArgs().get(0)) + "[]";
            case WILDCARD -> {
                if (type.bounds() != null) {
                    yield "? extends " + renderTypeAsHtml(type.bounds());
                }
                yield "?";
            }
            case DECLARED -> {
                String linked = linkTypeName(type);
                if (type.typeArgs().isEmpty()) {
                    yield linked;
                }
                StringBuilder sb = new StringBuilder(linked);
                sb.append("&lt;");
                for (int i = 0; i < type.typeArgs().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(renderTypeAsHtml(type.typeArgs().get(i)));
                }
                sb.append("&gt;");
                yield sb.toString();
            }
            case INTERSECTION -> renderTypeArgListAsHtml(type.typeArgs(), " &amp; ");
            case UNION -> renderTypeArgListAsHtml(type.typeArgs(), " | ");
            default -> escapeHtml(type.name());
        };
    }

    private String renderTypeArgListAsHtml(List<TypeRef> args, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(renderTypeAsHtml(args.get(i)));
        }
        return sb.toString();
    }

    /**
     * Returns the display name of a declared type wrapped in an {@code <a>} tag
     * when a link target can be resolved, or just the plain name if not.
     * Angle brackets in the display name are escaped to &lt;/&gt; so they render
     * correctly when embedded inside HTML {@code <code>} elements.
     */
    private String linkTypeName(TypeRef type) {
        String displayName = escapeHtml(type.name());
        String url = resolveTypeUrl(type);
        if (url == null) {
            return displayName;
        }
        return "<a href=\"" + url + "\">" + displayName + "</a>";
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
    private String resolveTypeUrl(TypeRef type) {
        // 1. Internal project class?
        ClassDoc internal = classByQualifiedName.get(type.qualifiedName());
        if (internal != null) {
            return "/" + version + "/" + classPath(internal);
        }

        // 2. External Javadoc?
        String externalUrl = resolveExternalTypeUrl(type.qualifiedName());
        if (externalUrl != null) {
            return externalUrl;
        }

        return null;
    }

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
        String url;
        // Anchors on the page are qualified: "qualifiedName_anchorId" (e.g. "com.example.Foo_bar-String-").
        String qualifiedAnchor = anchor != null ? targetClass.qualifiedName() + "_" + anchor : null;
        if (context != null && targetClass.qualifiedName().equals(context.qualifiedName()) && qualifiedAnchor != null) {
            url = "#" + qualifiedAnchor;
        } else {
            url = "/" + version + "/" + classPath(targetClass);
            if (qualifiedAnchor != null) {
                url = url + "#" + qualifiedAnchor;
            }
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
}
