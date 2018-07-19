# DApp API Specification

Pravda specifies unified API for DApps (Distributed Applications). 

This API allows client to execute different action on the Pravda blockchain, such as run arbitrary code, call methods from existing programs, get balance by address, sign binary data and transfer money from one address to another.


# API Methods

## Get current user address

You can get the _current user_ addres. The _current user_ is the user who signs a Pravda transaction by its private key, that is an executor of the transaction.

If there is no current user an API implemenation should return `NoKeys` error.


### Request

`GET api/address`

### Response

Hex formatted address of the current user as JSON string.

### Example

```
curl <api url>/api/address
```

might return `ebf452afd33987dd33dq` if the user with such address is the current user

## Get balance by Pravda address
You can get balance of the Pravda user by his address. Absent address parameter means the current user.

### Request

`GET api/balance?[address=<hex formatted address>]`

### Response

Requsted user balance as integer value


### Examples
```
curl <api url>/api/address
```
will return balance of the current user

```
curl <api url>/api/balance?address=aaeeff0abeffa0ffeea0568dfe
``` 

will return balance of the user with address `aaeeff0abeffa0ffeea0568dfe`


## Call program method

DApps API Specification introduces __method__ entity. This entity is similar to [Solidity](http://solidity.readthedocs.io/en/v0.4.24/) methods from Ethereum ecosystem.

DApp API specification establishes REST API for calling methods of the program with a given address.

An implementaion of the Standard should ask the current user if he confirms this transaction or not. An implementaion of the Standard should show the user the programm addres, program method and arguments to be executed.  If this transaction is not confirmed, `NotConfirmed` error should be sent. On the other hand, if the transaction is confirmed, it should be signed with the current user private key and boradcasted to the Pravda blockchain.

If there is no current user, an implemenation of the Standard should return `NoKeys` error.

### Request

`POST api/program/method`

```
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

### Response

```
{
    "value": <return value>,
    "tpe": "<type of return value>"    
}
```

### Additional information

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

### Examples

For example if we are calling `balanceOf` method for user with `0xABCDEF` address of some program with `0x123456789ABCDE` address we should pass:

```
curl -X POST -H "Content-Type: application/json" --data '{"address": "123456789ABCDE", "method": "balanceOf", "args": [{"tpe": "bytes", "value": "ABCDEF"}] }'  <api url>/api/program/method
```

And receive: 
```
{
    "value": 1234,
    "tpe": "uint32"
}
```


## Transfer money
This method allows you to transfer money from current user to any Pravda address

An implementaion of the Standard should ask the current user if he confirms this transfer or not. It should show the amount and the address of the transferring. If this transaction is not confirmed, `NotConfirmed` error should be sent. On the other hand, if the transaction is confirmed, it should be signed with the current user private key and boradcasted to the Pravda blockchain.

If there is no current user, an implemenation of the Standard should return `NoKeys` error.


### Request

`POST api/transfer`

```
{
    "to": "<hex formatted string of the receiver address>",
    "amount": <amount of coins you are going to transer>
}
```

### Examples

The following command will transfer 100 coins from the current account to the account with address `aaeeff0abeffa0ffeea0568dfe`
```
curl -X POST -H "Content-Type: application/json" --data '{ "to": "aaeeff0abeffa0ffeea0568dfe", "amount": 100 }' <api url>/api/transfer
```


## Execute VM bytecode
This method allows you to execute any Pravda bytecode

An implementaion of the Standard should ask the current user if he confirms this execution or not. It should show the bytecode translated to the Pravda assembler. If this transaction is not confirmed, `NotConfirmed` error should be sent. On the other hand, if the transaction is confirmed, it should be signed with the current user private key and boradcasted to the Pravda blockchain.

If there is no current user, an implemenation of the Standard should return `NoKeys` error.


### Request

`POST api/execute`

```
{
    "transaction": "<hex formatted transaction>",
    "wattLimit": <watt limit> // Optional
}
```

### Response
```
{
    "error" : "<error type>", // Optional
    "totalWatts" : <spent watts>,
    "stack" : [<stack data>],
    "heap" : [<heap data>]
}
```

## Sign binary data
This method allows you to execute any Pravda bytecode

An implementaion of the Standard should ask the current user if he confirms this signing or not. It should be possible to see the data. If the currenit user doesn't allow to sign the data, `NotConfirmed` error should be sent. On the other hand, if the current user allows to sign the data, the data should be signed with the current user private key and returned back.

If there is no current user, an implemenation of the Standard should return `NoKeys` error.



### Request

`POST api/sign`

```
{
  "app": "<application name>",
  "bytes": "<hex formatted binary data>"
}
```

### Response
```
{
  "signedBytes": "<hex formatted signed binary data>"
}
```

### Additional information 
You can also sign binary data with `application/octet-stream` or `application/base64` http content types by using `api/binary/sign?app=<application name>` url with corresponding type.


# Errors

Every API method may return error.

```
{
  "error": "<error type>"
}
```

Basic error types are:
- NotConfirmed - means current user did not confirm the transaction
- NoKeys  - means there is no current user, so there is no keys for signing transactions

