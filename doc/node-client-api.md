# Node Client API Service

Service provides REST API to sent transactions to the network.

### Install
Requires environment variables:

* `PRAVDA_BROADCAST_ENPOINT` - URL to broadcast transactions.
* `PRAVDA_BROADCAST_PK` - public key of signer
* `PRAVDA_BROADCAST_SK` - private key of signer

Run on docker:

`docker run expload/pravda-node-client-api`

### Request

```
curl --request POST \
  --url '<api url>/broadcast?wattLimit=<integer>&wattPrice=<integer>' \
  --header 'content-type: <text/x-asm or plain/text or application/octet-stream' \
  --data '<assembler or compiled bytecode>'
  ```
### Response

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