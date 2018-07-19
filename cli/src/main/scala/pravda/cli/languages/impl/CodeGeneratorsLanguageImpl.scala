package pravda.cli.languages.impl

import com.google.protobuf.ByteString
import pravda.cli.languages.CodeGeneratorsLanguage
import pravda.codegen.dotnet.DotnetCodegen

import scala.concurrent.{ExecutionContext, Future}

final class CodeGeneratorsLanguageImpl(implicit executionContext: ExecutionContext)
    extends CodeGeneratorsLanguage[Future] {
  override def dotnet(input: ByteString, excludeBigInteger: Boolean): Future[List[(String, String)]] = Future {
    val methods = DotnetCodegen.generate(input)
    List(methods)
  }
}
