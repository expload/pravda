package pravda.dotnet

package parsers

import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import utest._

object EventTests extends TestSuite {

  val tests = Tests {
    'eventParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("event.exe")

      methods ==> List(
        Method(
          List(
            Nop,
            LdStr("my_event"),
            LdcI4(1234),
            Call(MethodSpecData(MemberRefData(TypeRefData(10, "Log", "Com.Expload"), "Event", 16), 24)),
            Nop,
            LdStr("my_event"),
            LdStr("my_string"),
            Call(MethodSpecData(MemberRefData(TypeRefData(10, "Log", "Com.Expload"), "Event", 16), 28)),
            Nop,
            LdStr("my_event"),
            LdcR8(2.0),
            Call(MethodSpecData(MemberRefData(TypeRefData(10, "Log", "Com.Expload"), "Event", 16), 32)),
            Nop,
            LdStr("my_event"),
            LdcI44,
            NewArr(TypeRefData(6, "Byte", "System")),
            Dup,
            LdToken(FieldData(307, "12DADA1FFF4D4787ADE3333147202C3B443E376F", 64)),
            Call(MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"), "InitializeArray", 36)),
            NewObj(MemberRefData(TypeRefData(10, "Bytes", "Com.Expload"), ".ctor", 44)),
            Call(MethodSpecData(MemberRefData(TypeRefData(10, "Log", "Com.Expload"), "Event", 16), 50)),
            Nop,
            Ret
          ),
          4,
          None
        ),
        Method(List(Nop, Ret), 0, None),
        Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
      )

    }
  }
}
