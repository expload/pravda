package pravda.dotnet

package parser

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parser.CIL._
import pravda.common.TestUtils
import utest._

object StaticClassTests extends TestSuite {

  val tests = Tests {
    'staticClassParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("static_class.exe")

      TestUtils.assertEqual(
        methods,
        List(
          Method(List(LdArg1, Call(MethodDefData(5, 0, 150, "BytesToHex", 65, Vector(ParamData(0, 1, "bytes")))), Ret),
                 0,
                 None),
          Method(List(Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Ret), 0, None),
          Method(
            List(
              LdArg0,
              BrTrueS(6),
              LdStr("0"),
              Ret,
              LdArg0,
              LdcI41,
              BneUnS(6),
              LdStr("1"),
              Ret,
              LdArg0,
              LdcI42,
              BneUnS(6),
              LdStr("2"),
              Ret,
              LdArg0,
              LdcI43,
              BneUnS(6),
              LdStr("3"),
              Ret,
              LdArg0,
              LdcI44,
              BneUnS(6),
              LdStr("4"),
              Ret,
              LdArg0,
              LdcI45,
              BneUnS(6),
              LdStr("5"),
              Ret,
              LdArg0,
              LdcI46,
              BneUnS(6),
              LdStr("6"),
              Ret,
              LdArg0,
              LdcI47,
              BneUnS(6),
              LdStr("7"),
              Ret,
              LdArg0,
              LdcI48,
              BneUnS(6),
              LdStr("8"),
              Ret,
              LdArg0,
              LdcI4S(9),
              BneUnS(6),
              LdStr("9"),
              Ret,
              LdArg0,
              LdcI4S(10),
              BneUnS(6),
              LdStr("A"),
              Ret,
              LdArg0,
              LdcI4S(11),
              BneUnS(6),
              LdStr("B"),
              Ret,
              LdArg0,
              LdcI4S(12),
              BneUnS(6),
              LdStr("C"),
              Ret,
              LdArg0,
              LdcI4S(13),
              BneUnS(6),
              LdStr("D"),
              Ret,
              LdArg0,
              LdcI4S(14),
              BneUnS(6),
              LdStr("E"),
              Ret,
              LdArg0,
              LdcI4S(15),
              BneUnS(6),
              LdStr("F"),
              Ret,
              LdStr(""),
              Ret
            ),
            2,
            None
          ),
          Method(
            List(
              LdArg0,
              LdcI4S(16),
              Div,
              Call(MethodDefData(3, 0, 145, "HexPart", 55, Vector(ParamData(0, 1, "b")))),
              LdArg0,
              LdcI4S(16),
              Rem,
              Call(MethodDefData(3, 0, 145, "HexPart", 55, Vector(ParamData(0, 1, "b")))),
              Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", 16)),
              Ret
            ),
            0,
            None
          ),
          Method(
            List(
              LdStr(""),
              StLoc0,
              LdcI40,
              StLoc1,
              BrS(23),
              LdLoc0,
              LdArg0,
              LdLoc1,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Com.Expload"), "get_Item", 27)),
              Call(MethodDefData(4, 0, 150, "ByteToHex", 60, Vector(ParamData(0, 1, "b")))),
              Call(MemberRefData(TypeRefData(6, "String", "System"), "Concat", 16)),
              StLoc0,
              LdLoc1,
              LdcI41,
              Add,
              StLoc1,
              LdLoc1,
              LdArg0,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Com.Expload"), "Length", 32)),
              BltS(-32),
              LdLoc0,
              Ret
            ),
            3,
            Some(22)
          )
        )
      )
    }
  }
}
