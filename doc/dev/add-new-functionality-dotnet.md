# Adding new functionality to the DotNet translator

The process consists of the following steps:

1) Add stub method in [Pravda.cs] without implementing any functionality(https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda.cs)
2) Describe translation of the method in [CallsTranslation](https://github.com/expload/pravda/blob/master/dotnet/src/main/scala/pravda/dotnet/translation/opcode/CallsTranslation.scala)
3) Write test C# program that uses that method. Place appropriate tests to parser, transaction and teskit folders.

### Add stub method into Pravda.cs

For example, we want to add static method `ProgramAddress` to the `Info` class. To do it we can define stub method as following:

```
public static Bytes ProgramAddress() { return null; }
```

Returning value doesn't have real meaning. So you can return the default value for returing type.

### Method translation in CallsTranslation

#### Changing the stack while calling the method

We need to tell the translator how the stack is changed during method calling. To do it we should add pattern-match case in the `deltaOffsetOne` function similarly how it doing for other methods.

For example, the method `ProgramAddress()` doesn't take parameters, but returns one value. It means, that height of the stack is changed by +1. If the method takes two parameters and returns one value, then height of the stack is changed by -1. 

#### Translation of the method to the Pravda VM opcodes

We need to add pattern-match for `Call` case with our method (similarly how it already does for other methods from [`Pravda.cs`](https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda.cs)) in the function `asmOpsOne`. There we need to implement the method with using exist Pravda VM opcodes.

For example, for the method `ProgramAddress()` we have `PADDR` opcode, so we can write the following:

```
case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "ProgramAddress", _)) =>
  Right(List(Operation(Opcodes.PADDR)))
```

### Parser and translation tests

At first, we need to add test C# program in the folder `dotnet-tests/resources/`.

#### Parser

We need to create a file with `.prs` extension in the folder `dotnet/src/test/resources/parser`. Filename can be chosen arbitrary.

The file should contain `preconditions` which describe dotnet compilation steps:

```
dotnet-compilation:
  steps:
  - target: Pravda.dll
    sources:
    - PravdaDotNet/Pravda.cs
    optimize: false
  - target: <C# program name>.exe
    sources:
    - Pravda.dll
    - dotnet-tests/resources/<C# program name>.cs
    optimize: false
```

The next command complete the test file with expected parsing result.

```
sbt dotnet/test:runMain pravda.dotnet.ParserSuite --overwrite
```

#### Translation

Create a file similarly to that described above, but the extension should be `.trs`, and the file should be created in the `dotnet/src/test/resources/translation` folder.

The next command complete the test file with expected translation result.

```
sbt dotnet/test:runMain pravda.dotnet.TranslationSuite --overwrite
```