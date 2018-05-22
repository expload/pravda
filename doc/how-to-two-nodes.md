# How to run two node testnet

1. Build and install tools:

	cd pravda
	sbt universal:packageBin
	sudo mkdir /usr/local/pravda

	sudo unzip -d /usr/local/pravda node/target/universal/pravda-node-0.0.1.zip
	sudo unzip -d /usr/local/pravda forth/target/universal/forth-0.0.1.zip

	# Add to .bashrc (or you favourite shell profile)
    export PATH=/usr/local/pravda/forth-0.0.1/bin:$PATH
    export PATH=/usr/local/pravda/pravda-node-0.0.1/bin:$PATH

	# Now you should be able to run from anywhere
	forth
	pravda-node

2. Start nodes.

	Run start-alice.sh and start-bob.sh in two terminals to start
	two separate nodes locally.

	Both scripts can be found in `examples/two-nodes` folder.

	TC_SEEDS environment variable in both scripts is set to make Alice and Bob
	each other's seed.

	After starting nodes you'll be able to see interactions between Alice's and
	Bob's nodes in logs.

3. Send some transaction (program) to any of the nodes.
	```
	curl -X POST -H "Content-Type: application/base64" localhost:8081/api/private/broadcast?fee=0 -d $(forth -i -b '2 2 add')
	curl -X POST -H "Content-Type: application/base64" localhost:8082/api/private/broadcast?fee=0 -d $(forth -i -b '2 2 add')
	```

	You will also be able to view blocks via `Block explorer` started by each
	node on `https://localhost:<node api port>/ui`.

	`forth -i -b '2 2 add'` takes the program from command line, compiles it to
	Pravda bytecode and prints it to standard out in base64-encoded form.

	The program `2 2 add` pushes two integers on the stack and adds them. The
	response to the curl command must be `[00000004]`

	Watch logs to see more.

