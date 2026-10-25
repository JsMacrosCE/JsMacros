# Script type namespaces

JavaScript and GraalPython scripts receive these globals automatically:

| Namespace | Types |
| --- | --- |
| `EntityHelpers` | Entity helpers, including `Horse`, `AbstractHorse`, `Living`, `Mob`, and `Player` |
| `Inventories` | Inventory wrappers, including `Villager`, `Anvil`, `Recipe`, and `Container` |
| `Widgets` | `Clickable`, `Button`, `CheckBox`, `CyclingButton`, `LockButton`, `Slider`, `TextField` |
| `RenderElements` | 2D/3D elements and interfaces, including `Text`, `Image`, `Rect`, `Box`, `Surface`, `Element`, and `Alignable` |
| `NBTHelpers` | `Element`, `Number`, `List`, `Compound` |
| `HitResults` | `Result`, `Block`, `Entity` |
| `ScriptClasses` | `CustomImage`, `KeyTracker` |

Generated references list every member and link to its canonical API page. The
in-game editor suggests namespace members; generated TypeScript and Python files
also expose them for external editors.

```js
const entities = World.getEntities();
if (entities !== null) {
    for (const ent of entities) {
        if (ent instanceof EntityHelpers.Horse) {
            Chat.log(ent.getVariant());
        }
    }
}
```

These are real host types, not predicates based on registry IDs. Inheritance works:
a horse helper is also an `EntityHelpers.AbstractHorse` and `EntityHelpers.Animal`.
`ent.is("horse")` remains available when the intended question is the registry ID.
Exports do not change factory dispatch: a helper type's presence in a namespace
does not imply that every entity factory constructs that specialization.

Members support constructors and static access when the underlying type provides
them. Interfaces and abstract classes are useful for checks but cannot be
constructed. Exported constructors retain their existing parameters and threading
requirements; exporting a helper does not add a raw-entity conversion.

```js
const image = ScriptClasses.CustomImage.createWidget(16, 16, "badge");
```

In Python, use the same names with `isinstance`. Guard nullable values explicitly:
GraalPython's Java interop can regard `None` as assignable to a host reference type.

```python
entities = World.getEntities()
if entities is not None:
    for ent in entities:
        if ent is not None and isinstance(ent, EntityHelpers.Horse):
            Chat.log(ent.getVariant())
```

Namespace members cannot be reassigned, added, or removed. The namespace global
itself can be shadowed like existing library globals. The exported type's own
mutable static fields retain their existing behavior. `HitResults.Block` includes
misses (`isMissed()` distinguishes them), and `NBTHelpers.List` includes array tags.
Existing `Java.type(...)` and reflection APIs remain supported.

## Maintaining exports

The authoritative catalogs live under
`common/src/main/java/com/jsmacrosce/jsmacros/client/api/classes/types/`. A catalog
has `@ScriptNamespace("StableName")` and public static final `Class<T>` fields:

```java
@ScriptNamespace("EntityHelpers")
public final class EntityHelpers {
    private EntityHelpers() {}
    public static final Class<HorseEntityHelper> Horse = HorseEntityHelper.class;
}
```

Register a new catalog with `runner.scriptTypeRegistry.register(Catalog.class)`
during profile initialization. Adding a member to an existing catalog needs no
second registration list. Runtime bindings and all four documentation formats use
the same catalogs; no classpath scanning or package-derived public names are used.
Keep field names stable across Java moves and Stonecutter targets. `Class<T>` must
match the actual class literal; raw `Class`, wildcards, mutable fields, conflicting
namespace names, and null exports are rejected. Exports should target documented
public API types so Python and website references can link to their declarations.

Core metadata contains no Graal values. Each script context resolves its own host
symbols lazily. Other Graal languages keep their existing bindings; these type
namespaces currently support JavaScript and GraalPython.

## Validation

Run the engine tests and compiled-catalog checks without starting Minecraft:

```sh
./gradlew --no-configuration-cache :extension:graal:js:test :extension:graal:python:test \
  :common:1.21.8:testScriptTypeRegistry :common:26.1.2:testScriptTypeRegistry \
  :common:26.3:testScriptTypeRegistry :buildSrc:testDocletLinks
./gradlew --no-configuration-cache :fabric:26.1.2:compileJava :neoforge:26.1.2:compileJava
```

Generate references and compare every export and namespace example across formats:

```sh
./gradlew --no-configuration-cache -PdocsMinecraftVersions=1.21.8,26.1.2 \
  generateTSDoc generatePyDoc generateWebDoc generateVitepressDoc
python3 buildSrc/src/test/python/test_script_type_artifacts.py 1.21.8 26.1.2
```

Type-check `docs/typescript/tests/script-types.ts` together with one generated
target's `typescript/headers/*.d.ts`, using TypeScript 5.3 or newer for interface
`Symbol.hasInstance` narrowing. Use `--strict --noEmit --target ES2022 --lib ES2022`
and the shipped `--skipLibCheck` setting. The negative `@ts-expect-error` assertions
ensure invalid names, writes, and subtype return types are still rejected.
