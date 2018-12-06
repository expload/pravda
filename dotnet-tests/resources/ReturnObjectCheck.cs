using System;
using Expload.Pravda;

namespace ReturnObjectNamespace {

    [Program]
    public class ReturnObjectCheck
    {
        public int TestReturnObject()
        {
           Bytes address = new Bytes("123456789012345678901234567890123456789012345678901234567890ABCD");
           return ProgramHelper.Program<ReturnObject>(address).GetObject().SomeField;
        }

        public static void Main() {}
    }
}
