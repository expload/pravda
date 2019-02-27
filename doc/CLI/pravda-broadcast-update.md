<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda broadcast update --dry-run --wallet <file> --program-wallet <file> --watt-payer-wallet <file> --limit <long> --price <long> --endpoint <string> --meta-to-ipfs --ipfs-node <string> --input <file>```

## Description
Update existing Pravda program in the blockchain.
## Options

|Option|Description|
|----|----|
|`--dry-run`|Broadcast action without applying effects.
|`-w`, `--wallet`|File with user wallet. You can obtain it using 'pravda gen address' command. Format: {"address": <public key>, "privateKey": <private key>}
|`-p`, `--program-wallet`|Wallet of program account
|`--watt-payer-wallet`|File with watt payer wallet. Format same as for wallet.
|`-l`, `--limit`|Watt limit (300 by default).
|`-P`, `--price`|Watt price (1 by default).
|`-e`, `--endpoint`|Node endpoint (http://localhost:8080/api/public by default).
|`--meta-to-ipfs`|Save all metadata to IPFS. To configure the IPFS node address use "--ipfs-node" parameter.
|`--ipfs-node`|Ipfs node (/ip4/127.0.0.1/tcp/5001 by default).
|`-i`, `--input`|Input file.
