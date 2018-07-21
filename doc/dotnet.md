# Dotnet Translation

Pravda project allows you to write _programs_ in subset of C\# language.
Pravda Translator translates [CIL](https://en.wikipedia.org/wiki/Common_Intermediate_Language) to Pravda bytecode.

## How to compile program
Pravda provides special `expload.dll` file with auxiliary methods for translation from CIL to Pravda bytecode. 

This dll file serves **only as meta info** for translator, 
it __doesn't__ provide any meaningful implementation for these methods.
Translator just looks at calls of these methods and generates necessary Pravda bytecode.
 
You can download it [here](../dotnet/src/test/resources/expload.dll). 
Source of this dll can be found [here](../dotnet/src/test/resources/expload.cs). 
To build it from source, run:
```bash
csc expload.cs -target:Library
```
This outputs `expload.dll` file.

To compile your C# program with `expload.dll`:
```bash
csc your_program.cs /reference:expload.dll
```

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
- Access to sender address via `Info.Sender()` method;
- Class methods that are translated to program methods; 
- Integer primitive types (`int`, `short`, `byte`, `uint`) and `bool`;
- Basic arithmetics and logical operations; 
- Local variables and method arguments;
- If conditions and loops;
- `String`s and auxiliary methods (`+`, access to particular chars, `Slice`);
- `Bytes` (immutable byte arrays), auxiliary methods (access to particular bytes, `Slice`, `Concat`), creation from `byte[]`: `new Bytes(bytes_array)`;
- Arrays of primitive types (`int`, `byte`, `String`), reading and writing of particular elements;
- Explicit conversion of primitive types via 
`System.Convert.ToByte`, `System.Convert.ToChar`,`System.Convert.ToInt16`,`System.Convert.ToInt32`,`System.Convert.ToDouble`,`System.Convert.ToBoolean`,`System.Convert.ToString`
- Cryptographic functions: Ripemd160 hashing, validation of Ed25519 Signature. See more in [Standard library](ref/vm/stdlib.md) docs.

Things that are *not* supported:
- User defined classes as values, classes is used only as _programs_ for the moment;
- Standard C# library;
- Standard C# collections.

## Examples

You can look at several examples of test _programs_ to learn current abilities of translation:
- [Simple _program_](../dotnet/src/test/resources/smart_program.cs) with `balanceOf` and `transfer` methods similar to corresponding methods from [ERC20](https://theethereum.wiki/w/index.php/ERC20_Token_Standard)
- [Zoo _program_](../dotnet/src/test/resources/zoo_program.cs) that allows you to create zoos, pets and breed them. 
- [String examples](../dotnet/src/test/resources/strings.cs) that show how to operate with `String`s.
- [Array examples](../dotnet/src/test/resources/arrays.cs) that show how to operate with arrays.
