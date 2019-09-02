# Working with Meta in IPFS

You can extract [meta](../virtual-machine/meta.md) from your file with Pravda bytecode and place it in the [IPFS](https://ipfs.io/). This can help you to significantly reduce the size of data you are writing to the blockchain and, accordingly, reduce your costs. Since meta doesn't affect the runtime behavior of your program, extracting it is safe and won't cause any problems even if the IPFS is unavailable.

### Deploying Pravda Program

Extracting of meta occurs when you deploy your Pravda program to the blockchain:

```bash
pravda broadcast deploy \
  -w <your-wallet> \
  -p <program-wallet> \
  -i <bytecode-of-your-program> \
  --meta-to-ipfs \
  --ipfs-node "/ip4/127.0.0.1/tcp/5001"
```

You should specify the `--meta-to-ipfs` parameter to enable meta extracting and `--ipfs-node` to select the necessary IPFS node (by default it uses localhost).

This command will extract _all_ meta from your bytecode and place it in the IPFS, then it will deploy the program _without_ meta on the Pravda blockchain.

You can also use `pravda broadcast update` to extract meta with the same CLI parameters.

### Reading Meta from IPFS

If you need to read meta from IPFS, for example, when disassembling the bytecode, you also use special CLI parameters:

```bash
pravda compile disasm \
    -i <bytecode-of-your-program> \
    --meta-from-ipfs
    --ipfs-node "/ip4/127.0.0.1/tcp/5001"
```

This command will read the IPFS hash of the file with meta from the bytecode, read this file from IPFS and disassemble the bytecode with this new information.

### IPFS Node

Hosting an open IPFS is insecure, therefore we suggest that you run your own IPFS node on your private host if you want to write meta to IPFS. If you just want to read meta from IPFS, you can use any open IPFS node (for example, ipfs.io).
