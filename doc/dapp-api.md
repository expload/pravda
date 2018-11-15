# DApp API Specification

Pravda specifies unified API for DApps (Distributed Applications). 

This API allows client to execute different action on the Pravda blockchain,
such as run arbitrary code, call methods from existing programs,
get balance by address, sign binary data and transfer money from one address to another.


# General information

## Login

Before using API the user should be logged in app. User can sign up using its own public/private keys pair
or it can use automatically generated keys pair.

If the user has not logged the `NoKeysError` will be returned for any API call.

To run the app use `bin/expload-desktop` from https://download.expload.com/expload-desktop/. 

## API Endpoint

API server can be found at `http://localhost:8087`.

## Response format

The response should have one of the following status codes:

- 200 - if the request was valid and no errors have happened while the request processing
- 400 - if the request was invalid
- 500 - if the unhandled error was happened
- 503 - if the request has not processed for a 30 seconds

The response for any call of the API method should have the content-type header being set to `application/json`.

The response should be a JSON object with the following structure:

```
{
  "error"     : "error message",
  "errorCode" : "error code",
  "data"      : {}
}
```

If the `errorCode` field is empty then it means no errors have happened and the `data` field contains the result. Otherwise, the `errorCode` has a predefined error code and the `error` field contains error description.

For example, if the user has not logged into the app, the caller should receive a response like this:

```
{
  "error":     "NoKeysFound",
  "errorCode": "NoKeysError",
  "data":      {}
}
```

## Errors

Every API method may return error. If that happened the `errorCode` should not be empty.


Basic error codes are:

- `ActionNotConfirmedError` - means current user hasn't confirm the transaction
- `NoKeysError` - means there is no current user, so there is no keys for signing transactions
- `PravdaNodeHttpError` - means there is a connection problem with the Pravda Node.
- `UserErrorPravdaVmError` - means there was an error (like user-defined exception or system exception) in some Program
- `InsufficientFundsGameTokenProgramError` - means the user has not enough GameTokens

## JSON representation of the data

If you want to handle values in JSON representation then consider the following
[description of data representation in JSON format](https://github.com/expload/pravda/blob/master/doc/ref/vm/data.md#json-representation).


# API Methods

## Get current user address

You can get the _current user_ address. The _current user_ is the user who is able to sign a Pravda transaction by using its private key.

If there is no current user (for instance, if the user has not logged into app) an API implementation should return `NoKeysError` error.

### Request

`GET api/address`

### Response

Hex formatted address of the current user as JSON string.

### Example

```
curl <api url>/api/address
```

should return: 

```
{
  "error":"",
  "errorCode":"",
  "data":"hexadecimal representation of the user's address"
}
```

## Get balance by Pravda address

You can get balance of the Pravda user by its address. Blank address parameter indicates the current user`s address should be used.

### Request

`GET api/balance?[address=<hex formatted address>]`

### Response

Requsted user balance as integer value

### Examples

```
curl <api url>/api/address
```

will return balance of the current user. For instance, if the current user has 100 XCoins you will get
the following result:

```
{
  "error":"",
  "errorCode":"",
  "data":100
}
```

## Call program method

DApps API Specification introduces __method__ entity. This entity is similar to [Solidity](http://solidity.readthedocs.io/en/v0.4.24/) methods from Ethereum ecosystem.

DApp API specification establishes REST API for calling methods of the program with a given address.

An implementation of the Standard should ask the current user if they confirm this transaction or not.
An implementation of the Standard should show the user the program address, program's method and arguments for the method.
If this transaction is not confirmed, `ActionNotConfirmedError` error should be sent.
Otherwise it should be signed with the current user private key and broadcasted to the Pravda blockchain.

If there is no current user, an implementation of the Standard should return `NoKeysError` error.

### Request

`POST api/program/method`

```
{
    "address": "<hex formatted string of program's address>",
    "method": "<name of the method>",
    "args": [ <list of arguments> ]
}
```

Arguments should be properly encoded according to their [JSON representation](https://github.com/expload/pravda/blob/master/doc/ref/vm/data.md#json-representation).

### Response

```
{
  "error: "",
  "errorCode: "",
  "data" : {
    "finalState" : {
      "spentWatts" : <spent-watts>,
      "refundWatts" : <refunded-watts>,
      "totalWatts" : <total-watts>,
      "stack" : [<stack-data>],
      "heap" : [<heap-data>]
    },
    "effects" : [ {
      "eventType" : "<effect-type>",
      ...
      <event-dependent-fields>
      ...
    }, ...]
  }
}
```

### Examples

For example if we want to call `BalanceOf` method for user with `0xABCDEF` address
of some program with `0xe1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0` address we should pass:

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"address": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0", 
           "method": "BalanceOf", 
           "args": ["bytes.ABCDEF"] }'
  <api url>/api/program/method
```

As result we will receive the response like that:

```
{
  "error: "",
  "errorCode: "",
  "data" : {
    "finalState" : {
      "spentWatts" : <spent-watts>,
      "refundWatts" : <refunded-watts>,
      "totalWatts" : <total-watts>,
      "stack" : ["bigint.555"],
      "heap" : [<heap-data>]
    },
    "effects" : [ {
      "eventType" : "<effect-type>",
      ...
      <event-dependent-fields>
      ...
    }, ...]
  }
}
```

Balance is placed in `"data" \ "finalState" \ "stack"` field.


## Transfer money

This method allows you to transfer XCoins from current user to any Pravda address

### Request

`POST api/transfer`

```
{
    "to": "<hex formatted string of the receiver's address>",
    "amount": <amount of coins you are going to transfer>
}
```

### Examples

The following command will transfer 100 coins from the current account to the account
with address `e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0`

```
curl -X POST -H "Content-Type: application/json" --data '{ "to": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0", "amount": 100 }' <api url>/api/transfer
```


## Execute VM bytecode

This method allows you to execute arbitrary Pravda bytecode

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
  "finalState" : {
    "spentWatts" : <spent-watts>,
    "refundWatts" : <refunded-watts>,
    "totalWatts" : <total-watts>,
    "stack" : [<stack-data>],
    "heap" : [<heap-data>]
  },
  "effects" : [ {
    "eventType" : "<effect-type>",
    ...
    <event-dependent-fields>
    ...
  }, ...]
}
```
or with error

```
{
  "executionResult" : {
    "error" : {
      "error" : <error-code-or-string>,
      "finalState" : {
        "spentWatts" : <spent-watts>,
        "refundWatts" : <refunded-watts>,
        "totalWatts" : <total-watts>,
        "stack" : [<stack-data>],
        "heap" : [<heap-data>]
      },
      "callStack" : [<call-stack-data>],
      "lastPosition" : <last-position>
    }
  },
  "effects" : [ {
    "eventType" : "<effect-type>",
    ...
    <event-dependent-fields>
    ...
  }, ...]
}
```

## Sign binary data

This method allows you to sign arbitrary binary data

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