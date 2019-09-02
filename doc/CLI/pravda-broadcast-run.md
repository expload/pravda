<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda broadcast run --dry-run --wallet <file> --program-wallet <file> --watt-payer-wallet <file> --limit <long> --price <long> --endpoint <string> --meta-to-ipfs --ipfs-node <string> --input <file>```

## Description
Send a transaction with Pravda Program address to the blockchain to run it
## Options

|Option|Description|
|----|----|
|`--dry-run`|Broadcast an action without applying effects.
|`-w`, `--wallet`|A file with a user wallet. You can obtain it using the 'pravda gen address' command. The format is as follows: {"address": <public key>, "privateKey": <private key>}
|`-p`, `--program-wallet`|A wallet of the program account
|`--watt-payer-wallet`|A file with a watt payer wallet. The format is the same as for the wallet.
|`-l`, `--limit`|The watt limit (300 by default).
|`-P`, `--price`|The watt price (1 by default).
|`-e`, `--endpoint`|The node endpoint (http://localhost:8080/api/public by default).
|`--meta-to-ipfs`|Save all metadata to IPFS. To configure the IPFS node address use "--ipfs-node" parameter.
|`--ipfs-node`|The ipfs node (/ip4/127.0.0.1/tcp/5001 by default).
|`-i`, `--input`|Input file.
