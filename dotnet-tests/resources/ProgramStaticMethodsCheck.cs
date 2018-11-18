using System;
using Expload.Pravda;

namespace ProgramNamespace {

    [Program]
    public class ProgramStaticMethodsCheck
    {
        public int TestStaticMethods()
        {
            var psm = ProgramStaticMethods.GetInstance();
            int a = psm.Add(2, 2);
            int b = psm.Add(10, 10);
            int c = psm.Add(300, 300);

            return a + b + c;
        }

        public static void Main() {}
    }
}