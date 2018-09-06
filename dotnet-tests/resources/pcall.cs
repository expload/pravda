using System;
using Com.Expload;

namespace Com.Expload.Programs
{
    [Program]
    public class MyProgram {
        public int pcall() {
            int res = ProgramHelper.Program<MyAnotherProgram>(Bytes.VOID_ADDRESS).Add(10, 20);
            return res;
        }

        public static void Main() {}
    }
}