# DApp API Specification

Pravda features a unified API for DApps (distributed applications).

This API allows the client to execute different actions on the Pravda blockchain such as running the arbitrary code, calling methods from the existing programs, requesting the balance by address, signing binary data and transferring funds from one address to another.

## Login

Before using the API, the user should log in the application. The user can sign using their own pair of public/private keys or a pair of automatically generated keys.

If the user has not logged in, a `NoKeysError` will be returned for an API call.

To run the application, use `bin/expload-desktop` from https://download.expload.com/expload-desktop/.

## API Endpoint

The API server can be found at `http://localhost:8087`.

## Response Format

The response should contain one of the following status codes:

- 200 — a valid request without errors occurred while processing the request
- 400 — an invalid request
- 500 — an unhandled error
- 503 — a request that has not been processed within 30 seconds

The response for any call of the API method should have a content-type header being set to `application/json`.

The response should be a JSON object with the following structure:

```
{
  "error"     : "error message",
  "errorCode" : "error code",
  "data"      : {}
}
```

If the `errorCode` field is empty, this means that no errors have occurred and the `data` field contains the result. Otherwise, the `errorCode` will have a predefined error code and the `error` field will contain error description.

For example, if the user has not logged into the app, the caller should receive a response like this:

```
{
  "error":     "NoKeysFound",
  "errorCode": "NoKeysError",
  "data":      {}
}
```

## Errors

Any API method may return an error. If this is the case, the `errorCode` should not be empty.


The basic error codes include:

- `ActionNotConfirmedError` — the current user hasn't confirmed the transaction
- `NoKeysError` — there is no current user, therefore there are no keys for the signing of transactions
- `PravdaNodeHttpError` — there is a connection problem with the Pravda Node.
- `UserErrorPravdaVmError` — an error has occurred (such as a user-defined exception or a system exception) in a Program
- `InsufficientFundsGameTokenProgramError` — the user’s funds are insufficient

## JSON Representation of Data

