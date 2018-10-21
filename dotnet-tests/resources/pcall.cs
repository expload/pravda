using System;
using Expload.Pravda;

namespace Expload.Pravda.Programs
{
    [Program]
    public class MyProgram {
        public int pcall() {
            byte[] address = new byte[] {30, 174, 210, 11, 124, 226, 179, 54, 4, 62, 75, 52, 11, 3, 31, 149, 187, 28, 230, 217, 53, 239, 115, 58, 228, 223, 27, 102, 225, 227, 217, 31};
            int res = ProgramHelper.Program<MyAnotherProgram>(new Bytes(address)).Add(10, 20);
            return res;
        }

        public static void Main() {}
    }
}
