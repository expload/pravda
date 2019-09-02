# Code Generation for Unity3D

The Pravda project is able to generate an auxiliary code for [Unity](https://unity3d.com/) that provides a convenient way to call _program's_ methods.
It uses [`meta`](../virtual-machine/meta.md) information from the given bytecode to detect and analyse methods of the _program_.

## Dependecies
**Important note:**
The generated code uses [Json .NET](https://www.newtonsoft.com/json) library for hadling Json.
You can download its Unity version from [AssetStore](https://assetstore.unity.com/packages/tools/input-management/json-net-for-unity-11347).

## How to Generate Code

```
pravda gen unity --input input.pravda
```

This command will create an `Assets` folder (if it doesn't exist) and will place several files in it. You should include these files to your Unity project.

## How to Use the Generated Code

The generated code uses [coroutinues](https://docs.unity3d.com/ScriptReference/Coroutine.html) to handle [DApp API](dapp-api.md) requests.

For example: if you have the `Increment` method that takes an integer and increments it in your program, the generated code will include the `IncrementRequest` class
that handles a DApp API request for the calling of the `Increment` method.
```c#
var req = new IncrementRequest(address); // address of program in the blockchain as a byte array
yield return req.Increment(42);
ProcessResult(req);
```

Internally, the `IncrementRequest` class will be represented as follows:
```c#
public class IncrementRequest: ProgramRequest<int> // int is the type of result
{
    public IncrementRequest(byte[] programAddress) : base(programAddress) { } // address of deployed program in the blockchain

    protected override int ParseResult(string elem) // method that parses Pravda specific format to result value
    {
        return ExploadTypeConverters.ParseInt32(elem); // parse int32 see (data specification)[ref/vm/data.md]
    }

    public IEnumerator Increment(int arg0)
    {
        yield return SendRequest( // send http request to DApp API
            "Increment",
            new string[] { ExploadTypeConverters.PrintInt32(arg0) } // print int32 see (data specification)[ref/vm/data.md]
        );
    }
}
```

## Expload.Unity.Codegen

Internally, the generated code uses the auxiliary functionality from the namespace called `Expload.Unity.Codegen`.
The source file of this namespace is located [here](../../codegen/src/main/resources/ExploadUnityCodegen.cs).


