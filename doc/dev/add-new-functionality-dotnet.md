# Adding a New Functionality to the DotNet Translator

The process consists of the following steps:

1) Add a stub method to [Pravda.cs](https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda/Pravda.cs) without implementing any functionality.
2) Describe the translation of the method in [CallsTranslation](https://github.com/expload/pravda/blob/master/dotnet/src/main/scala/pravda/dotnet/translation/opcode/CallsTranslation.scala)
3) Write a test C# program that uses this method. Place the appropriate tests to [parser](https://github.com/expload/pravda/tree/master/dotnet/src/test/resources/parser), [translation](https://github.com/expload/pravda/tree/master/dotnet/src/test/resources/translation) and [teskit](https://github.com/expload/pravda/tree/master/testkit/src/test/resources) folders.

## Stub the Method into Pravda.cs

For example, when adding the static method `ProgramAddress` to the `Info` class, we can define the stub method as follows:

```
public static Bytes ProgramAddress() { return null; }
```

The returning value is irrelevant. You can return the default value for the returning type.

## Method Translation in CallsTranslation

### Changing the Stack During Method Execution

We need to tell the translator how the stack is changed during the method execution. For this purpose, we should add a pattern-match case to the `deltaOffsetOne` function, which is similar with regard to other methods.

For example, the `ProgramAddress()` method takes no parameters, but returns one value. This means, that the stack height is changed by +1. If the method takes two parameters and returns one value, then the stack height is changed by -1.

### Method Translation to Pravda VM Opcodes

We need to add a pattern-match for the `Call` case with our method (which is similar with regard to other methods from [`Pravda.cs`](https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda/Pravda.cs)) in the function `asmOpsOne`. There, we need to implement the method using Pravda VM opcodes.

For example, for the `ProgramAddress()` method we have the `PADDR` opcode, enabling us to write the following:

```
case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "ProgramAddress", _)) =>
  Right(List(Operation(Opcodes.PADDR)))
```

## Parser and Translation Tests

At first, we need to add a C# test program to the `dotnet-tests/resources/` folder.

### Parser

We need to create a file with the `.prs` extension in `dotnet/src/test/resources/parser`. The file name can be chosen arbitrarily.

The file should contain `preconditions` which describe the dotnet compilation steps:

```
dotnet-compilation:
  steps:
  - target: Pravda.dll
    sources:
    - PravdaDotNet/Pravda/Pravda.cs
    optimize: true
  - target: <C# program name>.exe
    sources:
    - Pravda.dll
    - dotnet-tests/resources/<C# program name>.cs
    optimize: true
---
```

The next command completes the test file with the expected parsing result.

```
sbt dotnet/test:runMain pravda.dotnet.ParserSuite --overwrite
```

### Translation

Create a file similarly to the one described above, but in this case the file extension should be `.trs` and the file should be created in the `dotnet/src/test/resources/translation` folder.

The next command completes the test file with the expected translation result.

```
sbt dotnet/test:runMain pravda.dotnet.TranslationSuite --overwrite
```

## Adding Tests to the Testkit

We need to add the runtime test to the respective folder in the [testkit](https://github.com/expload/pravda/tree/master/testkit/src/test/resources).

The test file should have the `.sbox` extension and contain the following minimum set of the preconditions:

```
stack:
  [utf8.name of method from test C# program]
storage:
  utf8.init: "null"
dotnet-compilation:
  steps:
  - target: Pravda.dll
    sources:
    - PravdaDotNet/Pravda/Pravda.cs
    optimize: true
  - target: <C# program name>.exe
    sources:
    - Pravda.dll
    - dotnet-tests/resources/<C# program name>.cs
    optimize: true
---
```

As you can see, it contains three preconditions:

- the method name from the test C# program, that should be put on the top of the stack
- the program should be initialized and to do this, we put the following key-value pair to the storage: key = "utf8.init", value = "null"
- the dotnet compilation steps


