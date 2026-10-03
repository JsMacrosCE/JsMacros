#!/usr/bin/env python3
"""Compile actual non-Minecraft source files and run standalone JVM regressions."""

import argparse
import os
import re
import subprocess
import tempfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath", help="Override core dependencies (annotations, gson, commons-io, guava; no Graal jars)")
    parser.add_argument("--graal", action="store_true", help="Also test the real Graal engine with separate mod/extension loaders")
    parser.add_argument("--graal-classpath", help="Override isolated Graal dependency jars for --graal")
    parser.add_argument("--javac", default="javac")
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    test_directory = Path(__file__).resolve().parent
    root = test_directory.parents[2]
    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
    if args.classpath:
        classpath = args.classpath
    else:
        dependencies = []
        for artifact in ["org.jetbrains/annotations", "com.google.code.gson/gson", "commons-io/commons-io", "com.google.guava/guava"]:
            candidates = sorted((cache / artifact).glob("*/*/*.jar"))
            candidates = [p for p in candidates if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
            if not candidates:
                raise SystemExit(f"Missing cached dependency {artifact}; compile the mod first or supply --classpath")
            dependencies.append(str(candidates[-1]))
        classpath = os.pathsep.join(dependencies)
    graal_classpath = None
    if args.graal:
        if args.graal_classpath:
            graal_classpath = args.graal_classpath
        else:
            dependencies = []
            versions = (root / "gradle/libs.versions.toml").read_text()
            version = re.search(r'^graal\s*=\s*"([^"]+)"', versions, re.MULTILINE).group(1)
            for artifact in [
                "org.graalvm.polyglot/polyglot", "org.graalvm.js/js-language",
                "org.graalvm.truffle/truffle-api", "org.graalvm.regex/regex",
                "org.graalvm.shadowed/icu4j", "org.graalvm.sdk/collections",
                "org.graalvm.sdk/nativeimage", "org.graalvm.sdk/word",
            ]:
                candidates = sorted((cache / artifact / version).glob("*/*.jar"))
                candidates = [p for p in candidates if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
                if not candidates:
                    raise SystemExit(f"Missing cached Graal dependency {artifact}:{version}; supply --graal-classpath or build the JS extension")
                dependencies.append(str(candidates[-1]))
            graal_classpath = os.pathsep.join(dependencies)
    source_root = root / "common/src/main/java/com/jsmacrosce/jsmacros"
    sources = [source_root / f"core/library/impl/classes/{name}.java" for name in ["HTTPRequest", "FileHandler"]]
    filters = source_root / "client/api/classes/worldscanner/filter"
    sources += [filters / "BasicFilter.java", filters / "ClassWrapperFilter.java"]
    for directory in ["api", "compare", "logical"]:
        sources.extend(sorted((filters / directory).glob("*.java")))
    sources.append(test_directory / "MiscApiCoreRegressions.java")
    if args.graal:
        sources.append(test_directory / "MiscApiGraalClassLoaderRegressions.java")
    scratch = Path("/tmp/opencode") if Path("/tmp/opencode").is_dir() else None
    with tempfile.TemporaryDirectory(prefix="misc-api-core-", dir=scratch) as output:
        core_output = Path(output) / "mod"
        core_output.mkdir()
        subprocess.run([args.javac, "-cp", classpath, "-d", str(core_output), *map(str, sources)], check=True, cwd=root)
        core_classpath = str(core_output) + os.pathsep + classpath
        subprocess.run([args.java, "-cp", core_classpath, "MiscApiCoreRegressions"], check=True, cwd=root)
        if args.graal:
            extension_output = Path(output) / "extension"
            extension_output.mkdir()
            loader_source = root / "extension/graal/src/main/java/com/jsmacrosce/jsmacros/graal/language/impl/GraalHostClassLoader.java"
            subprocess.run([
                args.javac, "-cp", core_classpath + os.pathsep + graal_classpath, "-d", str(extension_output),
                str(loader_source), str(test_directory / "MiscApiGraalRegressions.java"),
            ], check=True, cwd=root)
            subprocess.run([
                args.java, "-cp", core_classpath, "MiscApiGraalClassLoaderRegressions",
                str(extension_output), graal_classpath, str(test_directory / "misc_api_regressions.js"),
            ], check=True, cwd=root)


if __name__ == "__main__":
    main()
