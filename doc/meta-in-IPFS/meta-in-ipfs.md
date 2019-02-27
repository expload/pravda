# Working with meta in IPFS

You can extract [meta](../virtual-machine/meta.md) 
from your file with Pravda bytecode and place it in the [IPFS](https://ipfs.io/).
It can help to dramatically reduce size of data you are writing to the blockchain 
and thus save your money. 
Since meta doesn't affect the runtime behaviour of your program, 
extracting it is safe and won't cause any problems even if the IPFS is not avaliable. 

### Deploying Pravda program

Extracting of meta happens when you deploy your Pravda program to the blockchain:

```bash
pravda broadcast deploy \
  -w <your-wallet> \
  -p <program-wallet> \ 
  -i <bytecode-of-your-program> \ 
  --meta-to-ipfs \
  --ipfs-node "/ip4/127.0.0.1/tcp/5001"
```

You should specify `--meta-to-ipfs` parameter to enable meta extracting 
and `--ipfs-node` to select necessary IPFS node (by default it uses localhost). 

This command will extract _all_ meta from your bytecode and place it in the IPFS, 
after that it will deploy program _without_ meta to the Pravda blockchain.

You can also use `pravda broadcast update` to extract meta with the same CLI parameters. 

### Reading meta from IPFS

If you need to read meta from IPFS, for example when you are disassembling bytecode, 
you also use special CLI parameters:

```bash
pravda compile disasm \
    -i <bytecode-of-your-program> \
    --meta-from-ipfs
    --ipfs-node "/ip4/127.0.0.1/tcp/5001"
```

This command will read IPFS hash of file with meta from the bytecode, 
read this file from IPFS and disassemble bytecode with this new information.