package io.mytc.sood.cil

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.PE.Info._
import io.mytc.sood.cil.TablesData._
import io.mytc.sood.cil.TablesInfo._
import io.mytc.sood.cil.utils._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

// all *.exe files was compiled by csc *.cs

class PeParsersSpec extends FlatSpec with Matchers {
  def parsePe(file: String): Validated[(Pe, CilData, Seq[Seq[OpCode]])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    val peV = PE.parseInfo(Bytes(fileBytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      codeParser = CIL.code(cilData)
      ops <- pe.methods.map(m => codeParser.parse(m.codeBytes).toValidated.joinRight).sequence
    } yield (pe, cilData, ops)
  }

  "hello_world.exe" should "be parsed correctly" in {
    val Right((pe, cilData, opCodes)) = parsePe("hello_world.exe")

    pe shouldBe Pe(
      PeHeader(
        PeFileHeader(3, 224, 34),
        PeHeaderStandardFields(1024, 2048, 0, 9118, 8192, 16384),
        NtSpecificFields(4194304, 8192, 32768, 512),
        PeHeaderDataDirectories(9036, 79, 24576, 12, 8192, 8, 8200, 72),
        ArrayBuffer(
          SectionHeader(".text", 932, 8192, 1024, 512, 1610612768),
          SectionHeader(".rsrc", 1196, 16384, 1536, 1536, 1073741888),
          SectionHeader(".reloc", 12, 24576, 512, 3072, 1107296320)
        )
      ),
      CliHeader(72, 2, 5, 8296, 740, 1, 100663297, 0, 0, 0, 0, 0, 0),
      MetadataRoot(
        "v4.0.30319",
        ArrayBuffer(StreamHeader(108, 252, "#~"),
                    StreamHeader(360, 252, "#Strings"),
                    StreamHeader(612, 28, "#US"),
                    StreamHeader(640, 16, "#GUID"),
                    StreamHeader(656, 84, "#Blob"))
      ),
      PeData(
        hex"""0x003c4d6f64756c653e006d73636f726c69620048656c6c6f576f726c
                640068656c6c6f5f776f726c6400436f6e736f6c650057726974654c69
                6e650044656275676761626c6541747472696275746500436f6d70696c
                6174696f6e52656c61786174696f6e734174747269627574650052756e
                74696d65436f6d7061746962696c697479417474726962757465006865
                6c6c6f5f776f726c642e6578650053797374656d004d61696e002e6374
                6f720053797374656d2e446961676e6f73746963730053797374656d2e
                52756e74696d652e436f6d70696c657253657276696365730044656275
                6767696e674d6f646573004f626a65637400000000""",
        hex"""0x0019480065006c006c006f00200057006f0072006c00640021000000""",
        hex"""0x00042001010803200001052001011111040001010e08b77a5c561934
               e089030000010801000800000000001e01000100540216577261704e6f6
               e457863657074696f6e5468726f77730108010007010000000000""",
        Seq(0, 1, 2, 6, 10, 12, 32, 35),
        Seq(
          ArrayBuffer(ModuleRow),
          ArrayBuffer(TypeRefRow, TypeRefRow, TypeRefRow, TypeRefRow, TypeRefRow, TypeRefRow),
          ArrayBuffer(TypeDefRow, TypeDefRow),
          ArrayBuffer(MethodDefRow(8272, 0, 150, 165, 30, 1), MethodDefRow(8286, 0, 6278, 170, 6, 1)),
          ArrayBuffer(MemberRefRow(9, 170, 1),
                      MemberRefRow(17, 170, 6),
                      MemberRefRow(25, 170, 10),
                      MemberRefRow(49, 50, 16),
                      MemberRefRow(41, 170, 6)),
          ArrayBuffer(CustomAttributeRow, CustomAttributeRow, CustomAttributeRow),
          ArrayBuffer(AssemblyRow),
          ArrayBuffer(AssemblyRefRow)
        )
      ),
      Seq(TinyMethodHeader(hex"0x007201000070280400000a002a"), TinyMethodHeader(hex"0x02280500000a002a"))
    )

    cilData.tables shouldBe Seq(
      Seq(Ignored),
      Seq(Ignored, Ignored, Ignored, Ignored, Ignored, Ignored),
      Seq(Ignored, Ignored),
      Seq(MethodDefData("Main"), MethodDefData(".ctor")),
      Seq(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(49, "WriteLine", hex"0x0001010e"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      Seq(Ignored, Ignored, Ignored),
      Seq(Ignored),
      Seq(Ignored)
    )

    opCodes shouldBe
      Seq(
        Seq(Nop, LdStr("Hello World!"), Call(MemberRefData(49, "WriteLine", hex"0x0001010e")), Nop, Ret),
        Seq(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
      )
  }

  "1.exe" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("1.exe")

    cilData.tables shouldBe Seq(
      Seq(Ignored),
      Seq(Ignored, Ignored, Ignored, Ignored, Ignored, Ignored, Ignored, Ignored),
      Seq(Ignored, Ignored),
      Seq(MethodDefData("Main"), MethodDefData(".ctor")),
      Seq(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(49, "WriteLine", hex"0x0001010e"),
        MemberRefData(49, "ReadLine", hex"0x00000e"),
        MemberRefData(57, "Parse", hex"0x0001080e"),
        MemberRefData(65, "Concat", hex"0x00020e1c1c"),
        MemberRefData(49, "Read", hex"0x000008"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      Seq(Ignored, Ignored, Ignored),
      Seq(Ignored),
      Seq(Ignored),
      Seq(Ignored)
    )

    opCodes shouldBe
      Seq(
        Seq(
          Nop,
          LdcI40,
          StLoc1,
          LdStr("Enter the Number : "),
          Call(MemberRefData(49, "WriteLine", hex"0x0001010e")),
          Nop,
          Call(MemberRefData(49, "ReadLine", hex"0x00000e")),
          Call(MemberRefData(57, "Parse", hex"0x0001080e")),
          StLoc0,
          LdStr("Number: "),
          LdLoc0,
          Box(Ignored),
          Call(MemberRefData(65, "Concat", hex"0x00020e1c1c")),
          Call(MemberRefData(49, "WriteLine", hex"0x0001010e")),
          Nop,
          BrS(11),
          Nop,
          LdLoc1,
          LdcI41,
          Add,
          StLoc1,
          LdLoc0,
          LdcI4S(10),
          Div,
          StLoc0,
          Nop,
          LdLoc0,
          LdcI40,
          Cgt,
          StLoc2,
          LdLoc2,
          BrTrueS(-19),
          LdStr("Magnitude: "),
          LdLoc1,
          Box(Ignored),
          Call(MemberRefData(65, "Concat", hex"0x00020e1c1c")),
          Call(MemberRefData(49, "WriteLine", hex"0x0001010e")),
          Nop,
          Call(MemberRefData(49, "Read", hex"0x000008")),
          Pop,
          Ret
        ),
        Seq(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
      )
  }

  "2.exe" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("2.exe")

    cilData.tables shouldBe Seq(
      Seq(Ignored),
      Seq(Ignored, Ignored, Ignored, Ignored, Ignored),
      Seq(Ignored, Ignored),
      Seq(FieldData(22, "val", hex"0x0608")),
      Seq(MethodDefData("B"), MethodDefData("Main"), MethodDefData(".ctor"), MethodDefData(".cctor")),
      Seq(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      Seq(Ignored, Ignored, Ignored),
      Seq(Ignored, Ignored),
      Seq(Ignored),
      Seq(Ignored)
    )

    opCodes shouldBe Seq(
      Seq(Nop, LdSFld(FieldData(22, "val", hex"0x0608")), StLoc0, BrS(0), LdLoc0, Ret),
      Seq(Nop,
          LdcI42,
          StLoc0,
          Call(MethodDefData("B")),
          StLoc1,
          LdLoc0,
          LdLoc1,
          Add,
          StLoc2,
          LdLoc0,
          LdLoc1,
          Mull,
          StLoc3,
          LdLoc0,
          LdLoc1,
          Div,
          StLocS(4),
          Ret),
      Seq(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      Seq(LdcI4S(42), StSFld(FieldData(22, "val", hex"0x0608")), Ret)
    )
  }

  "arithmetic operations" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("arithmetics.exe")

    cilData.tables shouldBe Seq(
      Seq(Ignored),
      Seq(Ignored, Ignored, Ignored, Ignored, Ignored),
      Seq(Ignored, Ignored),
      Seq(FieldData(22, "x", hex"0x0608")),
      Seq(MethodDefData("Main"), MethodDefData(".ctor"), MethodDefData(".cctor")),
      Seq(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      Seq(Ignored, Ignored, Ignored),
      Seq(Ignored),
      Seq(Ignored),
      Seq(Ignored)
    )

    opCodes shouldBe List(
      List(
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Add,
        StLoc0,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Mull,
        StLoc1,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Div,
        StLoc2,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI42,
        Rem,
        StLoc3,
        LdLoc0,
        LdLoc1,
        Add,
        LdcI4S(42),
        Add,
        LdLoc2,
        Mull,
        LdLoc3,
        Add,
        LdcI4(1337),
        Div,
        StLocS(4),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdcI4S(10), StSFld(FieldData(22, "x", hex"0x0608")), Ret)
    )
  }

  "method calling" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("method_calling.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(
        MethodDefData("answer"),
        MethodDefData("secretAnswer"),
        MethodDefData("sum"),
        MethodDefData("personalAnswer"),
        MethodDefData("personalSecretAnswer"),
        MethodDefData("Main"),
        MethodDefData(".ctor")
      ),
      List(Ignored, Ignored),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(Nop, LdcI4S(42), StLoc0, BrS(0), LdLoc0, Ret),
      List(
        Nop,
        Call(MethodDefData("answer")),
        StLoc0,
        Call(MethodDefData("secretAnswer")),
        StLoc1,
        LdLoc0,
        LdLoc1,
        Call(MethodDefData("sum")),
        StLoc2,
        NewObj(MethodDefData(".ctor")),
        StLoc3,
        LdLoc3,
        CallVirt(MethodDefData("personalAnswer")),
        StLocS(4),
        LdLoc3,
        CallVirt(MethodDefData("personalSecretAnswer")),
        StLocS(5),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }

  "objects" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("objects.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored, Ignored, Ignored),
      List(FieldData(1, "a", hex"0x0608"), FieldData(1, "b", hex"0x0608")),
      List(MethodDefData(".ctor"),
           MethodDefData("answerA"),
           MethodDefData(".ctor"),
           MethodDefData("answerB"),
           MethodDefData("Main"),
           MethodDefData(".ctor")),
      List(Ignored, Ignored),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(LdArg0,
           Call(MemberRefData(41, ".ctor", hex"0x200001")),
           Nop,
           Nop,
           LdArg0,
           LdArg1,
           StFld(FieldData(1, "a", hex"0x0608")),
           Ret),
      List(Nop, LdArg0, LdFld(FieldData(1, "a", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(LdArg0,
           Call(MemberRefData(41, ".ctor", hex"0x200001")),
           Nop,
           Nop,
           LdArg0,
           LdArg1,
           StFld(FieldData(1, "b", hex"0x0608")),
           Ret),
      List(Nop, LdArg0, LdFld(FieldData(1, "b", hex"0x0608")), LdcI4S(42), Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(
        Nop,
        LdcI4S(-42),
        NewObj(MethodDefData(".ctor")),
        StLoc0,
        LdcI40,
        NewObj(MethodDefData(".ctor")),
        StLoc1,
        LdLoc0,
        CallVirt(MethodDefData("answerA")),
        LdLoc1,
        CallVirt(MethodDefData("answerB")),
        Add,
        StLoc2,
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }

  "loop" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("loop.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(MethodDefData("Main"), MethodDefData(".ctor")),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored)
    )
    opCodes shouldBe List(
      List(
        Nop,
        LdcI40,
        StLoc0,
        LdcI40,
        StLoc1,
        BrS(10),
        Nop,
        LdLoc0,
        LdcI42,
        Add,
        StLoc0,
        Nop,
        LdLoc1,
        LdcI41,
        Add,
        StLoc1,
        LdLoc1,
        LdcI4S(10),
        Clt,
        StLoc2,
        LdLoc2,
        BrTrueS(-19),
        BrS(6),
        Nop,
        LdLoc0,
        LdcI42,
        Mull,
        StLoc0,
        Nop,
        LdLoc0,
        LdcI4(10000),
        Clt,
        StLoc3,
        LdLoc3,
        BrTrueS(-18),
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
    )
  }

  "if" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("if.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored),
      List(FieldData(22, "x", hex"0x0608")),
      List(MethodDefData("Main"), MethodDefData(".ctor"), MethodDefData(".cctor")),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI41,
        Clt,
        StLoc0,
        LdLoc0,
        BrFalseS(8),
        Nop,
        LdcI44,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI45,
        Cgt,
        StLoc1,
        LdLoc1,
        BrFalseS(22),
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI46,
        Cgt,
        StLoc2,
        LdLoc2,
        BrFalseS(8),
        Nop,
        LdcI47,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        Nop,
        LdSFld(FieldData(22, "x", hex"0x0608")),
        LdcI40,
        Cgt,
        StLoc3,
        LdLoc3,
        BrFalseS(10),
        Nop,
        LdcI44,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        BrS(8),
        Nop,
        LdcI45,
        StSFld(FieldData(22, "x", hex"0x0608")),
        Nop,
        Ret
      ),
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(LdcI41, StSFld(FieldData(22, "x", hex"0x0608")), Ret)
    )
  }

  "smart program" should "be parsed correctly" in {
    val Right((_, cilData, opCodes)) = parsePe("smart_program.exe")

    cilData.tables shouldBe List(
      List(Ignored),
      List(Ignored, Ignored, Ignored, Ignored, Ignored, Ignored),
      List(Ignored, Ignored, Ignored),
      List(FieldData(22, "counter", hex"0x0608"), FieldData(6, "fa", hex"0x0608"), FieldData(6, "fb", hex"0x0608")),
      List(
        MethodDefData(".ctor"),
        MethodDefData("doSmth"),
        MethodDefData("receive"),
        MethodDefData("otherFunc"),
        MethodDefData("Main"),
        MethodDefData(".ctor"),
        MethodDefData(".cctor")
      ),
      List(Ignored, Ignored, Ignored, Ignored, Ignored, Ignored, Ignored),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(41, ".ctor", hex"0x200001"),
        MemberRefData(49, ".ctor", hex"0x200001")
      ),
      List(Ignored, Ignored, Ignored, Ignored),
      List(Ignored),
      List(Ignored),
      List(Ignored)
    )

    opCodes shouldBe List(
      List(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret),
      List(Nop, LdArg0, LdArg1, Add, StLoc0, BrS(0), LdLoc0, Ret),
      List(
        Nop,
        LdArg1,
        StLoc0,
        LdLoc0,
        LdcI41,
        Sub,
        Switch(ArrayBuffer(2, 13, 24, 40)),
        BrS(66),
        LdArg0,
        LdArg2,
        LdArg3,
        Add,
        StFld(FieldData(6, "fa", hex"0x0608")),
        BrS(55),
        LdArg0,
        LdArg2,
        LdArg3,
        Add,
        StFld(FieldData(6, "fb", hex"0x0608")),
        BrS(44),
        LdArg0,
        LdArg2,
        StFld(FieldData(6, "fa", hex"0x0608")),
        LdArg0,
        LdArg3,
        StFld(FieldData(6, "fb", hex"0x0608")),
        BrS(28),
        LdArg0,
        LdArg2,
        LdArg3,
        Call(MethodDefData("doSmth")),
        StFld(FieldData(6, "fa", hex"0x0608")),
        LdArg0,
        LdArg2,
        LdArg3,
        Call(MethodDefData("doSmth")),
        StFld(FieldData(6, "fb", hex"0x0608")),
        BrS(0),
        Ret
      ),
      List(Nop, Ret),
      List(Nop, Ret),
      List(
        LdArg0,
        LdcI40,
        StFld(FieldData(6, "fa", hex"0x0608")),
        LdArg0,
        LdcI40,
        StFld(FieldData(6, "fb", hex"0x0608")),
        LdArg0,
        Call(MemberRefData(49, ".ctor", hex"0x200001")),
        Nop,
        Ret
      ),
      List(LdcI40, StSFld(FieldData(22, "counter", hex"0x0608")), Ret)
    )
  }
}
