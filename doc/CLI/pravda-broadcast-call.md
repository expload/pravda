<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda broadcast call --dry-run --wallet <file> --program-wallet <file> --watt-payer-wallet <file> --limit <long> --price <long> --endpoint <string> --meta-to-ipfs --ipfs-node <string> --address <string> --method <string> --args <sequence>```

## Description
Call the method of the program with arguments
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
|`--address`|Address of the program that will be called. For example, "xdc5056337b83726b881f241bf534ca04f7694452e0e879018872679cf8815af4" 
|`--method`|Method's name. For example, "Spend"
|`--args`|Method's arguments (comma separated). For example, "xdc5056337b83726b881f241bf534ca04f7694452e0e879018872679cf8815af4,20"
