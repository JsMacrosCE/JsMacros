# JsMacrosCE devkit

Each Minecraft-numbered folder contains that target's TypeScript headers, Python
stubs and legacy HTML reference. Use only the folder matching your Minecraft
version when configuring an editor; do not load every version's typings together.
The TypeScript example project is in `<Minecraft>/typescript/`.

`vitepress/` contains the **built** documentation website, with a Minecraft version
selector and local search. No Node installation or site build is needed. To view
it locally with Python 3, run this from the extracted devkit:

```sh
python3 serve-docs.py
```

Open <http://127.0.0.1:8000/>. On Windows, `py -3 serve-docs.py` also works.
Use `--port 8001` to select another port. The server listens only on your machine
and supports the site's extensionless URLs; opening its HTML with `file://` or
using a server without clean-URL support does not provide a working VitePress site.

Alternatively, serve `vitepress/` at the root of an HTTP static server configured
to resolve extensionless paths to their `.html` files. Legacy HTML remains in
`<Minecraft>/web/` for each target.

This devkit contains no language runtimes. JavaScript is included in the mod;
optional GraalPython is a separate `jsmacrosce-ext-graalpy-<release>.jar` download.
Install that JAR in your Minecraft instance's `config/jsMacros/Extensions/`
directory (capital `E`), not in `mods/`.
