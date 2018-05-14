package io.mytc.sood.cil

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.PE.Info._
import io.mytc.sood.cil.TablesData._
import io.mytc.sood.cil.TablesInfo._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

// all *.exe files was compiled by csc *.cs

class MicsSpec extends FlatSpec with Matchers {

  "hello_world.exe" should "be parsed correctly" in {
    val Right((pe, cilData, opCodes)) = PeParsersUtils.parsePe("hello_world.exe")

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
        TablesInfo(
          List(),
          ArrayBuffer(MemberRefRow(9, 170, 1),
                      MemberRefRow(17, 170, 6),
                      MemberRefRow(25, 170, 10),
                      MemberRefRow(49, 50, 16),
                      MemberRefRow(41, 170, 6)),
          ArrayBuffer(MethodDefRow(8272, 0, 150, 165, 30, 1), MethodDefRow(8286, 0, 6278, 170, 6, 1)),
          List(),
          ArrayBuffer(TypeDefRow(0, 1, 0, 0, 1, 1), TypeDefRow(1048577, 19, 0, 21, 1, 1))
        )
      ),
      Seq(TinyMethodHeader(hex"0x007201000070280400000a002a"), TinyMethodHeader(hex"0x02280500000a002a"))
    )

    cilData.tables shouldBe TablesData(
      List(),
      List(
        MemberRefData(9, ".ctor", hex"0x20010108"),
        MemberRefData(17, ".ctor", hex"0x200001"),
        MemberRefData(25, ".ctor", hex"0x2001011111"),
        MemberRefData(49, "WriteLine", hex"0x0001010e"),
        MemberRefData(41, ".ctor", hex"0x200001")
      ),
      List(MethodDefData(0, 150, "Main", hex"0x000001", List()),
           MethodDefData(0, 6278, ".ctor", hex"0x200001", List())),
      List(),
      List(
        TypeDefData(0, "<Module>", "", Ignored, List(), List(MethodDefData(0, 150, "Main", hex"0x000001", List()))),
        TypeDefData(1048577,
                    "HelloWorld",
                    "",
                    Ignored,
                    List(),
                    List(MethodDefData(0, 150, "Main", hex"0x000001", List())))
      )
    )

    opCodes shouldBe
      Seq(
        Seq(Nop, LdStr("Hello World!"), Call(MemberRefData(49, "WriteLine", hex"0x0001010e")), Nop, Ret),
        Seq(LdArg0, Call(MemberRefData(41, ".ctor", hex"0x200001")), Nop, Ret)
      )
  }
}
