package com.jsmacrosce.doclet.core.tsdoclet;

import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import com.jsmacrosce.FileHandler;
import com.jsmacrosce.doclet.core.BasicDocCommentParser;
import com.jsmacrosce.doclet.core.BasicTypeResolver;
import com.jsmacrosce.doclet.core.DocletModelBuilder;
import com.jsmacrosce.doclet.core.render.TsRenderer;
import com.jsmacrosce.doclet.options.IgnoredItem;
import com.jsmacrosce.doclet.options.OutputDirectory;
import com.jsmacrosce.doclet.options.Version;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main implements Doclet {
    public static Reporter reporter;
    public static DocTrees treeUtils;

    @Override
    public void init(Locale locale, Reporter reporter) {
        Main.reporter = reporter;
    }

    @Override
    public String getName() {
        return "TypeScript Generator";
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
                new Version(),
                new OutputDirectory(),
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
        treeUtils = environment.getDocTrees();

        File outDir = OutputDirectory.outputDir;
        if (outDir == null) {
            reporter.print(Diagnostic.Kind.ERROR, "Output directory not set\n");
            return false;
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            reporter.print(Diagnostic.Kind.ERROR, "Failed to create version dir\n");
            return false;
        }

        FileHandler outputTS = new FileHandler(new File(outDir, "JsMacros-" + Version.version + ".d.ts"));
        BasicTypeResolver typeResolver = new BasicTypeResolver();
        DocletModelBuilder builder = new DocletModelBuilder(typeResolver, new BasicDocCommentParser(treeUtils));
        var model = builder.build(environment);
        TsRenderer renderer = new TsRenderer(typeResolver);

        try {
            outputTS.write(renderer.render(model));
        } catch (IOException e) {
            reporter.print(Diagnostic.Kind.ERROR, "Failed to write TypeScript output: " + e.getMessage());
            return false;
        }
        return true;
    }

}
