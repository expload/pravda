# Dotnet Translation

Pravda project allows you to write _programs_ in subset of C\# language.
Pravda Translator translates [CIL](https://en.wikipedia.org/wiki/Common_Intermediate_Language) to Pravda bytecode.

## How to compile program
Pravda provides special `expload.dll` file with auxiliary methods for translation from CIL to Pravda bytecode. 

This dll file serves **only as meta info** for translator, 
it __doesn't__ provide any meaningful implementation for these methods.
Translator just looks at calls of these methods and generates necessary Pravda bytecode.
 
You can download `expload.dll` [here](../../../dotnet/src/test/resources/expload.dll).
Source of this dll can be found [here](../../../dotnet/src/test/resources/expload.cs).

For full support of all translation features you need also to compile your program with `/debug:portable` option.
This options will trigger the creation of `your_program.pdb` file that contains various auxiliary information about C# source.
 
_Portable_ pdb files are quite new, so you need up-to-date `csc` compiler to generate them. See more [here](https://github.com/dotnet/core/blob/master/Documentation/diagnostics/portable_pdb.md).

To compile your C# program with [`expload.dll`](../../../dotnet/src/test/resources/expload.dll):
```bash
csc your_program.cs /reference:expload.dll /debug:portable
```

## How to run translation

Pravda CLI has special command to run translation of `.exe` file produced by C# compiler.
```
pravda compile dotnet --input input.exe --output output.pravda --pdb input.pdb
```
`pdb` file is optional, but it's strongly recommended to provide it (see [Compile](#how-to-compile-program) section for instructions). 

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
- Cryptographic functions: Ripemd160 hashing, validation of Ed25519 Signature. See more in [Standard library](../vm/stdlib.md) docs.
- User defined classes (although you can't store them in the storage yet).
- Calling other programs via `ProgramHelper.Program<...>` interface.
See some examples ([pcall.cs](../../../dotnet/src/test/resources/pcall.cs), [pcall_program.cs](../../../dotnet/src/test/resources/pcall_program.cs)).

Things that are *not* supported:
- Standard C# library (except of some specific functions from the list above);
- Standard C# collections.

## Examples

You can look at several examples of test _programs_ to learn current abilities of translation:
- [String examples](../../../dotnet/src/test/resources/strings.cs) that show how to operate with `String`s.
- [Array examples](../../../dotnet/src/test/resources/arrays.cs) that show how to operate with arrays.
- [Simple _program_](../../../dotnet/src/test/resources/smart_program.cs) with `balanceOf` and `transfer` methods similar to corresponding methods from [ERC20](https://theethereum.wiki/w/index.php/ERC20_Token_Standard)
- [Buffer](../../../testkit/src/test/resources/buffer.cs) -- Dynamic resizable array implemented in C#.
- [Zoo _program_](../../../dotnet/src/test/resources/zoo_program.cs) that allows you to create zoos, pets and breed them.
- [Poker _program_](../../../testkit/src/test/resources/poker.cs) that implements simple poker game on the blockchain. _(poker.cs was provided by [Ducatur team](https://github.com/DucaturFw/ExploadHackathonContract))_

