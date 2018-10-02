package pravda
import org.json4s.{JValue, JsonInput}

package object yaml4s {

  def renderYaml(node: JValue): String = YamlMethods.render(node)

  def parseYamlOpt(in: JsonInput, useBigDecimalForDouble: Boolean): Option[JValue] =
    YamlMethods.parseOpt(in, useBigDecimalForDouble)

  def parseAllYamlOpt(in: JsonInput, useBigDecimalForDouble: Boolean): Option[List[JValue]] =
    YamlMethods.parseAllOpt(in, useBigDecimalForDouble)
}
