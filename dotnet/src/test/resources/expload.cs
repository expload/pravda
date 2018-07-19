using System;

namespace Com.Expload {
    public class Program : Attribute {}

    // access to the storage
    public class Mapping<K, V> {
       public V get(K key) { return default(V); }
       public bool exists(K key) { return false; }
       public void put(K key, V value) { return; }
       public V getDefault(K key, V def) { return default(V); }
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
