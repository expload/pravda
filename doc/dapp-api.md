# DApp API Specification

Pravda specifies unified API for DApps (Distributed Applications). 

This API allows client to execute different action on the Pravda blockchain,
such as run arbitrary code, call methods from existing programs,
get balance by address, sign binary data and transfer money from one address to another.


# General information

## Login

Before using API, the user should be logged in app. User can sign up using its own public/private keys pair
or it can use automatically generated keys pair.

If the user has not logged, the `NoKeysError` will be returned for any API call.

To run the API, use `bin/expload-desktop` from http://download.expload.com/expload-desktop/. 

## API Endpoint

API server can be found at this address: `http://localhost:8087`.

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

If the `errorCode` field is empty, then it means no errors have happened and the `data` field contains the result. Otherwise, the `errorCode` has a predefined error code and the `error` field high likely contains error description.

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

- `ActionNotConfirmedError` - means current user did not confirm the transaction
- `NoKeysError` - means there is no current user, so there is no keys for signing transactions
- `PravdaNodeHttpError` - means there is a connection problem with the Pravda Node.
- `UserErrorPravdaVmError` - means there was an error (like user-defined exception or system exception) in some Program happened
- `InsufficientFundsGameTokenProgramError` - means the user has not enough GameTokens

## JSON representation of the primitive data

If you want to pass or receive values in JSON representation then you should follow
[this description](https://github.com/expload/pravda/blob/master/doc/ref/vm/data.md#json-representation) of how to do it.


# API Methods

## Get current user address

You can get the _current user_ address. The _current user_ is the user who able to sign a Pravda transaction by using its private key.

If there is no current user (for instance, if the user has not logged into app) an API implemenation should return `NoKeysError` error.

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

You can get balance of the Pravda user by its address. Absent address parameter means the current user.

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

The same is equal for an arbitrary address.

## Call program method

DApps API Specification introduces __method__ entity. This entity is similar to [Solidity](http://solidity.readthedocs.io/en/v0.4.24/) methods from Ethereum ecosystem.

DApp API specification establishes REST API for calling methods of the program with a given address.

An implementaion of the Standard should ask the current user if he confirms this transaction or not.
An implementaion of the Standard should show the user the programm address, program's method and arguments to be executed.
If this transaction is not confirmed, `ActionNotConfirmedError` error should be sent.
Otherwise it should be signed with the current user private key and broadcasted to the Pravda blockchain.

If there is no current user, an implemenation of the Standard should return `NoKeysError` error.

### Request

`POST api/program/method`

```
{
    "address": "<hex formatted string of program's address>",
    "method": "<name of the method>",
    "args": [ <array of arguments> ]
}
```

Arguments should have properly encoded according to their JSON representation (see corresponding section of that document).

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

For example if we want to call `balanceOf` method for user with `0xABCDEF` address
of some program with `0xe1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0` address we should pass:

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"address": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0", 
           "method": "balanceOf", 
           "args": ["bytes.ABCDEF"] }'
  <api url>/api/program/method
```

As result we might receive the response like that:

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

Balance can be found in "data \ finalState \ stack" field.


## Transfer money

This method allows you to transfer money from current user to any Pravda address

### Request

`POST api/transfer`

```
{
    "to": "<hex formatted string of the receiver address>",
    "amount": <amount of coins you are going to transer>
}
```

### Examples

The following command will transfer 100 coins from the current account to the account
with address `e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0`

```
curl -X POST -H "Content-Type: application/json" --data '{ "to": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0", "amount": 100 }' <api url>/api/transfer
```


## Execute VM bytecode

This method allows you to execute any Pravda bytecode

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

This method allows you to execute any Pravda bytecode

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

You can also sign binary data with `application/octet-stream` or `application/base64`
http content types by using `api/binary/sign?app=<application name>` url with corresponding type.
