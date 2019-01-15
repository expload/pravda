using System;
using Expload.Pravda;

namespace PcallNamespace {

    [Program]
    public class Callers
    {
        public Bytes TestCallers()
        {
           return Info.Callers()[0];
        }

        public static void Main() {}
    }
}