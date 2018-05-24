# How to run two node testnet

1. Build and install tools:

	cd pravda
	sbt universal:packageBin
	sudo mkdir /usr/local/pravda

	sudo unzip -d /usr/local/pravda node/target/universal/pravda-0.1.0.zip

	# Add to .bashrc (or your favourite shell profile)
	export PATH=/usr/local/pravda/pravda-0.1.0/bin:$PATH

	# Now you should be able to run from everywhere
	pravda

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
	PRAVDA_PROGRAM=$(echo '2 2 add' | pravda compile forth | base64)
	curl -X POST -H "Content-Type: application/base64" localhost:8081/api/private/broadcast?fee=0 -d $PRAVDA_PROGRAM
	curl -X POST -H "Content-Type: application/base64" localhost:8082/api/private/broadcast?fee=0 -d $PRAVDA_PROGRAM
	```

	You will also be able to view blocks via `Block explorer` started by each
	node on `https://localhost:<node api port>/ui`.

	`echo '2 2 add' | pravda compile forth | base64` takes the program in command line, compiles it to
	Pravda bytecode and prints the result to standard out base64 encoded.

	The program `2 2 add` pushes two integers on the stack and adds them. The
	response to the curl command must be `[00000004]`

	Watch the logs of the Pravda node to see more.

