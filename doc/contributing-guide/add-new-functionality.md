# How to Add a New Functionality to VM

Given that Pravda is in the active development phase, there is currently an opportunity to add new opcodes and functions to VM. However, individual developers cannot do this at their sole discretion. There should be a rationale behind the adding of any new opcode, which should be discussed with our colleagues in the chat or the Github issue. In the event that our team agrees to the adding of a new opcode, you can use the guide below for this purpose.

Remember to [generate docs](gen-doc.md)!
## Opcodes
1. Add an opcode to [pravda.vm.Opcodes](https://github.com/expload/pravda/blob/master/vm-api/src/main/scala/pravda/vm/Opcodes.scala) in the `vm-api` module. Select the section carefully. Use the hex number next to the last opcode in the section.
2. Add implementation of the opcode to the module from the [pravda.vm.operations](https://github.com/expload/pravda/tree/master/vm/src/main/scala/pravda/vm/operations) package that corresponds to the selected section. Remember to add the documentation for the method.
3. Add the appropriate pattern-match branch to the [pravda.vm.impl.VmImpl](https://github.com/expload/pravda/blob/master/vm/src/main/scala/pravda/vm/impl/VmImpl.scala).
4. Add a mnemonic declaration to the [pravda.vm.asm.Operation](https://github.com/expload/pravda/blob/master/vm-asm/src/main/scala/pravda/vm/asm/Operation.scala) orphans. If the opcode is complex (takes arguments, for example) take a look at [pravda.vm.asm.PravdaAssembler](https://github.com/expload/pravda/blob/master/vm-asm/src/main/scala/pravda/vm/asm/PravdaAssembler.scala).
5. Write a few test cases in [/vm/src/test/resources](https://github.com/expload/pravda/tree/master/vm/src/test/resources)

## Standard Library
1. Add implementation to the `pravda.vm.std` in `vm` module. For id, use the hex number next to the last added standard library function.
2. Add a reference to the `pravda.vm.StandardLibrary` object.
3. Write a few test cases in `/vm/src/test/resources`


