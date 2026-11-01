---
outline: deep
---
# How to read the API reference

The API reference is generated from the JsMacros and Minecraft sources for one
Minecraft target. It is the place for exact signatures, raw Minecraft types, and
every member of a class. It is not a tutorial; the [Guide](/guide/) teaches the
concepts.

## The four groups

- **Libraries** are the globals a script starts with: `Chat`, `World`, `Player`,
  `Hud`, `JsMacros`, and so on. A library page shows the methods you call directly.
- **Events** are the payloads delivered to listeners. Their pages list the fields
  you can read and the cancellation behaviour.
- **Classes** are the objects you get back from libraries and events: entity
  helpers, inventories, draw elements, screens, and builders.
- **Type namespaces** are stable script names for Java types, such as
  `EntityHelpers.Horse`. See [Script type namespaces](/guide/advanced/script-types).

A method that returns a class is also a link to that class. Follow the return type
when you are not sure what the result can do.

## The page title and the Java name

The large title is the **script name**: the alias for a library or event, or the
short name of a class. The small monospace line under it is the fully qualified
Java name. The Java name is useful for `Java.type` and reflection; scripts should
usually use the short name.

Nested types are shown under their owner. For example, `JsMacros.EventAndContext`
is a supporting type of `JsMacros`, not a separate global.

## Badges and metadata

- **read-only** means the field is exposed for reading. It is not a constant and
  may still change over time.
- **deprecated** means the member is kept for compatibility but should not be used
  in new scripts.
- **Since** is the JsMacros version that introduced the member, not the Minecraft
  version.

## Overloads

Methods with the same name are grouped under one heading. Each signature is listed
separately with its own parameters and description. Read the parameter list to
choose the overload; the short description may be shared, but the accepted values
and the result can differ.

Constructors are shown only for types that scripts are meant to construct. Library
globals usually do not show constructors because you receive them from the runtime.

## Version differences

Every snapshot is generated for one Minecraft target. A method can exist on one
target and not another, and a raw Minecraft type can have a different name or
shape. The version selector in the navigation bar tells you which target you are
reading.

If a feature exists only on some versions, its page says so. A compatibility stub
can exist on a target without the full behavior; read the description, not just the
signature.

## Searching

Local search is scoped to the reference you are reading. Results from another
Minecraft target or another JsMacros version are hidden, so a search cannot send
you to a method that does not exist on your target.

On a Guide page, search is scoped to the Guide. To search the API, open the
reference first.

## A good lookup order

1. Start with the **library** you already have (`World`, `Player`, `Hud`).
2. Follow the **return type** into a class page.
3. For an event, read the **event page** for its fields and cancellation rules.
4. If a value is an unclear helper, check **type namespaces** before using
   reflection.
5. Only then reach for **raw access or reflection**. See
   [Raw Minecraft access](/guide/advanced/raw-minecraft).
