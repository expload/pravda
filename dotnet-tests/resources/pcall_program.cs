using System;
using Expload.Pravda;

// this interface is obligatory for programs that are called with ProgramHelper.Program<...>
namespace Expload.Pravda.Programs
{
    [Program]
    public class MyAnotherProgram {
        public int Add(int a, int b)
        {
            return a + b;
        }

        public static void Main() {}
    }
}