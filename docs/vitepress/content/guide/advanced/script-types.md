---
outline: deep
---
# Script type namespaces

Many JsMacros factories return a helper whose concrete type depends on what they
found: an entity, an open container, a screen widget, a render element, or an NBT
tag. The type namespace gives those concrete types a stable name, so a script can
check for the useful one without knowing its Java package.

```js
const entities = World.getEntities();
if (entities !== null) {
  for (const entity of entities) {
    if (entity instanceof EntityHelpers.Horse) {
      Chat.log(entity.getVariant());
    }
  }
}
```

## The namespaces at a glance

| Namespace | Use it to check for | Example member |
| --- | --- | --- |
| `EntityHelpers` | A specific entity helper or one of its base classes | `EntityHelpers.Horse`, `EntityHelpers.Living` |
| `Inventories` | The kind of open inventory | `Inventories.Container`, `Inventories.Anvil` |
| `Widgets` | The kind of screen widget | `Widgets.Button`, `Widgets.Slider` |
| `RenderElements` | A 2D or 3D drawing element | `RenderElements.Text`, `RenderElements.Box` |
| `NBTHelpers` | The kind of NBT value | `NBTHelpers.Compound`, `NBTHelpers.List` |
| `HitResults` | What a ray trace hit | `HitResults.Block`, `HitResults.Entity` |
| `ScriptClasses` | Utility classes with static entry points | `ScriptClasses.CustomImage` |

The reference has a [Type namespaces](/api/types/) section that lists every member
and links to its Java class.

## `instanceof`, not a string check

These are real host types. Inheritance works: a horse helper is an
`EntityHelpers.Horse`, an `EntityHelpers.AbstractHorse`, and an
`EntityHelpers.Animal`.

`entity.is("horse")` answers a different question: it checks the game's entity
registry ID. Use `instanceof` for helper capabilities and `is(...)` when the
question is about the entity type.

## Factories choose the helper

The namespace does not change what a factory returns. The factory inspects the
actual object first and constructs the matching helper. `EntityHelper.create`
returns a `HorseEntityHelper` for a horse, a `ZombieEntityHelper` for a zombie, and
a base `EntityHelper` only when it has no more specific class for the object. The
namespace then gives that returned type a name you can check.

Use `instanceof` because a method may declare a base return type, or may return more
than one kind of helper. If a factory returns a base helper for a known entity class
that has a more-specific exported helper, that is a missing specialization in the
factory, not something the namespace can fix.

## Constructors and static access

The exported names are the real Java types. If a type has a public constructor or
static methods, they are available and keep their existing parameters and threading
requirements. Exporting a helper does not add a raw-entity conversion or make a
constructor safe to call from the wrong thread.

Interfaces and abstract classes are useful for checks but cannot be constructed.

## Python

Python uses the same names with `isinstance`:

```python
entities = World.getEntities()
if entities is not None:
    for ent in entities:
        if ent is not None and isinstance(ent, EntityHelpers.Horse):
            Chat.log(ent.getVariant())
```

Guard nullable values explicitly. GraalPython's Java interop can regard `None` as
assignable to a host reference type.

## When not to use a namespace

Use the registry ID string when the question is "what kind of game object is this?"
Use `Java.type` or [`Reflection`](/guide/advanced/raw-minecraft) for a Java class or
raw Minecraft method that is not part of the script API. The namespaces are a
short-name index for helper types, not a general class lookup.

## Related pages

- [Raw Minecraft access and reflection](/guide/advanced/raw-minecraft)
- [Player and world](/guide/game/player-and-world)
- API reference: [Type namespaces](/api/types/),
  [`EntityHelpers`](/api/types/EntityHelpers),
  [`Widgets`](/api/types/Widgets),
  [`RenderElements`](/api/types/RenderElements)
