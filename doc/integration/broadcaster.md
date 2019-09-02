# Broadcaster

The service provides REST API to send transactions to the network. The dedicated service is required because Pravda Node API takes the already compiled bytecode with signature. This means that you cannot send a transaction without knowing the transaction’s binary format. This service can take Pravda Assembler, compile it, sign with the given `PRAVDA_BROADCAST_SK` and send to the `PRAVDA_BROADCAST_ENPOINT` endpoint. It allows transactions to be sent from any programing language that supports HTTP.

### Install
Requires environment variables:

* `PRAVDA_BROADCAST_ENPOINT` — URL to broadcast transactions
* `PRAVDA_BROADCAST_PK` —  the signer’s public key
* `PRAVDA_BROADCAST_SK` —  the signer’s private key

Run on the docker:

```
docker run \
  -p 127.0.0.1:5000:5000 \
  -e PRAVDA_BROADCAST_PK=<sender public key> \
  -e PRAVDA_BROADCAST_SK=<sender private key> \
  -e PRAVDA_BROADCAST_ENDPOINT=https://publicnode.expload.com/api/public/broadcast \
  expload/pravda-broadcaster
```


## API

### Broadcast

#### Request

```
curl --request POST \
  --url '<api url>/broadcast?wattLimit=<integer>&wattPrice=<integer>' \
  --header 'content-type: <text/x-asm or plain/text or application/octet-stream' \
  --data '<assembler or compiled bytecode>'
```

### Call the Program Method

#### Request

An example of the full request string is provided below:

```
curl --request POST \
  --url '<api url>/broadcast-call?address=<address>&method=<method>&arg=<arg1>&arg=<arg2>&arg=<argN>&wattLimit=<integer>&wattPrice=<integer>'
```

The repeated parameter `arg` is optional and can be omitted when the method doesn't have arguments. For example,

```
curl --request POST \
     --url 'localhost:5000/broadcast-call?address=xdc5056337b83726b881f241bf534ca04f7694452e0e879018872679cf8815af4&method=MyBalance&wattLimit=1000&wattPrice=1'
```

The parameters `wattLimit`, `wattPrice` are optional and have the following default values:

- wattLimit: 100000
- wattPrice: 1

## API Response

```
{
  "executionResult" : {
    "success" : {
      "spentWatts" : <spent-watts>,
      "refundWatts" : <refunded-watts>,
      "totalWatts" : <total-watts>,
      "stack" : [<stack-data>],
      "heap" : [<heap-data>]
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

or with an error

```
{
  "executionResult" : {
    "failure" : {
      "error" : {
        "code": <error-code>,
        "message": <error-message>,
      },
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

