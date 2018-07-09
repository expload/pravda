using System;
using io.mytc.pravda;

namespace io.mytc.pravda {

    // our special attribute, it will be a simple library
    public class Program : Attribute {}

    // access to the storage
    public abstract class Mapping<K, V> {
       public abstract V get(K key);
       public abstract bool exists(K key);
       public abstract void put(K key, V value);

       public V getDefault(K key, V def) {
          if (!this.exists(key)) {
             return def;
          } else {
             return this.get(key);
          }
       }
    }

    public class Address {}
    public class Data {}
    public class Word {}

    public class Bytes {
       public Bytes(params byte[] bytes) {}

       public byte this[int i] { get { return 0; } set { return; } }
       public Bytes Slice(int start, int length) { return null; }
    }
}

public class HelloWorld
{
    public Mapping<Bytes, Bytes> bytes;

    public void WorkWithBytes() {
        byte[] arr1 = new byte[] {1, 2, 3};
        Bytes bytes1 = new Bytes(4, 5, 6);
        Bytes bytes2 = new Bytes(7, 8, 9);

        byte b1 = arr1[0];
        byte b2 = arr1[2];
        byte b3 = bytes1[1];
        byte b4 = bytes2[1];

        Bytes bytes3 = bytes1.Slice(1, 2);

        bytes.put(bytes1, bytes2);
        if (bytes.exists(new Bytes(8, 9, 10))) {
          bytes.put(bytes1, new Bytes(7, 8, 9));
        }

        arr1[0] = 2;
        arr1[1] = 1;
    }

    public void WorkWithArrays() {
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
    }

    static public void Main ()
    {
    }
}