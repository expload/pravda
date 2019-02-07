using System;
using Expload.Pravda;

namespace ExternalNamespace {

    [Program]
    public class ExternalMethodsCheck
    {
        public int TestStaticMethods()
        {
            ExternalProgramMethods epm = ExternalProgramMethods.GetInstance();
            int a = epm.Add(2, 2);
            int b = epm.Add(10, 10);
            int c = epm.Add(300, 300);

            return a + b + c;
        }

        public int TestRegularMethods()
        {
            ExternalMethods em = new ExternalMethods(3, 3);
            int a = em.Add();
            int b = em.Add(100);
            int c = ExternalMethods.Add(1000, 1000);

            return a + b + c;
        }

        public static void Main() {}
    }
}