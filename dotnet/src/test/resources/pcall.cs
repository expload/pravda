using System;
using Com.Expload;

namespace Com.Expload.Programs
{
    [Program]
    public class MyProgram {
        public int scall() {
            int res = ProgramHelper.Program<MyAnotherProgram>(new Bytes(1, 2, 3, 4)).Add(10, 20);
            return res;
        }

        public static void Main() {}
    }
}