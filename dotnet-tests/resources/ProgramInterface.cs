using System;
using Expload.Pravda;

namespace InterfaceNamespace {

    [Program]
    public interface ProgramInterface
    {
        int Add(int a, int b);
    }

    [Program]
    public class ProgramInterfaceImpl : ProgramInterface
    {
        public int Add(int a, int b)
        {
            return a + b;
        }

        public static void Main() {}
    }
}