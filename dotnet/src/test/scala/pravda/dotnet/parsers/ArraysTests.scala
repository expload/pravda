package pravda.dotnet

package parsers

import pravda.common.DiffUtils
import pravda.dotnet.data.Method
import pravda.dotnet.data.TablesData._
import pravda.dotnet.parsers.CIL._
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._
import utest._

object ArraysTests extends TestSuite {

  val tests = Tests {
    'arrayParse - {
      val Right((_, cilData, methods, signatures)) = parsePeFile("arrays.exe")

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "7037807198C22A7D2B0807371D763779A84FDFCF", 119)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              StLoc0,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "E809C5D1CEA47B45E34701D23F608A9A58034DC9", 119)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              NewObj(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), ".ctor", 41)),
              StLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "B470CF972A0D84FBAEEEDB51A963A902269417E8", 119)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              NewObj(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), ".ctor", 41)),
              StLoc2,
              LdLoc0,
              LdcI40,
              LdElemU1,
              StLoc3,
              LdLoc0,
              LdcI42,
              LdElemU1,
              StLocS(4),
              LdLoc1,
              LdcI41,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), "get_Item", 47)),
              StLocS(5),
              LdLoc2,
              LdcI41,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), "get_Item", 47)),
              StLocS(6),
              LdLoc1,
              LdcI41,
              LdcI42,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), "Slice", 52)),
              StLocS(7),
              LdArg0,
              LdFld(FieldData(6, "bytes", 109)),
              LdLoc1,
              LdLoc2,
              CallVirt(MemberRefData(TypeSpecData(59), "put", 68)),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "bytes", 109)),
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "1EAFE5ED57A26A58369E0ECC65DD21A143D475E1", 119)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              NewObj(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), ".ctor", 41)),
              CallVirt(MemberRefData(TypeSpecData(59), "exists", 76)),
              StLocS(9),
              LdLocS(9),
              BrFalseS(37),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "bytes", 109)),
              LdLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "B470CF972A0D84FBAEEEDB51A963A902269417E8", 119)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              NewObj(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), ".ctor", 41)),
              CallVirt(MemberRefData(TypeSpecData(59), "put", 68)),
              Nop,
              Nop,
              LdLoc0,
              LdcI40,
              LdcI42,
              StElemI1,
              LdLoc0,
              LdcI41,
              LdcI41,
              StElemI1,
              LdLoc1,
              CallVirt(MemberRefData(TypeRefData(10, "Bytes", "Expload.Pravda"), "Length", 82)),
              StLocS(8),
              Ret
            ),
            5,
            Some(16)
          ),
          Method(
            List(
              Nop,
              LdcI43,
              NewArr(TypeRefData(6, "Char", "System")),
              Dup,
              LdToken(FieldData(307, "9F04F41A848514162050E3D68C1A7ABB441DC2B5", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              StLoc0,
              LdcI43,
              NewArr(TypeRefData(6, "Int32", "System")),
              Dup,
              LdToken(FieldData(307, "E429CCA3F703A39CC5954A6572FEC9086135B34E", 127)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              StLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Double", "System")),
              Dup,
              LdToken(FieldData(307, "380E84549CB845604C318E8E14B73622CC10AF42", 123)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              StLoc2,
              LdcI43,
              NewArr(TypeRefData(6, "String", "System")),
              Dup,
              LdcI40,
              LdStr("abc"),
              StElemRef,
              Dup,
              LdcI41,
              LdStr("def"),
              StElemRef,
              Dup,
              LdcI42,
              LdStr("rty"),
              StElemRef,
              StLoc3,
              LdcI43,
              NewArr(TypeRefData(6, "UInt32", "System")),
              Dup,
              LdToken(FieldData(307, "8CFA957D76B6E190580D284C12F31AA6E3E2D41C", 127)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              33)),
              StLocS(4),
              LdLoc0,
              LdcI41,
              LdcI4S(100),
              StElemI2,
              LdLoc1,
              LdcI41,
              LdcI44,
              StElemI4,
              LdLoc2,
              LdcI41,
              LdcR8(4.0),
              StElemR8,
              LdLoc3,
              LdcI41,
              LdStr("asdf"),
              StElemRef,
              LdLocS(4),
              LdcI41,
              LdcI47,
              StElemI4,
              LdLoc3,
              LdLen,
              ConvI4,
              StLocS(5),
              Ret
            ),
            4,
            Some(86)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(
            List(
              LdArg0,
              NewObj(MemberRefData(TypeSpecData(59), ".ctor", 6)),
              StFld(FieldData(6, "bytes", 109)),
              LdArg0,
              Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)),
              Nop,
              Ret
            ),
            0,
            None
          )
        )
      )
      DiffUtils.assertEqual(
        signatures.toList.sortBy(_._1),
        List(
          (1, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false)))),
          (6, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List())),
          (10,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(ValueTpe(TypeRefData(15, "DebuggingModes", "")), false)))),
          (16,
           LocalVarSig(
             List(
               LocalVar(Arr(U1, ArrayShape(1, List(), List())), false),
               LocalVar(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")), false),
               LocalVar(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")), false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")), false),
               LocalVar(I4, false),
               LocalVar(Boolean, false)
             ))),
          (33,
           MethodRefDefSig(
             false,
             false,
             false,
             false,
             0,
             Tpe(Void, false),
             List(Tpe(Cls(TypeRefData(6, "Array", "System")), false),
                  Tpe(ValueTpe(TypeRefData(6, "RuntimeFieldHandle", "System")), false))
           )),
          (41,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Arr(U1, ArrayShape(1, List(), List())), false)))),
          (47, MethodRefDefSig(true, false, false, false, 0, Tpe(U1, false), List(Tpe(I4, false)))),
          (52,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")), false),
                           List(Tpe(I4, false), Tpe(I4, false)))),
          (59,
           TypeSig(
             Tpe(
               Generic(Cls(TypeRefData(10, "Mapping`2", "Expload.Pravda")),
                       List(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")),
                            Cls(TypeRefData(10, "Bytes", "Expload.Pravda")))),
               false
             ))),
          (68,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (76, MethodRefDefSig(true, false, false, false, 0, Tpe(Boolean, false), List(Tpe(Var(0), false)))),
          (82, MethodRefDefSig(true, false, false, false, 0, Tpe(I4, false), List())),
          (86,
           LocalVarSig(
             List(
               LocalVar(Arr(Char, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(I4, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(R8, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(String, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(U4, ArrayShape(1, List(), List())), false),
               LocalVar(I4, false)
             ))),
          (109,
           FieldSig(
             Generic(Cls(TypeRefData(10, "Mapping`2", "Expload.Pravda")),
                     List(Cls(TypeRefData(10, "Bytes", "Expload.Pravda")),
                          Cls(TypeRefData(10, "Bytes", "Expload.Pravda")))))),
          (119,
           FieldSig(
             ValueTpe(
               TypeDefData(3,
                           275,
                           "__StaticArrayInitTypeSize=3",
                           "",
                           TypeRefData(6, "ValueType", "System"),
                           Vector(),
                           Vector())))),
          (123,
           FieldSig(
             ValueTpe(
               TypeDefData(6,
                           275,
                           "__StaticArrayInitTypeSize=24",
                           "",
                           TypeRefData(6, "ValueType", "System"),
                           Vector(),
                           Vector())))),
          (127,
           FieldSig(
             ValueTpe(
               TypeDefData(5,
                           275,
                           "__StaticArrayInitTypeSize=12",
                           "",
                           TypeRefData(6, "ValueType", "System"),
                           Vector(),
                           Vector())))),
          (131,
           FieldSig(
             ValueTpe(
               TypeDefData(4,
                           275,
                           "__StaticArrayInitTypeSize=6",
                           "",
                           TypeRefData(6, "ValueType", "System"),
                           Vector(),
                           Vector())))),
          (135, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List()))
        )
      )
    }
  }
}
