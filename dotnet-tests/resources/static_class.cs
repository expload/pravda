using Expload.Pravda;
using System;

namespace Expload.Pravda
{
    public static class StringUtils {
        private static string HexPart(int b)
        {
            if (b == 0)
                return "0";
            else if (b == 1)
                return "1";
            else if (b == 2)
                return "2";
            else if (b == 3)
                return "3";
            else if (b == 4)
                return "4";
            else if (b == 5)
                return "5";
            else if (b == 6)
                return "6";
            else if (b == 7)
                return "7";
            else if (b == 8)
                return "8";
            else if (b == 9)
                return "9";
            else if (b == 10)
                return "A";
            else if (b == 11)
                return "B";
            else if (b == 12)
                return "C";
            else if (b == 13)
                return "D";
            else if (b == 14)
                return "E";
            else if (b == 15)
                return "F";
            return "";
        }

        public static string ByteToHex(byte b)
        {
            return HexPart(b / 16) + HexPart(b % 16);
        }

        public static string BytesToHex(Bytes bytes)
        {
            string res = "";
            for (int i = 0; i < bytes.Length(); i++) {
                res += ByteToHex(bytes[i]);
            }
            return res;
        }
    }
}

[Program]
class MyProgram {
    public string ToHex(Bytes bs) {
        return StringUtils.BytesToHex(bs);
    }

    public static void Main() {}
}
