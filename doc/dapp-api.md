# DApp API Specification

Pravda provides unified API for DApps (Distributed Applications). 
This API allows to run arbitrary code, call methods from existing programs and ... on the Pravda blockchain.

### Method calling

[Dotnet](dotnet.md) translation generates programs that has so-called _methods_. 
They are similar to methods from other smart-contract systems like [Solidity](http://solidity.readthedocs.io/en/v0.4.24/). 
Program with methods takes several (maybe zero) typed arguments and name of the method. 
For example if you want to call `balanceOf` method (that takes address of some user and returns current balance for that address)
you should provide `bytes` value for address and `string` "balanceOf" for method name, after that you will receive `uint32` value of balance.
_(See [data documentation](ref/vm/data.md) for description of typed data)_

#### REST API
DApp API specification establishes REST API for calling methods of program. 

It receives json in the following format:
```json
{
  "address": "<hex formatted string of program's address>",
  "method": "<name of the method>",
  "args": [ 
    {
      "value": <value of argument>,
      "tpe": "<type of argument>"
    }
  ]
}
```

And returns another json with computed value:
```json
{
  "value": <return value>,
  "tpe": "<type of return value>"    
}
```

Argument type is the regular string, format of the argument value depends on that type 
and each such type string corresponds to one [data](ref/vm/data.md) type. Details are given in the following table: 

| Type string (`"tpe"` field) | Example of `"value"` field | Data |
| --- | --- | --- |
| `"int8"` | `-123` | `int8` |
| `"int16"` | `23123` | `int16` |
| `"int32"` | `123123123`| `int32` |
| `"uint8"` | `123` | `uint8` |
| `"uint16"` | `23123` | `uint16` |
| `"uint32"` | `123123123` | `uint32` |
| `"number"` | `5.6` | `number` |
| `"boolean"` | `true` of `false` | `boolean` |
| `"utf8"` | `"string"` | `utf8` |
| `"bytes"` | `"6789ABCD"` | `bytes` |
| `"array <primitive type>"` | `[1, 2, 3]` | `array <primitive type>` |

For example if we are calling `balanceOf` method for user with `0xABCDEF` address of some program with `0x123456789ABCDE` address we should pass:
```json
{
  "address": "123456789ABCDE",
  "method": "balanceOf",
  "args": [ 
    {
      "value": "ABCDEF",
      "tpe": "bytes"
    }
  ]
}
```

And receive: 
```json
{
  "value": 1234,
  "tpe": "uint32"
}
```
