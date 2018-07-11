using System;

namespace Com.Expload {

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
