using System;
using System.Collections;
using Expload.Unity.Codegen;

namespace Expload.Pravda.ZooProgram
{
    public class NewPetRequest: ProgramRequest<string>
    {
        public NewPetRequest(byte[] programAddress) : base(programAddress) { }

        protected override string ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseUtf8(elem);
        }

        public IEnumerator Test(int arg0)
        {
            yield return SendRequest("NewPet", new string[] { ExploadTypeConverters.PrintInt32(arg0) }, true);
        }

        public IEnumerator Call(int arg0)
        {
            yield return SendRequest("NewPet", new string[] { ExploadTypeConverters.PrintInt32(arg0) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator NewPet(int arg0)
        {
            yield return SendRequest("NewPet", new string[] { ExploadTypeConverters.PrintInt32(arg0) }, false);
        }
    }
    public class TransferPetRequest: ProgramRequest<object>
    {
        public TransferPetRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, int arg1, string arg2)
        {
            yield return SendRequest("TransferPet", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1), ExploadTypeConverters.PrintUtf8(arg2) }, true);
        }

        public IEnumerator Call(byte[] arg0, int arg1, string arg2)
        {
            yield return SendRequest("TransferPet", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1), ExploadTypeConverters.PrintUtf8(arg2) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator TransferPet(byte[] arg0, int arg1, string arg2)
        {
            yield return SendRequest("TransferPet", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1), ExploadTypeConverters.PrintUtf8(arg2) }, false);
        }
    }
    public class BreedPetsRequest: ProgramRequest<string>
    {
        public BreedPetsRequest(byte[] programAddress) : base(programAddress) { }

        protected override string ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseUtf8(elem);
        }

        public IEnumerator Test(string arg0, string arg1)
        {
            yield return SendRequest("BreedPets", new string[] { ExploadTypeConverters.PrintUtf8(arg0), ExploadTypeConverters.PrintUtf8(arg1) }, true);
        }

        public IEnumerator Call(string arg0, string arg1)
        {
            yield return SendRequest("BreedPets", new string[] { ExploadTypeConverters.PrintUtf8(arg0), ExploadTypeConverters.PrintUtf8(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator BreedPets(string arg0, string arg1)
        {
            yield return SendRequest("BreedPets", new string[] { ExploadTypeConverters.PrintUtf8(arg0), ExploadTypeConverters.PrintUtf8(arg1) }, false);
        }
    }
    public class TransferZooRequest: ProgramRequest<object>
    {
        public TransferZooRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, int arg1)
        {
            yield return SendRequest("TransferZoo", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, int arg1)
        {
            yield return SendRequest("TransferZoo", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator TransferZoo(byte[] arg0, int arg1)
        {
            yield return SendRequest("TransferZoo", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }
    }
    public class NewZooRequest: ProgramRequest<int>
    {
        public NewZooRequest(byte[] programAddress) : base(programAddress) { }

        protected override int ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseInt32(elem);
        }

        public IEnumerator Test()
        {
            yield return SendRequest("NewZoo", new string[] {  }, true);
        }

        public IEnumerator Call()
        {
            yield return SendRequest("NewZoo", new string[] {  }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator NewZoo()
        {
            yield return SendRequest("NewZoo", new string[] {  }, false);
        }
    }
}