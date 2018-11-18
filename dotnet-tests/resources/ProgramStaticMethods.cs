using System;
using Expload.Pravda;

namespace ProgramNamespace {

    [Program]
    public class ProgramStaticMethods
    {
        public static ProgramStaticMethods GetInstance()
        {
            return ProgramHelper.Program<ProgramStaticMethods>(new Bytes(
                "123456789012345678901234567890123456789012345678901234567890ABCD"
            ));
        }

        public int Add(int a, int b)
        {
            return a + b;
        }

        public static void Main() {}
    }
}