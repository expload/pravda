<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda broadcast seal --dry-run --wallet <file> --watt-payer-wallet <file> --limit <long> --price <long> --endpoint <string> --input <file> --address <string>```

## Description
Seal existing Pravda program in the blockchain.
## Options

|Option|Description|
|----|----|
|`--dry-run`|Broadcast action without applying effects.
|`-w`, `--wallet`|File with user wallet. You can obtain it using 'pravda gen address' command. Format: {"address": <public key>, "privateKey": <private key>}
|`--watt-payer-wallet`|File with watt payer wallet. Format same as for wallet.
|`-l`, `--limit`|Watt limit (300 by default).
|`-p`, `--price`|Watt price (1 by default).
|`-e`, `--endpoint`|Node endpoint (http://localhost:8080/api/public by default).
|`-i`, `--input`|Input file.
|`-a`, `--address`|Address of the program to seal
