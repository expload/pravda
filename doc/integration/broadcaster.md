# Broadcaster

The service provides REST API to send transactions to the network. Dedicated service is required because Pravda Node API takes already compiled bytecode with signature. It means you can't send transaction without knowledge of transaction binary format. This service can take Pravda Assembler, compile it, sign with given `PRAVDA_BROADCAST_SK` and send to `PRAVDA_BROADCAST_ENPOINT` endpoint. It allows to send transactions from any programing language which supports HTTP.  

### Install
Requires environment variables:

* `PRAVDA_BROADCAST_ENPOINT` - URL to broadcast transactions.
* `PRAVDA_BROADCAST_PK` - public key of signer
* `PRAVDA_BROADCAST_SK` - private key of signer

Run on docker:

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

### Call the method of the program

#### Request

The example of the full request string is provided below:

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

or with error

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