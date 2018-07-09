package pravda.dotnet.parsers

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
      val Right((_, cilData, methods, signatures)) = FileParser.parseFile("arrays.exe")

      println(cilData.tables.fieldTable.mkString("\n"))

      DiffUtils.assertEqual(
        methods,
        List(
          Method(
            List(
              Nop,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "7037807198C22A7D2B0807371D763779A84FDFCF", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              StLoc0,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "E809C5D1CEA47B45E34701D23F608A9A58034DC9", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              NewObj(MethodDefData(0, 6278, ".ctor", 160, Vector(ParamData(0, 1, "bytes")))),
              StLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "B470CF972A0D84FBAEEEDB51A963A902269417E8", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              NewObj(MethodDefData(0, 6278, ".ctor", 160, Vector(ParamData(0, 1, "bytes")))),
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
              CallVirt(MethodDefData(0, 2182, "get_Item", 166, Vector(ParamData(0, 1, "i")))),
              StLocS(5),
              LdLoc2,
              LdcI41,
              CallVirt(MethodDefData(0, 2182, "get_Item", 166, Vector(ParamData(0, 1, "i")))),
              StLocS(6),
              LdLoc1,
              LdcI41,
              LdcI42,
              CallVirt(
                MethodDefData(0, 134, "Slice", 177, Vector(ParamData(0, 1, "start"), ParamData(0, 2, "length")))),
              StLocS(7),
              LdArg0,
              LdFld(FieldData(6, "bytes", 121)),
              LdLoc1,
              LdLoc2,
              CallVirt(MemberRefData(TypeSpecData(45), "put", 54)),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "bytes", 121)),
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "1EAFE5ED57A26A58369E0ECC65DD21A143D475E1", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              NewObj(MethodDefData(0, 6278, ".ctor", 160, Vector(ParamData(0, 1, "bytes")))),
              CallVirt(MemberRefData(TypeSpecData(45), "exists", 62)),
              StLocS(8),
              LdLocS(8),
              BrFalseS(37),
              Nop,
              LdArg0,
              LdFld(FieldData(6, "bytes", 121)),
              LdLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Byte", "System")),
              Dup,
              LdToken(FieldData(307, "B470CF972A0D84FBAEEEDB51A963A902269417E8", 131)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              NewObj(MethodDefData(0, 6278, ".ctor", 160, Vector(ParamData(0, 1, "bytes")))),
              CallVirt(MemberRefData(TypeSpecData(45), "put", 54)),
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
              Ret
            ),
            5,
            Some(21)
          ),
          Method(
            List(
              Nop,
              LdcI43,
              NewArr(TypeRefData(6, "Char", "System")),
              Dup,
              LdToken(FieldData(307, "9F04F41A848514162050E3D68C1A7ABB441DC2B5", 143)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              StLoc0,
              LdcI43,
              NewArr(TypeRefData(6, "Int32", "System")),
              Dup,
              LdToken(FieldData(307, "E429CCA3F703A39CC5954A6572FEC9086135B34E", 139)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
              StLoc1,
              LdcI43,
              NewArr(TypeRefData(6, "Double", "System")),
              Dup,
              LdToken(FieldData(307, "380E84549CB845604C318E8E14B73622CC10AF42", 135)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
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
              LdToken(FieldData(307, "8CFA957D76B6E190580D284C12F31AA6E3E2D41C", 139)),
              Call(
                MemberRefData(TypeRefData(6, "RuntimeHelpers", "System.Runtime.CompilerServices"),
                              "InitializeArray",
                              37)),
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
              Ret
            ),
            4,
            Some(68)
          ),
          Method(List(Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Attribute", "System"), ".ctor", 6)), Nop, Ret),
                 0,
                 None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(List(), 0, None),
          Method(
            List(
              Nop,
              LdArg0,
              LdArg1,
              CallVirt(MemberRefData(TypeSpecData(87), "exists", 62)),
              LdcI40,
              Ceq,
              StLoc0,
              LdLoc0,
              BrFalseS(5),
              Nop,
              LdArg2,
              StLoc1,
              BrS(11),
              Nop,
              LdArg0,
              LdArg1,
              CallVirt(MemberRefData(TypeSpecData(87), "get", 96)),
              StLoc1,
              BrS(0),
              LdLoc1,
              Ret
            ),
            2,
            Some(81)
          ),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Ret), 0, None),
          Method(List(LdArg0, Call(MemberRefData(TypeRefData(6, "Object", "System"), ".ctor", 6)), Nop, Nop, Ret),
                 0,
                 None),
          Method(List(Nop, LdcI40, StLoc0, BrS(0), LdLoc0, Ret), 1, Some(103)),
          Method(List(Nop, BrS(0), Ret), 0, None),
          Method(List(Nop, LdNull, StLoc0, BrS(0), LdLoc0, Ret), 1, Some(107))
        )
      )

      val bytesClass = Cls(
        TypeDefData(
          1048577,
          "Bytes",
          "io.mytc.pravda",
          Ignored,
          Vector(),
          Vector(
            MethodDefData(0, 6278, ".ctor", 160, Vector(ParamData(0, 1, "bytes"))),
            MethodDefData(0, 2182, "get_Item", 166, Vector(ParamData(0, 1, "i"))),
            MethodDefData(0, 2182, "set_Item", 171, Vector(ParamData(0, 1, "i"), ParamData(0, 2, "value"))),
            MethodDefData(0, 134, "Slice", 177, Vector(ParamData(0, 1, "start"), ParamData(0, 2, "length")))
          )
        ))

      val mappingClass = Cls(
        TypeDefData(
          1048705,
          "Mapping`2",
          "io.mytc.pravda",
          Ignored,
          Vector(),
          Vector(
            MethodDefData(0, 1478, "get", 96, Vector(ParamData(0, 1, "key"))),
            MethodDefData(0, 1478, "exists", 62, Vector(ParamData(0, 1, "key"))),
            MethodDefData(0, 1478, "put", 54, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "value"))),
            MethodDefData(0, 134, "getDefault", 151, Vector(ParamData(0, 1, "key"), ParamData(0, 2, "def"))),
            MethodDefData(0, 6276, ".ctor", 6, Vector())
          )
        ))

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
          (16, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(String, false)))),
          (21,
           LocalVarSig(
             List(
               LocalVar(Arr(U1, ArrayShape(1, List(), List())), false),
               LocalVar(bytesClass, false),
               LocalVar(bytesClass, false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(U1, false),
               LocalVar(bytesClass, false),
               LocalVar(Boolean, false)
             ))),
          (37,
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
          (45,
           TypeSig(
             Tpe(
               Generic(
                 mappingClass,
                 List(bytesClass, bytesClass)
               ),
               false
             ))),
          (54,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (62, MethodRefDefSig(true, false, false, false, 0, Tpe(Boolean, false), List(Tpe(Var(0), false)))),
          (68,
           LocalVarSig(
             List(
               LocalVar(Arr(Char, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(I4, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(R8, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(String, ArrayShape(1, List(), List())), false),
               LocalVar(Arr(U4, ArrayShape(1, List(), List())), false)
             ))),
          (81, LocalVarSig(List(LocalVar(Boolean, false), LocalVar(Var(1), false)))),
          (87,
           TypeSig(
             Tpe(
               Generic(
                 mappingClass,
                 List(Var(0), Var(1))
               ),
               false
             ))),
          (96, MethodRefDefSig(true, false, false, false, 0, Tpe(Var(1), false), List(Tpe(Var(0), false)))),
          (103, LocalVarSig(List(LocalVar(U1, false)))),
          (107,
           LocalVarSig(
             List(
               LocalVar(
                 bytesClass,
                 false
               )))),
          (121,
           FieldSig(
             Generic(
               mappingClass,
               List(
                 bytesClass,
                 bytesClass
               )
             ))),
          (131, FieldSig(ValueTpe(TypeDefData(275, "__StaticArrayInitTypeSize=3", "", Ignored, Vector(), Vector())))),
          (135, FieldSig(ValueTpe(TypeDefData(275, "__StaticArrayInitTypeSize=24", "", Ignored, Vector(), Vector())))),
          (139, FieldSig(ValueTpe(TypeDefData(275, "__StaticArrayInitTypeSize=12", "", Ignored, Vector(), Vector())))),
          (143, FieldSig(ValueTpe(TypeDefData(275, "__StaticArrayInitTypeSize=6 ", "", Ignored, Vector(), Vector())))),
          (147, MethodRefDefSig(false, false, false, false, 0, Tpe(Void, false), List())),
          (151,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Var(1), false),
                           List(Tpe(Var(0), false), Tpe(Var(1), false)))),
          (160,
           MethodRefDefSig(true,
                           false,
                           false,
                           false,
                           0,
                           Tpe(Void, false),
                           List(Tpe(Arr(U1, ArrayShape(1, List(), List())), false)))),
          (166, MethodRefDefSig(true, false, false, false, 0, Tpe(U1, false), List(Tpe(I4, false)))),
          (171, MethodRefDefSig(true, false, false, false, 0, Tpe(Void, false), List(Tpe(I4, false), Tpe(U1, false)))),
          (177,
           MethodRefDefSig(
             true,
             false,
             false,
             false,
             0,
             Tpe(
               bytesClass,
               false
             ),
             List(Tpe(I4, false), Tpe(I4, false))
           ))
        )
      )
    }
  }
}
