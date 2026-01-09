package xyz.wagyourtail.doclet.core.render;

import xyz.wagyourtail.FileHandler;
import xyz.wagyourtail.doclet.core.TargetLanguage;
import xyz.wagyourtail.doclet.core.TypeResolver;
import xyz.wagyourtail.doclet.core.model.ClassDoc;
import xyz.wagyourtail.doclet.core.model.DocComment;
import xyz.wagyourtail.doclet.core.model.DocTag;
import xyz.wagyourtail.doclet.core.model.DocTagKind;
import xyz.wagyourtail.doclet.core.model.DocletModel;
import xyz.wagyourtail.doclet.core.model.MemberDoc;
import xyz.wagyourtail.doclet.core.model.MemberKind;
import xyz.wagyourtail.doclet.core.model.PackageDoc;
import xyz.wagyourtail.doclet.core.model.ParamDoc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MarkdownWriter {
    private static final Pattern HTML_LINK =
        Pattern.compile("<a (?:\\n|.)*?href=\"([^\"]*)\"(?:\\n|.)*?>(.*?)</a>");
    private static final Pattern LINK_TAG = Pattern.compile("\\{@link\\s+([^}]+)}");
    private static final Map<String, String> JAVA_NUMBER_TYPES = Map.of(
        "java.lang.Integer", "int",
        "java.lang.Float", "float",
        "java.lang.Long", "long",
        "java.lang.Short", "short",
        "java.lang.Character", "char",
        "java.lang.Byte", "byte",
        "java.lang.Double", "double",
        "java.lang.Number", "number"
    );
    private static final String DEFAULT_CATEGORY = "Uncategorized";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final TypeResolver typeResolver;

    public MarkdownWriter(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    public void write(DocletModel model, File outDir, String version, String mcVersion) throws IOException {
        Map<String, List<ClassDoc>> classCategories = groupByCategory(model, "Class");
        Map<String, List<ClassDoc>> eventCategories = groupByCategory(model, "Event");
        Map<String, List<ClassDoc>> libraryCategories = groupByCategory(model, "Library");
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
            case "Event" -> "events";
            case "Class" -> "classes";
            case "Library" -> "libraries";
            default -> "";
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
        Map<String, List<ClassDoc>> grouped = new java.util.HashMap<>();
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
            .write(renderGroupPage("Libraries", grouped.getOrDefault("Library", List.of()), true, libraryCategories));
        new FileHandler(new File(outDir, "events.md"))
            .write(renderGroupPage("Events", grouped.getOrDefault("Event", List.of()), true, eventCategories));
        new FileHandler(new File(outDir, "classes.md"))
            .write(renderGroupPage("Classes", grouped.getOrDefault("Class", List.of()), false, classCategories));
    }

    private String renderOverview(Map<String, List<ClassDoc>> grouped, String version, String mcVersion) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\noutline: false\n---\n\n");
        builder.append("# JsMacros API Reference\n\n");
        builder.append("Version: `").append(version).append("`  \n");
        builder.append("Minecraft: `").append(mcVersion).append("`\n\n");
        builder.append("- [Libraries](./libraries.md) (").append(grouped.getOrDefault("Library", List.of()).size()).append(")\n");
        builder.append("- [Events](./events.md) (").append(grouped.getOrDefault("Event", List.of()).size()).append(")\n");
        builder.append("- [Classes](./classes.md) (").append(grouped.getOrDefault("Class", List.of()).size()).append(")\n\n");
        builder.append("Use the sidebar to browse packages and classes.\n");
        return builder.toString();
    }

    private String renderGroupPage(String title, List<ClassDoc> classes, boolean preferAlias, Map<String, List<ClassDoc>> categories) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\noutline: false\n---\n\n");
        builder.append("# ").append(title).append("\n\n");
        if ((categories == null || categories.isEmpty()) && classes.isEmpty()) {
            builder.append("No entries found.\n");
            return builder.toString();
        }
        if (categories != null && !categories.isEmpty()) {
            for (Map.Entry<String, List<ClassDoc>> entry : categories.entrySet()) {
                builder.append("### ").append(entry.getKey()).append("\n\n");
                renderGroupEntries(entry.getValue(), preferAlias, builder);
                builder.append("\n");
            }
            return builder.toString();
        }
        renderGroupEntries(classes, preferAlias, builder);
        return builder.toString();
    }

    private void renderGroupEntries(List<ClassDoc> entries, boolean preferAlias, StringBuilder builder) {
        if (entries == null || entries.isEmpty()) {
            builder.append("No entries found.\n");
            return;
        }
        for (ClassDoc clz : entries) {
            String linkText = preferAlias && hasAlias(clz)
                ? clz.alias()
                : clz.qualifiedName();
            builder.append("- [")
                .append(linkText)
                .append("](./")
                .append(classPath(clz))
                .append(".md)");
            if (preferAlias && hasAlias(clz) && !linkText.equals(clz.qualifiedName())) {
                builder.append(" (`").append(clz.qualifiedName()).append("`)");
            }
            builder.append("\n");
        }
    }

    private boolean hasAlias(ClassDoc clz) {
        return clz.alias() != null && !clz.alias().isBlank();
    }

    private Map<String, List<ClassDoc>> groupByCategory(DocletModel model, String targetGroup) {
        Map<String, List<ClassDoc>> categories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if (!targetGroup.equals(clz.group())) {
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
        StringBuilder builder = new StringBuilder();
        builder.append("---\noutline: deep\n---\n\n");
        builder.append("# ").append(displayTitle(clz)).append("\n\n");
        builder.append(clz.qualifiedName()).append("\n\n");
        String desc = formatDescription(clz.docComment());
        builder.append(desc.isEmpty() ? "TODO: No description supplied" : desc);
        if ("Library".equals(clz.group())) {
            String accessName = clz.alias() == null || clz.alias().isEmpty() ? clz.name() : clz.alias();
            builder.append("\nAccessible in scripts via the global `").append(accessName).append("` variable.");
        }
        builder.append("\n\n");

        renderMemberSection(builder, clz, MemberKind.CONSTRUCTOR, "Constructors");
        renderMemberSection(builder, clz, MemberKind.FIELD, "Fields");
        renderMemberSection(builder, clz, MemberKind.METHOD, "Methods");
        return builder.toString();
    }

    private String displayTitle(ClassDoc clz) {
        if ("Event".equals(clz.group()) && hasAlias(clz)) {
            return clz.alias();
        }
        return clz.name();
    }

    private void renderMemberSection(StringBuilder builder, ClassDoc clz, MemberKind kind, String title) {
        List<MemberDoc> members = clz.members().stream()
            .filter(member -> member.kind() == kind)
            .toList();
        if (members.isEmpty()) {
            return;
        }
        builder.append("## ").append(title).append("\n\n");
        for (MemberDoc member : members) {
            builder.append("### ").append(renderMemberTitle(member)).append("\n\n");
            String signature = renderSignature(member);
            builder.append("**Signature:** `").append(signature.replace("`", "\\`")).append("`\n\n");
            String desc = formatDescription(member.docComment());
            if (!desc.isEmpty()) {
                builder.append(desc).append("\n\n");
            }
            String since = getTagText(member.docComment(), DocTagKind.SINCE);
            if (!since.isEmpty()) {
                builder.append("**Since:** ").append(formatDocText(since)).append("\n\n");
            }
            if (hasDeprecatedTag(member.docComment())) {
                String deprecated = getTagText(member.docComment(), DocTagKind.DEPRECATED);
                builder.append("**Deprecated:** ").append(formatDocText(deprecated)).append("\n\n");
            }
            appendParamDocs(builder, member);
            if (member.kind() == MemberKind.METHOD) {
                String ret = getTagText(member.docComment(), DocTagKind.RETURN);
                if (!ret.isEmpty()) {
                    builder.append("**Returns:** ").append(formatDocText(ret)).append("\n\n");
                }
            }
            appendSeeDocs(builder, member.docComment());
        }
    }

    private String renderMemberTitle(MemberDoc member) {
        if (member.kind() == MemberKind.CONSTRUCTOR) {
            return "new " + member.name() + "(" + renderParamNames(member) + ")";
        } else if (member.kind() == MemberKind.FIELD) {
            return member.name();
        }
        return member.name() + "(" + renderParamNames(member) + ")";
    }

    private String renderParamNames(MemberDoc member) {
        StringBuilder builder = new StringBuilder();
        for (ParamDoc param : member.params()) {
            builder.append(param.name()).append(", ");
        }
        if (!member.params().isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        return builder.toString();
    }

    private String renderSignature(MemberDoc member) {
        if (member.kind() == MemberKind.FIELD) {
            return member.name() + ": " + renderType(member);
        }
        StringBuilder builder = new StringBuilder();
        if (member.kind() == MemberKind.CONSTRUCTOR) {
            builder.append("new ");
        }
        builder.append(member.name()).append("(");
        if (member.replaceParams() != null && !member.replaceParams().isBlank()) {
            builder.append(member.replaceParams());
        } else {
            for (ParamDoc param : member.params()) {
                builder.append(renderParam(param)).append(", ");
            }
            if (!member.params().isEmpty()) {
                builder.setLength(builder.length() - 2);
            }
        }
        builder.append(")");
        if (member.kind() != MemberKind.CONSTRUCTOR) {
            builder.append(": ").append(renderType(member));
        }
        return builder.toString();
    }

    private String renderParam(ParamDoc param) {
        return typeResolver.format(param.type(), TargetLanguage.MARKDOWN) + " " + param.name();
    }

    private String renderType(MemberDoc member) {
        if (member.replaceReturn() != null && !member.replaceReturn().isBlank()) {
            return member.replaceReturn();
        }
        return typeResolver.format(member.returnType(), TargetLanguage.MARKDOWN);
    }

    private String getTagText(DocComment comment, DocTagKind kind) {
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == kind) {
                return tag.text();
            }
        }
        return "";
    }

    private String formatDescription(DocComment comment) {
        if (comment == null) {
            return "";
        }
        String text = comment.description();
        if (text == null || text.isBlank()) {
            text = comment.summary();
        }
        return text == null ? "" : formatDocText(text);
    }

    private boolean hasDeprecatedTag(DocComment comment) {
        if (comment == null) {
            return false;
        }
        return comment.tags().stream().anyMatch(tag -> tag.kind() == DocTagKind.DEPRECATED);
    }

    private void appendParamDocs(StringBuilder builder, MemberDoc member) {
        List<ParamDoc> params = member.params();
        boolean hasDocs = params.stream().anyMatch(param -> param.description() != null && !param.description().isBlank());
        if (!hasDocs) {
            return;
        }
        builder.append("**Parameters:**\n");
        for (ParamDoc param : params) {
            String desc = param.description() == null ? "" : formatDocText(param.description());
            if (desc.isBlank()) {
                continue;
            }
            builder.append("- `").append(param.name()).append("`: ").append(desc).append("\n");
        }
        builder.append("\n");
    }

    private void appendSeeDocs(StringBuilder builder, DocComment comment) {
        if (comment == null) {
            return;
        }
        List<String> sees = new ArrayList<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.SEE && tag.text() != null && !tag.text().isBlank()) {
                sees.add(formatDocText(tag.text()));
            }
        }
        if (sees.isEmpty()) {
            return;
        }
        builder.append("**See:**\n");
        for (String see : sees) {
            builder.append("- ").append(see).append("\n");
        }
        builder.append("\n");
    }

    private String formatDocText(String text) {
        if (text == null) {
            return "";
        }
        String formatted = text.trim();
        if (formatted.isEmpty()) {
            return "";
        }
        formatted = formatted.replaceAll("\n <p>", "\n")
            .replaceAll("</?pre>", "```");
        formatted = HTML_LINK.matcher(formatted).replaceAll("[$2]($1)");
        formatted = formatted.replace("&lt;", "<").replace("&gt;", ">");
        formatted = formatted.replaceAll("(?<=[.,:;>]) ?\n", "  \n");
        formatted = convertLinkTags(formatted);
        String trimmed = formatted.trim();
        if (looksLikeSignature(trimmed)) {
            formatted = convertSignature(trimmed);
        }
        return formatted.trim();
    }

    private String convertLinkTags(String text) {
        java.util.regex.Matcher matcher = LINK_TAG.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String sig = matcher.group(1).trim();
            String mapped = mapSimpleLinkSignature(sig);
            String replacement = mapped != null ? mapped : "{@link " + convertSignature(sig) + "}";
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private boolean looksLikeSignature(String text) {
        if (text.startsWith("#")) {
            return true;
        }
        return text.matches("^xyz\\.wagyourtail\\.[^#]+\\w$")
            || text.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$");
    }

    private String convertSignature(String sig) {
        if (sig.matches("^xyz\\.wagyourtail\\.[^#]+\\w$")) {
            return sig.replaceFirst("^.+\\.(?=[^.]+$)", "");
        }
        if (sig.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$")) {
            return "Packages." + sig;
        }
        if (sig.startsWith("#")) {
            return sig.substring(1);
        }
        return sig
            .replaceFirst("^(?:xyz\\.wagyourtail\\.jsmacros\\.(?:client\\.api|core)\\.library\\.impl\\.)?F([A-Z]\\w+)#", "$1.")
            .replaceFirst("#", ".");
    }

    private String mapSimpleLinkSignature(String sig) {
        if (JAVA_NUMBER_TYPES.containsKey(sig)) {
            return JAVA_NUMBER_TYPES.get(sig);
        }
        return switch (sig) {
            case "java.lang.String" -> "string";
            case "java.lang.Boolean" -> "boolean";
            default -> null;
        };
    }
}
