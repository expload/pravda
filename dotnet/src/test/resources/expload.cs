using System;

namespace Com.Expload {
    public class Program : Attribute {}

    // access to the storage
    public abstract class Mapping<K, V> {
       public abstract V get(K key);
       public abstract bool exists(K key);
       public abstract void put(K key, V value);
       public abstract V getDefault(K key, V def);
    }

    public class Info {
        public static Bytes Sender() { return null; }
    }

    public class Bytes {
       public static Bytes EMPTY = null;

       public Bytes(params byte[] bytes) {}

       public byte this[int i] { get { return 0; } set { return; } }
       public Bytes Slice(int start, int length) { return null; }
       public Bytes Concat(Bytes other) { return null; }
    }
}
