package io.mytc.timechain

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.mytc.tendermint.abci.Server
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.servers.{Abci, ApiRoute, GuiRoute, HttpServer}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import io.mytc.timechain.persistence.{BlockChainStore, NodeStore}


object launcher extends App {

  import Config._

  sys.env.get("TC_CONFIG_FILE") foreach { path =>
    sys.props.put("config.file", path)
  }


  private implicit val system: ActorSystem = ActorSystem("timechain-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  val bcPath = new File(timeChainConfig.dataDirectory.getAbsolutePath, "bc_data").getAbsolutePath
  val nodePath = new File(timeChainConfig.dataDirectory.getAbsolutePath, "node_data").getAbsolutePath

  private implicit val blockchainStore: BlockChainStore = BlockChainStore(bcPath)
  private implicit val nodeStore: NodeStore = NodeStore(nodePath)

  val abciClient = new AbciClient(timeChainConfig.tendermint.rpcPort)

  val abci = new Abci(abciClient)
  val server = Server(
    cfg = Server.Config(
      host = "127.0.0.1",
      port = timeChainConfig.tendermint.proxyAppPort,
      usock = timeChainConfig.tendermint.proxyAppSock
    ),
    api = abci
  )

  val httpServer = {
    val apiRoute = new ApiRoute(abciClient)
    val guiRoute = new GuiRoute()
    HttpServer.start(timeChainConfig.api, apiRoute.route, guiRoute.route)
  }

  val tendermintNode = Await.result(tendermint.run(timeChainConfig), 1.seconds)

  server.start()

  println("Tendermint node started")

  sys.addShutdownHook {
    print("Shutting down tendermint node...")
    tendermintNode.destroy()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Shutting down API server...")
    Await.result(httpServer.flatMap(_.unbind()), 10.second)
    system.terminate()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Closing blockchain db...")
    blockchainStore.close()
    print("Closing node db...")
    nodeStore.close()
  }

}
