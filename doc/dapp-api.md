# DApp API Specification

Pravda specifies unified API for DApps (Distributed Applications). 

This API allows client to execute different action on the Pravda blockchain, such as run arbitrary code, call methods from existing programs, get balance by address, sign binary data and transfer money from one address to another.

## Api Methods

### Get current user address
You can get the current user addres. The __current user__ is the user who signs a Pravda transaction by its private key, that is an executor of the transaction.

**HTTP URL**: `api/address`
**HTTP METHOD**: `GET`

**OUTPUT**: 
```
<hex formatted address of the current user>
```


**EXAMPLES**:
`api/address` might return `ebf452afd33987dd33dq` if the user with such address is the curren user

### Get balance by Pravda address
You can get balance of the Pravda user by his address. Absent address parameter means the current user.

**HTTP URL**: `api/balance?[address=<hex formatted address>]`
**HTTP METHOD**: `GET`

**OUTPUT**: 
```
<balance>
```


**EXAMPLES**:
`api/balance` will return balance of the current user
`api/balance?address=aaeeff0abeffa0ffeea0568dfe` will return balance of the user with address `aaeeff0abeffa0ffeea0568dfe`


### Call program method

DApps API Specification introduces __method__ entity. This entity is similar to [Solidity](http://solidity.readthedocs.io/en/v0.4.24/) methods from Ethereum ecosystem.

DApp API specification establishes REST API for calling methods of the program with a given address. 


**HTTP URL**: `api/program/method`
**HTTP METHOD**: `POST`

**INPUT**: 
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

**OUTPUT**:
```json
{
  "value": <return value>,
  "tpe": "<type of return value>"    
}
```

**ADDITIONAL INFORMATION**:
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

**EXAMPLES**
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


### Transfer money
This method allows you to transfer money from current user to any Pravda address


**HTTP URL**: `api/transfer`
**HTTP METHOD**: `POST`

**INPUT**: 
```json
{
  "to": "<hex formatted string of the receiver address>",
  "amount": <amount of coins you are going to transer>
}
```

**EXAMPLES**:

The following command will transfer 100 coins from the current account to the account with address `aaeeff0abeffa0ffeea0568dfe`
```json
{
  "to": "aaeeff0abeffa0ffeea0568dfe",
  "amount": 100
}
```


### Execute VM bytecode
This method allows you to execute any Pravda bytecode


**HTTP URL**: `api/execute`
**HTTP METHOD**: `POST`

**INPUT**: 
```json
{
  "transaction": "<hex formatted transaction>",
  "wattLimit": <watt limit> // Optional
}
```

**OUTPUT**
# TODO
```json
{
"error" : "<error type>", // Optional
"totalWatts" : <spent watts>,
"stack" : [<stack data>],
"heap" : [<heap data>]
}
```

### Sign binary data
This method allows you to execute any Pravda bytecode


**HTTP URL**: `api/sign`
**HTTP METHOD**: `POST`

**INPUT**: 
```json
{
  "app": "<application name>",
  "bytes": "<hex formatted binary data>"
}
```

**OUTPUT**
```json
{
  "signedBytes": "<hex formatted signed binary data>"
}
```

**ADDITIONAL INFORMATION**
You can also sign binary data with `application/octet-stream` or `application/base64` http content types by using `api/binary/sign?app=<application name>` url with corresponding type.


## Errors

Every API method may return error.

```json
{
  "error": "<error type>"
}
```

Basic error types are:
**NotConfirmed** - means current user did not confirm the transaction
**NoKeys** - means there is no current user, so there is no keys for signing transactions

