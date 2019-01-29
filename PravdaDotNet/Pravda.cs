using System;
using System.Collections.Generic;
using System.Text;

// WARNING
// Purpose of this dll is only to marks specific methods for translator.
// Translator detects them and generates necessary bytecode.
namespace Expload.Pravda
{
    namespace Sandbox
    {
        public static class SandboxValues
        {
            public static List<Tuple<string, object>> Events = new List<Tuple<string, object>>();
            public static Dictionary<Bytes, long> Balances = new Dictionary<Bytes, long>();
            public static Bytes Sender = Bytes.VOID_ADDRESS;
            public static Bytes ProgramAddress = Bytes.VOID_ADDRESS;
            public static long Height = 0L;
            public static Bytes LastBlockHash = Bytes.VOID_ADDRESS;
            public static long LastBlockTime = 0L;
            public static Bytes[] Callers = new Bytes[0];
        }
    }

    public class PravdaException : Exception
    {
       public PravdaException(string message) : base(message) {}
    }

    public class NotEnoughMoney : PravdaException
    {
       public NotEnoughMoney(Bytes address) : base(String.Format("Not enough money on {0}", StdLib.BytesToHex(address))) {}
    }

    // Special attribute to mark classes to be translated to Pravda program
    public class Program : Attribute {}

    // Access to the storage
    public class Mapping<K, V>
    {
        private Dictionary<K, V> mapping = new Dictionary<K, V>();
        
        public V this[K k]
        {
            get { 
                return mapping[k];
            } 
            set {
                mapping[k] = value;
            }
        }

        // Checks the specified key is present in the mapping.
        public bool ContainsKey(K key)
        {
            return mapping.ContainsKey(key);
        }
        
        // Gets the value associated with the specified key.
        // If key is not fount returns defaultValue.
        public V GetOrDefault(K key, V defaultValue)
        {
            if (mapping.ContainsKey(key)) {
                return mapping[key];
            } else {
                return defaultValue;
            }
        }
    }

    public static class Info {
        // Get address of the sender user
        public static Bytes Sender()
        {
            return Sandbox.SandboxValues.Sender;
        }

        // Get balance of given address
        public static long Balance(Bytes address)
        {
            return Sandbox.SandboxValues.Balances[address];
        }

        // Get program address
        public static Bytes ProgramAddress()
        {
            return Sandbox.SandboxValues.ProgramAddress;
        }

        // Get list of contract's callers' addresses
        public static Bytes[] Callers()
        {
            return Sandbox.SandboxValues.Callers;
        }

        public static long Height()
        {
            return Sandbox.SandboxValues.Height;
        }
        public static Bytes LastBlockHash()
        {
            return Sandbox.SandboxValues.LastBlockHash;
        }
        // Get the timestamp of the last comitted block
        // Timestamp is a number of milliseconds since 1 Jan 1970 00:00 UTC+0
        public static long LastBlockTime()
        {
            return Sandbox.SandboxValues.LastBlockTime;
        }
    }

    public class Actions {
        // Transfer native coins from executor account
        public static void Transfer(Bytes to, long amount)
        {
            long senderBalance;
            if (Sandbox.SandboxValues.Balances.ContainsKey(Info.Sender())) {
                senderBalance = Sandbox.SandboxValues.Balances[Info.Sender()];
            } else {
                senderBalance = 0L;
            }

            long toBalance;
            if (Sandbox.SandboxValues.Balances.ContainsKey(to)) {
                toBalance = Sandbox.SandboxValues.Balances[to];
            } else {
                toBalance = 0L;
            }

            if (senderBalance < amount) {
                throw new NotEnoughMoney(Info.Sender());
            } else {
                Sandbox.SandboxValues.Balances[Info.Sender()] = senderBalance - amount;
                Sandbox.SandboxValues.Balances[to] = toBalance + amount;
            }
        }

