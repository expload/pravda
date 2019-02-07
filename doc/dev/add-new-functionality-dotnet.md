# Adding new functionality to the DotNet translator

The process consists of the following steps:

1) Add stub method in [Pravda.cs](https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda.cs) without implementing any functionality
2) Describe translation of the method in [CallsTranslation](https://github.com/expload/pravda/blob/master/dotnet/src/main/scala/pravda/dotnet/translation/opcode/CallsTranslation.scala)
3) Write test C# program that uses that method. Place appropriate tests to [parser](https://github.com/expload/pravda/tree/master/dotnet/src/test/resources/parser), [translation](https://github.com/expload/pravda/tree/master/dotnet/src/test/resources/translation) and [teskit](https://github.com/expload/pravda/tree/master/testkit/src/test/resources) folders.

## Stub method into Pravda.cs

For example we want to add static method `ProgramAddress` to the `Info` class. In order to do it we can define stub method as following:

```
public static Bytes ProgramAddress() { return null; }
```

Returning value doesn't matter. You can return the default value for the returing type.

## Method translation in CallsTranslation

### Changing the stack during method execution

We need to tell the translator how the stack is changed during method execution. In order to do it we should add pattern-match case in the `deltaOffsetOne` function similarly how it is done for other methods.

For example `ProgramAddress()` method takes no parameters, but returns one value. It means, that height of the stack is changed by +1. If the method takes two parameters and returns one value, then height of the stack is changed by -1. 

### Translation of the method to the Pravda VM opcodes

We need to add pattern-match for `Call` case with our method (similarly how it's done for other methods from [`Pravda.cs`](https://github.com/expload/pravda/blob/master/PravdaDotNet/Pravda.cs)) in the function `asmOpsOne`. There we need to implement the method using Pravda VM opcodes.

For example for `ProgramAddress()` method we have `PADDR` opcode, so we can write the following:

```
case Call(MemberRefData(TypeRefData(_, "Info", "Expload.Pravda"), "ProgramAddress", _)) =>
  Right(List(Operation(Opcodes.PADDR)))
```

## Parser and translation tests

At first, we need to add test C# program to `dotnet-tests/resources/` folder.

### Parser

We need to create a file with `.prs` extension in `dotnet/src/test/resources/parser`. File name can be chosen arbitrary.

The file should contain `preconditions` which describe dotnet compilation steps:

```
dotnet-compilation:
  steps:
  - target: Pravda.dll
    sources:
    - PravdaDotNet/Pravda.cs
    optimize: true
  - target: <C# program name>.exe
    sources:
    - Pravda.dll
    - dotnet-tests/resources/<C# program name>.cs
    optimize: true
---
```

The next command completes the test file with expected parsing result.

```
sbt dotnet/test:runMain pravda.dotnet.ParserSuite --overwrite
```

### Translation

Create a file similarly to the described one above, but now file extension should be `.trs` and the file should be created in the `dotnet/src/test/resources/translation` folder.

The next command completes the test file with expected translation result.

```
sbt dotnet/test:runMain pravda.dotnet.TranslationSuite --overwrite
```

## Adding tests to the testkit

We need to add runtime test to the appropriate folder in the [testkit](https://github.com/expload/pravda/tree/master/testkit/src/test/resources).

The test file should have `.sbox` extension and contain the following minimum set of the preconditions:

```
stack:
  [utf8.name of method from test C# program]
storage:
  utf8.init: "null"
dotnet-compilation:
  steps:
  - target: Pravda.dll
    sources:
    - PravdaDotNet/Pravda.cs
    optimize: true
  - target: <C# program name>.exe
    sources:
    - Pravda.dll
    - dotnet-tests/resources/<C# program name>.cs
    optimize: true
---
```

As you can see it contains three preconditions:

- method name from the test C# program, that should be put on the top of the stack
- program should be initialized, so we put the following key-value pair to the storage: key = "utf8.init", value = "null"
- dotnet compilation steps
