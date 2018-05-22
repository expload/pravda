`pravda-node` is a consensus engine based on Tendermint. It is also a CLI to run
single node.

pravda-node uses Tendermint to handle low level blockchain operations and P2P
networking. It depends on our implementation of Tendermint's ABCI server.

[Scala ABCI Server](https://github.com/mytimecoin/scala-abci-server) is a
generic gateway (or proxy) for Tendermint written on Scala that accepts
blockchain messages and passes them through to a business layer.

```
Usage:

pravda-node

    Start pravda node.

    Environment variables (override values in default config)

    TC_CONFIG_FILE
        Path to configuration file.

    TC_P2P_PORT
        Port to listen on for incoming peer connections. Passed to
        tendermint core.

    TC_RPC_PORT
        Port to listen on for incoming RPC connections. Passed to
        tendermint core.

    TC_ABCI_PORT
        Port to listen on for connections from tendermint core. This port is
        used by ABCI server.

    TC_ABCI_SOCK
        The same as TC_ABCI_PORT but uses unix domain socket. Only supported
        on Linux.

    TC_API_HOST
        API host.

    TC_API_PORT
        API port. There is a block explorer GUI listening on
        http://$TC_API_HOST:$TC_API_PORT/ui .

    TC_IS_VALIDATOR
        Boolean flag. If this node is a validator.

    TC_DISTRIBUTION
        Boolean flag. If this node used in initial distribution.

    TC_DATA
        Data directory path. Pravda keeps storage and configs in it.

    TC_SEEDS
        P2P seeds delimited by comma in format host:port.

    TC_GENESIS_TIME
        Genesis block date time, e.g.: 0001-01-01T00:00:00Z

    TC_GENESIS_CHAIN_ID
        Any printable ID for blockchain, e.g.: demonet

    TC_PAYMENT_WALLET_ADDRESS
        Node wallet address (public key) in hex.

    TC_PAYMENT_WALLET_PRIVATE_KEY
        Node wallet secret key in hex.

    TC_GENESIS_VALIDATORS
        Blockchain block validators in name:fee:wallet-address format
```