If you want to handle values in JSON representation, you should consider the following
[description of data representation in JSON format](https://github.com/expload/pravda/blob/master/doc/ref/vm/data.md#json-representation).


## Request Сurrent User Address

You can get the _current user_ address. The _current user_ is the user who is able to sign Pravda transactions by using their private key.

If there is no current user (for instance, if the user has not logged in to the application) the API implementation should return a `NoKeysError` error.

### Request

`GET api/address`

### Response

A hex formatted address of the current user as a JSON string.

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

## Request Balance by Pravda Address

You can get the balance of the Pravda user by their address. A blank address parameter indicates that the current user`s address should be used.

### Request

`GET api/balance?[address=<hex formatted address>]`

### Response

The requested user balance as an integer value

### Examples

```
curl <api url>/api/balance
```

will return the current user’s balance. For instance, if the current user has 100 XGold you will get the following result:

```
{
  "error":"",
  "errorCode":"",
  "data":100
}
```

## Request Username by Pravda Address

**Note:** This is a specific feature for the Expload Platform.

You can get the username (nickname) of the Pravda user by their address.

### Request

`GET api/username?[address=<hex formatted address>]`

### Response

The username as a string value

### Examples

```
curl <api url>/api/username
```

will return the username, for example:

```
{
  "error":"",
  "errorCode":"",
  "data":"nickname"
}
```

## Request the Current User’s XGold Balance

**Note** This is a specific feature for the Expload Platform.

You can get the actual XGold balance of the current logged-in user.

### Request

`GET api/balance/xgold`

### Response

The balance in miniXG (1 XGold = 100000000 miniXG) expressed in numeric values as a long type.

### Examples

```
curl <api url>/api/balance/xgold
```

will return the balance, for example:

```
{
  "error":"",
  "errorCode":"",
  "data": 100000000
}
```

## Call Program Methods

The DApps API Specification introduces a __method__ entity. This entity is similar to the [Solidity](http://solidity.readthedocs.io/en/v0.4.24/) methods from the Ethereum ecosystem.

The DApp API specification establishes REST API for the calling of program methods with a given address.

The implementation of the Standard should ask the current user if they confirm this transaction or not.
The implementation of the Standard should show the user the program address, program method and arguments for the method.
If the transaction is not confirmed, an `ActionNotConfirmedError` error should be sent.
Otherwise, it should be signed with the current user private key and broadcasted to the Pravda blockchain.

If there is no current user, the implementation of the Standard should return a `NoKeysError` error.

### Request

`POST api/program/method`

```
{
    "address": "<hex formatted string of program's address>",
    "method": "<name of the method>",
    "args": [ <list of arguments> ]
}
```

The arguments should be properly encoded according to their [JSON representation](https://github.com/expload/pravda/blob/master/doc/ref/vm/data.md#json-representation).

### Response

```
{
  "error: "",
  "errorCode: "",
  "data" : {
    "transactionId": "<transaction-id>",
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

For example, if you want to call the `BalanceOf` method for the user with `0xABCDEF` address of a certain program with the `0xe1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0` address you should pass:

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"address": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0",
           "method": "BalanceOf",
           "args": ["bytes.ABCDEF"] }'
  <api url>/api/program/method
```

As a result, you will receive a response like that:

```
{
  "error: "",
  "errorCode: "",
  "data" : {
    "transactionId": "<transaction-id>",
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

The balance is placed in the `"data" \ "finalState" \ "stack"` field.

### Test/Read Requests

Also, you can call a method without producing effects on the blockchain. This may be useful when you want to request certain data from the program’s storage.

`POST api/program/method-test`

The subsequent part is the same.

## Funds Transfer

This method allows you to transfer XGold from the current user to any Pravda address.

### Request

`POST api/transfer`

```
{
    "to": "<hex formatted string of the receiver's address>",
    "amount": <amount of coins you are going to transfer>
}
```

### Examples

The following command will cause 100XGold to be transferred from the current account to the account with the address `e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0`

```
curl -X POST -H "Content-Type: application/json" --data '{ "to": "e1941077e00b3cf81a8275788334292d9b2e2f0002bd622444cb37fa5e4d08a0", "amount": 100 }' <api url>/api/transfer
```


## Execute VM Bytecode

This method allows you to execute arbitrary Pravda bytecode.

### Request

`POST api/execute`

```
{
    "program": "<hex formatted transaction>",
    "wattLimit": <watt limit> // Optional
}
```

### Response

```
{
  "error: "",
  "errorCode: "",
  "data" : {
    "transactionId": "<transaction-id>",
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
or with an error

```
{
  "error: "<error>",
  "errorCode: "<error-code>",
  "data" : {
    ...
  }
}
```

## Sign Binary Data

This method allows you to sign arbitrary binary data.

### Request

`POST api/sign`

```
{
  "app": "<name of your application>",
  "bytes": "<hex formatted binary data>"
}
```

### Response
```
{
  "error": "",
  "errorCode": "",
  "data": {
    "signedData": "<hex formatted signed binary data>"
  }
}
```

## Check User Authentication

This method allows you to check if the current user has access to their private key.

The caller can send certain arbitrary data and when the result (signature-like footprint.) returns, the caller can verify it.

The result (the value of the `signedData` key) is determined as follows:

- Input binary data is preceded by the special constant string `EXPLOAD_NOT_AUTHORIZED_SIGNATURE`
- The hash of the above data is calculated by using either `sha-256` or `ripemd-160` depending on the input `hash` parameter
- The hash above **is signed with the user's private key**.
- Resulting binary data is encoded to a hex-string

**Note:**. The calling of this method does not involve interaction with any user.

### Request

`POST api/auth[?hash=sha-256|ripemd-160]`

```
{
  "app": "<name of your dapp>",
  "bytes": "<hex formatted binary data>"
}
```

If the `hash` query parameter is omitted, the `sha-256` hash function will be used by default.

### Response

```
{
  "error": "",
  "errorCode": "",
  "data": {
    "signedData": "<hex formatted data>"
  }
}
```


