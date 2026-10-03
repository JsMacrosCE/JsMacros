import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Launches the Graal regression suite with separate mod and extension class loaders.
 *
 * <p>The application classpath contains the production filter interface but must
 * not contain Graal. An extension loader receives the Graal test classes, the
 * production host-loader bridge, and Graal dependencies. This separation exposes
 * adapter failures that a combined application classpath would hide.</p>
 *
 * <p>This is a standalone test launcher, not a Minecraft entry point. It restores
 * the thread context class loader and closes the extension loader after the suite
 * finishes, including when a test fails.</p>
 */
public class MiscApiGraalClassLoaderRegressions {
    /**
     * Verifies that Graal is absent from the application loader, then invokes
     * {@link MiscApiGraalRegressions#main(String[])} through the extension loader.
     *
     * @param args extension-class directory, Graal dependency classpath separated
     *             by the platform path separator, and client regression macro path
     * @throws Throwable if setup fails or the invoked suite throws; reflective
     *                   invocation failures are unwrapped to preserve their cause
     */
    public static void main(String[] args) throws Throwable {
        ClassLoader modLoader = MiscApiGraalClassLoaderRegressions.class.getClassLoader();
        try {
            modLoader.loadClass("org.graalvm.polyglot.Value");
            throw new AssertionError("Test invalid: Graal leaked onto the mod/application classpath");
        } catch (ClassNotFoundException expected) {
            // This is the separation used by packaged JsMacros language extensions.
        }
        List<URL> urls = new ArrayList<>();
        urls.add(Path.of(args[0]).toUri().toURL());
        for (String dependency : args[1].split(Pattern.quote(System.getProperty("path.separator")))) {
            urls.add(Path.of(dependency).toUri().toURL());
        }
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader extensionLoader = new URLClassLoader(urls.toArray(URL[]::new), modLoader)) {
            Thread.currentThread().setContextClassLoader(extensionLoader);
            Class<?> tests = extensionLoader.loadClass("MiscApiGraalRegressions");
            if (tests.getClassLoader() != extensionLoader) throw new AssertionError("Graal tests must be extension-loaded");
            try {
                tests.getMethod("main", String[].class).invoke(null, (Object) new String[]{args[2]});
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
