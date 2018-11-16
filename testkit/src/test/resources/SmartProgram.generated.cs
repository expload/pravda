using System;
using System.Collections;
using Expload.Unity.Codegen;

namespace Expload.Pravda.SmartProgram
{
    public class BalanceOfRequest: ProgramRequest<int>
    {
        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override int ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseInt32(elem);
        }

        public IEnumerator Test(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, true);
        }

        public IEnumerator Call(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator BalanceOf(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, false);
        }
    }
    public class EmitRequest: ProgramRequest<object>
    {
        public EmitRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, int arg1)
        {
            yield return SendRequest("Emit", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, int arg1)
        {
            yield return SendRequest("Emit", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator Emit(byte[] arg0, int arg1)
        {
            yield return SendRequest("Emit", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }
    }
    public class TransferRequest: ProgramRequest<object>
    {
        public TransferRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, int arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, int arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator Transfer(byte[] arg0, int arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) }, false);
        }
    }
}