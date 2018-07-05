# Dotnet Translation

Pravda project allows you to write _programs_ in subset of C\# language.
Pravda Translator translates [CIL](https://en.wikipedia.org/wiki/Common_Intermediate_Language) to Pravda bytecode.

## How to run translation

Pravda CLI has specific command to run translation of `.exe` file produced by C# compiler.
```
pravda compile dotnet --input input.exe --output output.pravda
```

## Supported subset of C#

Pravda Translator supports only part of all C# features. 

For the moment it supports the following:
- Access to the _storage_ via class fields;
- Access to the _storage_ via `Mapping<K, V>` (`get`, `getDefault`, `put`, `exists` methods);
- Access to sender address via `Address sender` field;
- Class methods that are translated to regular functions;
- Integer primitive types (`int`, `short`, `byte`, `uint`) and `bool`;
- Basic arithmetics and logical operations; 
- Local variables and method arguments;
- If conditions and loops.

Things that are *not* supported:
- User defined classes as values, classes is used only as _programs_ for the moment;
- Standard C# library;
- Standard C# collections.
