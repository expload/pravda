using System;
using Expload.Pravda;

namespace ReturnObjectNamespace {

    public class SomeObject
    {
        public int SomeField;

        public SomeObject(int someField)
        {
            SomeField = someField;
        }
    }

    [Program]
    public class ReturnObject
    {
        public SomeObject GetObject()
        {
            return new SomeObject(42);
        }

        public static void Main() {}
    }
}
