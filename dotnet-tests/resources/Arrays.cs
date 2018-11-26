using System;
using Expload.Pravda;

[Program]
public class Arrays
{
    public Mapping<Bytes, Bytes> BytesMapping = new Mapping<Bytes, Bytes>();

    public void TestByteArrays() {
        byte[] arr1 = new byte[] {1, 2, 3};
        Bytes bytes1 = new Bytes(4, 5, 6);
        Bytes bytes2 = new Bytes(7, 8, 9);

        byte b1 = arr1[0];
        byte b2 = arr1[2];
        byte b3 = bytes1[1];
        byte b4 = bytes2[1];

        Bytes bytes3 = bytes1.Slice(1, 2);

        BytesMapping[bytes1] = bytes2;
        if (BytesMapping.ContainsKey(new Bytes(8, 9, 10))) {
          BytesMapping[bytes1] = new Bytes(7, 8, 9);
        }

        arr1[0] = 2;
        arr1[1] = 1;

        int len = bytes1.Length();
    }

    public void TestAllArrays() {
        char[] chars = new char[] { 'a', 'b', 'c' };
        int[] ints = new int[] { 1, 2, 3 };
        double[] doubles = new double[] { 1.0, 2.0, 3.0 };
        string[] strings = new string[] { "abc", "def", "rty" };
        uint[] uints = new uint[] { 4, 5, 6 };

        chars[1] = 'd';
        ints[1] = 4;
        doubles[1] = 4.0;
        strings[1] = "asdf";
        uints[1] = 7;

        int len = strings.Length;
    }

    static public void Main () {}
}