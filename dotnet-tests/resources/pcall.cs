using System;
using Expload.Pravda;

namespace Expload.Pravda.Programs
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