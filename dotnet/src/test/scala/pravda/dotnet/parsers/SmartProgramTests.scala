package pravda.dotnet

package parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object SmartProgramTests extends TestSuite {

  val tests = Tests {
    'smartProgramParse - {
      val Right((_, cilData, methods, signatures)) = parseFile("smart_program.exe")

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              LdArg1,
              LdcI40,
              CallVirt(MemberRefData(TypeSpecData(20), "getDefault", 28)),
              StLoc0,
              BrS(0),
              LdLoc0,
              Ret
            ),
            3,
            Some(16)
          ),
          Method(
            List(
              Nop,
              LdArg2,
              LdcI40,
              Cgt,
              StLoc0,
              LdLoc0,
              BrFalseS(95),
              Nop,
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              Call(MemberRefData(TypeRefData(10, "Info", "Com.Expload"), "Sender", 42)),
              LdcI40,
              CallVirt(MemberRefData(TypeSpecData(20), "getDefault", 28)),
              LdArg2,
              Clt,
              LdcI40,
              Ceq,
              StLoc1,
              LdLoc1,
              BrFalseS(66),
              Nop,
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              Call(MemberRefData(TypeRefData(10, "Info", "Com.Expload"), "Sender", 42)),
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              Call(MemberRefData(TypeRefData(10, "Info", "Com.Expload"), "Sender", 42)),
              LdcI40,
              CallVirt(MemberRefData(TypeSpecData(20), "getDefault", 28)),
              LdArg2,
              Sub,
              CallVirt(MemberRefData(TypeSpecData(20), "put", 47)),
              Nop,
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              LdArg1,
              LdArg0,
              LdFld(FieldData(1, "balances", 64)),
              LdArg1,
              LdcI40,
              CallVirt(MemberRefData(TypeSpecData(20), "getDefault", 28)),
              LdArg2,
              Add,
              CallVirt(MemberRefData(TypeSpecData(20), "put", 47)),
              Nop,
              Nop,
              Nop,
              Ret
            ),
            5,
            Some(37)
          ),
          Method(
            List(
              LdArg0,
              NewObj(MemberRefData(TypeSpecData(20), ".ctor", 6)),
              StFld(FieldData(1, "balances", 64)),
              LdArg0,
              Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
              Nop,
              Ret
            ),
            0,
            None
          ),
          Method(List(Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None)
        )
      )

      val mappingClass = Cls(TypeRefData(10, "Mapping`2", "Com.Expload"))
      val addressClass = Cls(TypeRefData(10, "Bytes", "Com.Expload"))

      DiffUtils.assertEqual(
        signatures.toList.sortBy(_._1),
        List(
          1 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false))),
          6 -> MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List()),
          10 -> MethodRefDefSig(true,
                                false,
                                false,
                                false,
                                0,
                                Tpe(Void, false),
                                List(Tpe(ValueTpe(TypeRefData(15, "DebuggingModes", "")), false))),
          16 -> LocalVarSig(List(LocalVar(I4, false))),
          20 -> TypeSig(
            Tpe(
              Generic(
                mappingClass,
                List(addressClass, I4)
              ),
              false
            )),
          28 -> MethodRefDefSig(true,
                                false,
                                false,
                                false,
                                0,
                                Tpe(Var(1), false),
                                List(Tpe(Var(0), false), Tpe(Var(1), false))),
          37 -> LocalVarSig(List(LocalVar(Boolean, false), LocalVar(Boolean, false))),
          42 -> MethodRefDefSig(false, false, false, false, 0, Tpe(addressClass, false), List()),
          (47,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (64, FieldSig(Generic(mappingClass, List(addressClass, I4)))),
          (73,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(I4, false),
                           List(Tpe(Cls(TypeRefData(10, "Bytes", "Com.Expload")), false)))),
          (79,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Cls(TypeRefData(10, "Bytes", "Com.Expload")), false), Tpe(I4, false)))),
          (86, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
