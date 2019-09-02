/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.cli

import java.io.File

import pravda.cli.PravdaConfig.{CompileMode, CodegenMode, DefaultValues}
import pravda.common.bytes
import pravda.common.data.blockchain.NativeCoin
import pravda.yopt._

object PravdaArgsParser extends CommandLine[PravdaConfig] {

  val broadcastInput: CommandLine.Opt[PravdaConfig, File] =
    opt[File]('i', "input")
      .text("Input file.")
      .action {
        case (file, config: PravdaConfig.Broadcast) =>
          config.copy(input = Some(file.getAbsolutePath))
        case (_, otherwise) => otherwise
      }

  val model: CommandLine.Head[PravdaConfig] =
    head("pravda")
      .text(
        s"Pravda ${pravda.cli.BuildInfo.version.takeWhile(_ != '-')} Command Line Interface\n\n" +
          "To learn about the options for the particular command you can use the flag --help or -h after the command." +
          " For example, to get help regarding the use of the \"gen address\" command, type \"pravda gen address -h\"")
      .mdText(s"Pravda Command Line Interface\n\n" +
        "To learn about the options for the particular command you can use the flag --help or -h after the command." +
        " For example, to get help regarding the use of the \"gen address\" command, type \"pravda gen address -h\"")
      .children(
        cmd("gen")
          .text("Generate auxiliary data for Pravda.")
          .children(
            cmd("address")
              .text("Generate the ed25519 key pair. It can be used as a regular wallet or a validator node identifier.")
              .action(_ => PravdaConfig.GenAddress())
              .children(
                opt[File]('o', "output")
                  .text("Output file")
                  .action {
                    case (file, PravdaConfig.GenAddress(_)) =>
                      PravdaConfig.GenAddress(Some(file.getAbsolutePath))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("unity")
              .text("Generate the auxiliary code to call the program's methods from Unity")
              .action(_ => PravdaConfig.Codegen(CodegenMode.Dotnet))
              .children(
                opt[File]('i', "input")
                  .text("Input file with assembly.")
                  .action {
                    case (f, conf: PravdaConfig.Codegen) => conf.copy(input = Some(f.getAbsolutePath))
                    case (_, otherwise)                  => otherwise
                  }
              ),
            opt[Unit]("meta-from-ipfs")
              .text("Load metadata from IPFS if necessary. To configure the IPFS node address use \"--ipfs-node\" parameter.")
              .action {
                case ((), config: PravdaConfig.Codegen) =>
                  config.copy(metaFromIpfs = true)
                case (_, otherwise) => otherwise
              },
            opt[String]("ipfs-node")
              .text(s"Ipfs node (${DefaultValues.Broadcast.IPFS_NODE} by default).")
              .action {
                case (ipfsNode, config: PravdaConfig.Codegen) =>
                  config.copy(ipfsNode = ipfsNode)
                case (_, otherwise) => otherwise
              }
          ),
        cmd("run")
          .text("Run byte-code on Pravda VM")
          .action(_ => PravdaConfig.RunBytecode())
          .children(
            opt[String]('e', "executor")
              .text("The executor address HEX representation")
              .validate {
                case s if bytes.isHex(s) && s.length == 64 => Right(())
                case s                                     => Left(s"`$s` is not valid address. It should be 32 bytes hex string.")
              }
              .action {
                case (address, config: PravdaConfig.RunBytecode) =>
                  config.copy(executor = address)
                case (_, otherwise) => otherwise
              },
            opt[File]('i', "input")
              .text("An input file")
              .action {
                case (file, config: PravdaConfig.RunBytecode) =>
                  config.copy(input = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]("storage")
              .text("A path to the application state database")
              .action {
                case (file, config: PravdaConfig.RunBytecode) =>
                  config.copy(appStateDbPath = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]("effectsDb")
              .text("A path to the effects database")
              .action {
                case (file, config: PravdaConfig.RunBytecode) =>
                  config.copy(effectsDbPath = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[Unit]("meta-from-ipfs")
              .text("Load the metadata from IPFS if necessary. To configure the IPFS node address, use \"--ipfs-node\" parameter.")
              .action {
                case ((), config: PravdaConfig.RunBytecode) =>
                  config.copy(metaFromIpfs = true)
                case (_, otherwise) => otherwise
              },
            opt[String]("ipfs-node")
              .text(s"An ipfs node (${DefaultValues.Broadcast.IPFS_NODE} by default).")
              .action {
                case (ipfsNode, config: PravdaConfig.RunBytecode) =>
                  config.copy(ipfsNode = ipfsNode)
                case (_, otherwise) => otherwise
              }
          ),
        cmd("compile")
          .text("Compile Pravda programs.")
          .action(_ => PravdaConfig.Compile(CompileMode.Nope))
          .children(
            opt[Seq[File]]('i', "input")
              .text("An input file")
              .action {
                case (files, config: PravdaConfig.Compile) =>
                  config.copy(input = files.map(_.getAbsolutePath).toList)
                case (_, otherwise) => otherwise
              },
            opt[File]('o', "output")
              .text("An output file")
              .action {
                case (file, config: PravdaConfig.Compile) =>
                  config.copy(output = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            cmd("asm")
              .text(
                "Assemble Pravda VM bytecode from the text representation. " +
                  "The input file is a Pravda assembly language text file. " +
                  "The output is a binary Pravda program. " +
                  "By default read from stdin and print to stdout.")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.Asm)),
            cmd("disasm")
              .text(
                "Disassemble Pravda VM bytecode to the text presentation. " +
                  "The input file is a Pravda executable binary. " +
                  "The output is a text file with Pravda assembly code. " +
                  "By default read from stdin and print to stdout.")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.Disasm)),
            cmd("dotnet")
              .text(
                "Compile .exe produced by .NET compiler to Pravda VM bytecode. " +
                  "The input file is a .NET PE (portable executable). " +
                  "The output is a binary Pravda program. " +
                  "By default read from stdin and print to stdout")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.DotNet))
              .children(
                opt[String]("main-class")
                  .text("Full name of the class that should be compile to Pravda program")
                  .action {
                    case (s, config: PravdaConfig.Compile) =>
                      config.copy(mainClass = Some(s))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("evm")
              .text(
                "[THIS COMPILATION MODE IS EXPERIMENTAL]" +
                  "Compile .bin produced by the solc compiler to Pravda VM bytecode. " +
                  "The input files are .bin contract and .abi. " +
                  "The output is a binary Pravda program. " +
                  "By default read from stdin and print to stdout")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.Evm)),
            opt[Unit]("meta-from-ipfs")
              .text("Load metadata from IPFS if necessary. To configure the IPFS node address use \"--ipfs-node\" parameter.")
              .action {
                case ((), config: PravdaConfig.Compile) =>
                  config.copy(metaFromIpfs = true)
                case (_, otherwise) => otherwise
              },
            opt[String]("ipfs-node")
              .text(s"Ipfs node (${DefaultValues.Broadcast.IPFS_NODE} by default).")
              .action {
                case (ipfsNode, config: PravdaConfig.Compile) =>
                  config.copy(ipfsNode = ipfsNode)
                case (_, otherwise) => otherwise
              }
          ),
        cmd("broadcast")
          .text("Broadcast transactions and programs to the Pravda blockchain.")
          .children(
            cmd("run")
              .text("Send a transaction with Pravda Program address to the blockchain to run it")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Run))
              .children(broadcastInput),
            cmd("transfer")
              .text("Transfer native coins to a given wallet.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Transfer(None, None)))
              .children(
                opt[String]('t', "to")
                  .action {
                    case (hex,
                          config @ PravdaConfig
                            .Broadcast(mode: PravdaConfig.Broadcast.Mode.Transfer, _, _, _, _, _, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(to = Some(hex)))
                    case (_, otherwise) => otherwise
                  },
                opt[Long]('a', "amount")
                  .action {
                    case (amount,
                          config @ PravdaConfig
                            .Broadcast(mode: PravdaConfig.Broadcast.Mode.Transfer, _, _, _, _, _, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(amount = Some(amount)))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("deploy")
              .text("Deploy the Pravda program on the blockchain.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Deploy))
              .children(broadcastInput),
            cmd("call")
              .text("Call the method of the program with arguments")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Call()))
              .children(
                opt[String]("address")
                  .text("Address of the program that will be called. For example, \"xdc5056337b83726b881f241bf534ca04f7694452e0e879018872679cf8815af4\" ")
                  .action {
                    case (address,
                          config @ PravdaConfig
                            .Broadcast(mode: PravdaConfig.Broadcast.Mode.Call, _, _, _, _, _, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(address = Some(address)))
                    case (_, otherwise) => otherwise
                  },
                opt[String]("method")
                  .text("Method's name. For example, \"Spend\"")
                  .action {
                    case (method,
                          config @ PravdaConfig
                            .Broadcast(mode: PravdaConfig.Broadcast.Mode.Call, _, _, _, _, _, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(method = Some(method)))
                    case (_, otherwise) => otherwise
                  },
                opt[Seq[String]]("args")
                  .text("Method's arguments (comma separated). For example, \"xdc5056337b83726b881f241bf534ca04f7694452e0e879018872679cf8815af4,20\"")
                  .action {
                    case (args,
                          config @ PravdaConfig
                            .Broadcast(mode: PravdaConfig.Broadcast.Mode.Call, _, _, _, _, _, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(args = args))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("seal")
              .text("Seal the existing Pravda program in the blockchain.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Seal))
              .children(broadcastInput),
            cmd("update")
              .text("Update existing the Pravda program in the blockchain.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Update))
              .children(broadcastInput),
            opt[Unit]("dry-run")
              .text("Broadcast an action without applying effects.")
              .action {
                case (_, config: PravdaConfig.Broadcast) => config.copy(dryRun = true)
                case (_, otherwise)                      => otherwise
              },
            opt[File]('w', "wallet")
              .text("A file with a user wallet. You can obtain it using the 'pravda gen address' command. The format is as follows: {\"address\": <public key>, \"privateKey\": <private key>}")
              .action {
                case (file, config: PravdaConfig.Broadcast) =>
                  config.copy(wallet = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]('p', "program-wallet")
              .text("A wallet of the program account")
              .action {
                case (file, config: PravdaConfig.Broadcast) =>
                  config.copy(programWallet = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]("watt-payer-wallet")
              .text("A file with a watt payer wallet. The format is the same as for the wallet.")
              .action {
                case (file, config: PravdaConfig.Broadcast) =>
                  config.copy(wattPayerWallet = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[Long]('l', "limit")
              .text(s"The watt limit (${DefaultValues.Broadcast.WATT_LIMIT} by default).")
              .action {
                case (limit, config: PravdaConfig.Broadcast) =>
                  config.copy(wattLimit = limit)
                case (_, otherwise) => otherwise
              },
            opt[Long]('P', "price")
              .text(s"The watt price (${DefaultValues.Broadcast.WATT_PRICE} by default).")
              .action {
                case (price, config: PravdaConfig.Broadcast) =>
                  config.copy(wattPrice = NativeCoin @@ price)
                case (_, otherwise) => otherwise
              },
            opt[String]('e', "endpoint")
              .text(s"The node endpoint (${DefaultValues.Broadcast.ENDPOINT} by default).")
              .action {
                case (endpoint, config: PravdaConfig.Broadcast) =>
                  config.copy(endpoint = endpoint)
                case (_, otherwise) => otherwise
              },
            opt[Unit]("meta-to-ipfs")
              .text("Save all metadata to IPFS. To configure the IPFS node address use \"--ipfs-node\" parameter.")
              .action {
                case ((), config: PravdaConfig.Broadcast) =>
                  config.copy(metaToIpfs = true)
                case (_, otherwise) => otherwise
              },
            opt[String]("ipfs-node")
              .text(s"The ipfs node (${DefaultValues.Broadcast.IPFS_NODE} by default).")
              .action {
                case (ipfsNode, config: PravdaConfig.Broadcast) =>
                  config.copy(ipfsNode = ipfsNode)
                case (_, otherwise) => otherwise
              }
          ),
        cmd("execute")
          .text("Executes a program without side-effects. No watt-limit is required.")
          .action(_ => PravdaConfig.Execute())
          .children(
            opt[File]('w', "wallet")
              .text("A file with a user wallet. You can obtain it using the 'pravda gen address' command. The format is as follows: {\"address\": <public key>, \"privateKey\": <private key>}")
              .action {
                case (file, config: PravdaConfig.Execute) =>
                  config.copy(wallet = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[String]('e', "endpoint")
              .text(s"The node endpoint (${DefaultValues.Broadcast.ENDPOINT} by default).")
              .action {
                case (endpoint, config: PravdaConfig.Execute) =>
                  config.copy(endpoint = endpoint)
                case (_, otherwise) => otherwise
              }
          ),
        cmd("node")
          .text("Control Pravda Network Node.")
          .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Nope, None))
          .children(
            opt[File]('d', "data-dir")
              .action {
                case (dataDir, config: PravdaConfig.Node) =>
                  config.copy(dataDir = Some(dataDir.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            cmd("init")
              .text("Create a data directory and configuration for the new node.")
              .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Local(None)), None))
              .children(
                opt[Unit]("local")
                  .text("Initialize the local node with self-validation.")
                  .action {
                    case (_, config: PravdaConfig.Node) =>
                      config.copy(mode = PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Local(None)))
                    case (_, otherwise) => otherwise
                  },
                opt[Unit]("testnet")
                  .text("Initialize the node connected to the official testnet.")
                  .action {
                    case (_, config: PravdaConfig.Node) =>
                      config.copy(mode = PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Testnet))
                    case (_, otherwise) => otherwise
                  },
                opt[String]("coin-distribution")
                  .text("Initialize the local node with the addresses that has a certain amount of coins at the initial state. JSON file. The format is as follows: [{\"address\":<public key in hex>,\"amount\":<number>}]")
                  .action {
                    case (coinDistribution,
                          config @ PravdaConfig
                            .Node(PravdaConfig.Node.Mode.Init(local: PravdaConfig.Node.Network.Local), _)) =>
                      config.copy(mode = PravdaConfig.Node.Mode.Init(local.copy(Some(coinDistribution))))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("run")
              .text("Run initialized node.")
              .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Run, None))
          )
      )
}
