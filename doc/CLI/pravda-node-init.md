<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda node init --data-dir <file> --local --testnet --coin-distribution <string>```

## Description
Create a data directory and configuration for the new node.
## Options

|Option|Description|
|----|----|
|`-d`, `--data-dir`|
|`--local`|Initialize the local node with self-validation.
|`--testnet`|Initialize the node connected to the official testnet.
|`--coin-distribution`|Initialize the local node with the addresses that has a certain amount of coins at the initial state. JSON file. The format is as follows: [{"address":<public key in hex>,"amount":<number>}]
