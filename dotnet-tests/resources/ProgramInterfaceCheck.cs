using System;
using Expload.Pravda;

namespace InterfaceNamespace {

    [Program]
    public class ProgramInterfaceCheck
    {
        public int CheckInterface()
        {
           Bytes address = new Bytes("1eaed20b7ce2b336043e4b340b031f95bb1ce6d935ef733ae4df1b66e1e3d91f");
           return ProgramHelper.Program<ProgramInterface>(address).Add(1, 2);
        }

        public static void Main() {}
    }
}
