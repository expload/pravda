using System;
using Expload.Pravda;

namespace PcallNamespace {

    [Program]
    public class Pcall
    {
        public int TestPcall()
        {
           Bytes address1 = new Bytes("1eaed20b7ce2b336043e4b340b031f95bb1ce6d935ef733ae4df1b66e1e3d91f");
           int a = ProgramHelper.Program<PcallProgram>(address1).Add(1, 2);
           Bytes address2 = new Bytes(new sbyte[] {30, -82, -46, 11, 124, -30, -77, 54, 4, 62, 75, 52, 11, 3, 31, -107, -69, 28, -26, -39, 53, -17, 115, 58, -28, -33, 27, 102, -31, -29, -39, 31});
           int b = ProgramHelper.Program<PcallProgram>(address2).Add(3, 4);
           return a + b;
        }

        public static void Main() {}
    }
}
