package xyz.wagyourtail.doclet.core.pydoclet;

import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import xyz.wagyourtail.doclet.core.BasicDocCommentParser;
import xyz.wagyourtail.doclet.core.BasicTypeResolver;
import xyz.wagyourtail.doclet.core.DocletModelBuilder;
import xyz.wagyourtail.doclet.core.render.PythonWriter;
import xyz.wagyourtail.doclet.options.IgnoredItem;
import xyz.wagyourtail.doclet.options.OutputDirectory;
import xyz.wagyourtail.doclet.options.Version;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
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
        return "Python Generator (Core)";
    }

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
        DocTrees trees = environment.getDocTrees();
        BasicTypeResolver typeResolver = new BasicTypeResolver();
        typeResolver.setPythonAliasEnabled(false);
        DocletModelBuilder builder = new DocletModelBuilder(typeResolver, new BasicDocCommentParser(trees));
        var model = builder.build(environment);

        File outDir = OutputDirectory.outputDir;
        if (outDir == null) {
            reporter.print(Diagnostic.Kind.ERROR, "Output directory not set\n");
            return false;
        }
        if (outDir.exists() && !deleteRecursively(outDir)) {
            reporter.print(Diagnostic.Kind.ERROR, "Failed to remove old python output\n");
            return false;
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            reporter.print(Diagnostic.Kind.ERROR, "Failed to create output directory\n");
            return false;
        }

        PythonWriter writer = new PythonWriter();
        try {
            writer.write(model, outDir, Version.version);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
}
