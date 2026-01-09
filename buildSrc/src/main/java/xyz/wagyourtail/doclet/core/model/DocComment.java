package xyz.wagyourtail.doclet.core.model;

import java.util.List;

public record DocComment(String summary, String description, List<DocTag> tags) {
}
