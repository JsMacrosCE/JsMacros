package xyz.wagyourtail.doclet.core.mddoclet;

import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import xyz.wagyourtail.doclet.core.BasicDocCommentParser;
import xyz.wagyourtail.doclet.core.BasicTypeResolver;
import xyz.wagyourtail.doclet.core.DocletModelBuilder;
import xyz.wagyourtail.doclet.core.render.MarkdownWriter;
import xyz.wagyourtail.doclet.mddoclet.options.Links;
import xyz.wagyourtail.doclet.mddoclet.options.McVersion;
import xyz.wagyourtail.doclet.options.IgnoredItem;
import xyz.wagyourtail.doclet.options.OutputDirectory;
import xyz.wagyourtail.doclet.options.Version;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class Main implements Doclet {
    private Reporter reporter;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "VitePressDoc Generator (Core)";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
            new Version(),
            new McVersion(),
            new OutputDirectory(),
            new Links(),
            new IgnoredItem("-doctitle", 1),
            new IgnoredItem("-notimestamp", 0),
            new IgnoredItem("-windowtitle", 1)
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_16;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        DocTrees trees = environment.getDocTrees();
        BasicTypeResolver typeResolver = new BasicTypeResolver();
        DocletModelBuilder builder = new DocletModelBuilder(typeResolver, new BasicDocCommentParser(trees));
        var model = builder.build(environment);

        File outDir = new File(OutputDirectory.outputDir.toPath().resolve("content").toString(), Version.version);
        if (!outDir.exists() && !outDir.mkdirs()) {
            reporter.print(Diagnostic.Kind.ERROR, "Failed to create version dir\n");
            return false;
        }

        MarkdownWriter writer = new MarkdownWriter(typeResolver);
        try {
            writer.write(model, outDir, Version.version, McVersion.mcVersion);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