        // Transfer native coins from program account
        public static void TransferFromProgram(Bytes to, long amount)
        {
            long programBalance;
            if (Sandbox.SandboxValues.Balances.ContainsKey(Info.ProgramAddress())) {
                programBalance = Sandbox.SandboxValues.Balances[Info.ProgramAddress()];
            } else {
                programBalance = 0L;
            }

            long toBalance;
            if (Sandbox.SandboxValues.Balances.ContainsKey(to)) {
                toBalance = Sandbox.SandboxValues.Balances[to];
            } else {
                toBalance = 0L;
            }

            if (programBalance < amount) {
                throw new NotEnoughMoney(Info.ProgramAddress());
            } else {
                Sandbox.SandboxValues.Balances[Info.ProgramAddress()] = programBalance - amount;
                Sandbox.SandboxValues.Balances[to] = toBalance + amount;
            }
        }
    }

    public static class Log {
        public static void Event<T>(string name, T data) {
            Sandbox.SandboxValues.Events.Add(Tuple.Create(name, (object)data));
        }
    }

    public static class Error {
        public static void Throw(String message)
        {
            throw new PravdaException(message);
        }
    }

    // Immutable array of bytes
    public class Bytes {
        // NOT TRANSLATED TO PRAVDA VM
        public sbyte[] bytes { get; }

        // Get the empty byte array
        public static Bytes EMPTY = new Bytes(new sbyte[0]);
        // Get the void address (32 zero bytes)
        public static Bytes VOID_ADDRESS = new Bytes(new sbyte[32]);

        public Bytes(params sbyte[] bytes)
        {
            this.bytes = bytes;
        }
        public Bytes(String hex)
        {
            this.bytes = StdLib.HexToBytes(hex).bytes;
        }

        // Get the i-th byte
        public sbyte this[int i]
        {
            get {
                return bytes[i];
            }
        }
        // Get the sub-array
        public Bytes Slice(int start, int length)
        {
            sbyte[] newBytes = new sbyte[length];
            Array.Copy(bytes, start, newBytes, 0, length);
            return new Bytes(newBytes);
        }
        // Concatenate two Bytes
        public Bytes Concat(Bytes other)
        {
            int length = this.Length() + other.Length();
            sbyte[] newBytes = new sbyte[length];
            Array.Copy(bytes, 0, newBytes, 0, this.Length());
            Array.Copy(other.bytes, 0, newBytes, this.Length(), other.Length());
            return new Bytes(newBytes);
        }
        // Length of byte array
        public int Length()
        {
            return bytes.Length;
        }


        // NOT TRANSLATED TO PRAVDA VM
        public override int GetHashCode() {
            int hash = 17;
            foreach (sbyte elem in bytes)
            {
               hash = hash * 31 + elem.GetHashCode();
            };

            return hash;
        }
        // NOT TRANSLATED TO PRAVDA VM
        public override bool Equals(object obj) {
            return Equals(obj as Bytes);
        }
        // NOT TRANSLATED TO PRAVDA VM
        public bool Equals(Bytes other) {
            if (other == null) {
                return false;
            }

            if (this.Length() != other.Length()) {
                return false;
            }

            for (int i = 0; i < this.Length(); i++) {
                if (this[i] != other[i]) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class StdLib {

        public static Bytes Ripemd160(Bytes bytes)
        {
            throw new PravdaException("RIPEMD160 hashing is not supported");
        }

        public static Bytes Ripemd160(String str)
        {
            throw new PravdaException("RIPEMD160 hashing is not supported");
        }

        public static Bytes HexToBytes(String hex)
        {
            if (hex.Length % 2 != 0)
            {
                throw new ArgumentException(
                    String.Format("The hex string cannot have an odd number of digits: {0}", hex)
                );
            }

            sbyte[] data = new sbyte[hex.Length / 2];
            for (int index = 0; index < data.Length; index++)
            {
                string byteValue = hex.Substring(index * 2, 2);
                data[index] = sbyte.Parse(byteValue);
            }

            return new Bytes(data);
        }

        public static String BytesToHex(Bytes bytes)
        {
            return BitConverter.ToString((byte[])(Array)bytes.bytes).Replace("-", "");
        }

        public static bool ValidateEd25519Signature(Bytes pubKey, Bytes message, Bytes signature)
        {
            throw new PravdaException("ED25519 signatures are not supported");
        }

        public static bool ValidateEd25519Signature(Bytes pubKey, String message, Bytes signature)
        {
            throw new PravdaException("ED25519 signatures are not supported");
        }
    }

    public static class ProgramHelper {
        public static T Program<T>(Bytes address)
        {
            throw new PravdaException("Calling other programs is not supported");
        }
    }
}
