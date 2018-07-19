# Local client API

Expload local client provides access to the blockchain (e.g. run arbitrary code, call methods from existing programs). 

### Method calling

[Dotnet](dotnet.md) translation generates programs that has so-called _methods_. 
They are similar to methods from other smart-contract systems like [Solidity](http://solidity.readthedocs.io/en/v0.4.24/). 
Program with methods takes several (maybe zero) typed arguments and name of the method. 
For example if you want to call `balanceOf` method (that takes address of some user and returns current balance for that address)
you should provide `bytes` value for address and `string` "balanceOf" for method name, after that you will receive `uint32` value of balance.
_(See [data documentation](ref/vm/data.md) for description of typed data)_

#### REST API
Expload local client provides REST API for method calling on `localhost:8087/api/program/method` endpoint. 

It takes json in the following format:
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

| Type string (`"tpe"` field) | Format of `"value"` field | Data |
| --- | --- | --- |
| `"int8"` | JSON number | `int8` |
| `"int16"` | JSON number | `int16` |
| `"int32"` | JSON number | `int32` |
| `"uint8"` | JSON number | `uint8` |
| `"uint16"` | JSON number | `uint16` |
| `"uint32"` | JSON number | `uint32` |
| `"number"` | JSON number | `number` |
| `"boolean"` | JSON `true` of `false` | `boolean` |
| `"utf8"` | JSON string | `utf8` |
| `"bytes"` | hex JSON string | `bytes` |
| `"array <primitive type>"` | JSON array with elems of corresponding types | `array <primitive type>` |

For example if we are calling `balanceOf` method for user with `0xABCDEF` address of some program with `0x123456789ABCDE` address we should pass:
```json
{
  "address": "123456789ABCDE",
  "method": "balanceOf",
  "args": [ 
    {
      "value": "0xABCDEF",
      "tpe": "bytes"
    }
  ]
}
```

And Expload client will call the necessary method and will return:
```json
{
  "value": 1234,
  "tpe": "uint32"
}
```
