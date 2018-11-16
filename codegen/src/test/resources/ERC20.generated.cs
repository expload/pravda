using System;
using System.Collections;
using Expload.Unity.Codegen;

namespace Expload.Pravda.ERC20
{
    public class BalanceOfRequest: ProgramRequest<uint>
    {
        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override uint ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseUInt32(elem);
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
    public class AllowanceRequest: ProgramRequest<uint>
    {
        public AllowanceRequest(byte[] programAddress) : base(programAddress) { }

        protected override uint ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseUInt32(elem);
        }

        public IEnumerator Test(byte[] arg0, byte[] arg1)
        {
            yield return SendRequest("Allowance", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, byte[] arg1)
        {
            yield return SendRequest("Allowance", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator Allowance(byte[] arg0, byte[] arg1)
        {
            yield return SendRequest("Allowance", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1) }, false);
        }
    }
    public class TransferRequest: ProgramRequest<object>
    {
        public TransferRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator Transfer(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, false);
        }
    }
    public class ApproveRequest: ProgramRequest<object>
    {
        public ApproveRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Approve", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, true);
        }

        public IEnumerator Call(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Approve", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator Approve(byte[] arg0, uint arg1)
        {
            yield return SendRequest("Approve", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintUInt32(arg1) }, false);
        }
    }
    public class TransferFromRequest: ProgramRequest<object>
    {
        public TransferFromRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseNull(elem);
        }

        public IEnumerator Test(byte[] arg0, byte[] arg1, uint arg2)
        {
            yield return SendRequest("TransferFrom", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1), ExploadTypeConverters.PrintUInt32(arg2) }, true);
        }

        public IEnumerator Call(byte[] arg0, byte[] arg1, uint arg2)
        {
            yield return SendRequest("TransferFrom", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1), ExploadTypeConverters.PrintUInt32(arg2) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator TransferFrom(byte[] arg0, byte[] arg1, uint arg2)
        {
            yield return SendRequest("TransferFrom", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintBytes(arg1), ExploadTypeConverters.PrintUInt32(arg2) }, false);
        }
    }
}