# C# translation overview

Pravda project allows you to write _programs_ in subset of C\# language.
Pravda Translator translates [CIL](https://en.wikipedia.org/wiki/Common_Intermediate_Language) to Pravda bytecode.

## Supported C# subset

Pravda Translator supports only part of all C# features. 

For the moment it supports the following:
- Access to the _storage_ via class fields;
- Access to the _storage_ via `Mapping<K, V>` (`get`, `getDefault`, `put`, `exists` methods);
- Access to sender address via `Info.Sender()` method;
- Access to list of contract's callers' addresses via `Info.Callers()` method;
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
- Cryptographic functions: Ripemd160 hashing, validation of Ed25519 Signature. See more in [Standard library](../virtual-machine/stdlib.md) docs.
- User defined classes (although you can't store them in the storage yet).
- Calling other programs via `ProgramHelper.Program<...>` interface.
See some examples ([Pcall.cs](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/Pcall.cs), [PcallProgram.cs](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/PcallProgram.cs)).
- Create events in your program via `Log.Event("name of event", <some_data>)`, see [Event.cs](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/Event.cs)

Things that are *not* supported:
- Standard C# library (except of some specific functions from the list above);
- Standard C# collections.

## Examples

You can look at several examples of test _programs_ to learn current abilities of translation:
- [String examples](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/Strings.cs) that show how to operate with `String`s.
- [Array examples](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/Arrays.cs) that show how to operate with arrays.
- [Simple program](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/SmartProgram.cs) with `balanceOf` and `transfer` methods similar to corresponding methods from [ERC20](https://theethereum.wiki/w/index.php/ERC20_Token_Standard)
- [Buffer](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/IntBuffer.cs) -- Dynamic resizable array implemented in C#.
- [Zoo program](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/ZooProgram.cs) that allows you to create zoos, pets and breed them.
- [Poker program](https://github.com/expload/pravda/blob/master/dotnet-tests/resources/Poker.cs) that implements simple poker game on the blockchain. _(poker.cs was provided by [Ducatur team](https://github.com/DucaturFw/ExploadHackathonContract))

