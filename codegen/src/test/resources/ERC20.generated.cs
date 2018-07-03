using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;
using Keiwando.BigInteger;

namespace Io.Mytc.ERC20 {
    [System.Serializable]
    class UintResult {
       public uint value;
       public string tpe;

       public static UintResult FromJson(string json) {
           return JsonUtility.FromJson<UintResult>(json);
       }
    }

    abstract class ProgramRequest<T>
    {
        public BigInteger ProgramAddress { get; protected set; }

        public T Result { get; protected set; }
        public string Error { get; protected set; }
        public bool IsError { get; protected set; }

        protected ProgramRequest(BigInteger programAddress)
        {
            ProgramAddress = programAddress;
            IsError = false;
            Error = "";
        }

        protected abstract T ParseResult(string json);

        protected IEnumerator SendJson(string json)
        {
            UnityWebRequest www = UnityWebRequest.Put("localhost:8080/program/method", json);
            www.method = "POST";
            www.SetRequestHeader("Content-Type", "application/json");

            yield return www.SendWebRequest();

            if (www.isNetworkError || www.isHttpError)
            {
                IsError = true;
                Error = www.error;
            }
            else
            {
                try
                {
                    Result = ParseResult(www.downloadHandler.text);
                }
                catch (ArgumentException e)
                {
                    IsError = true;
                    Error = "Invalid JSON: " + www.downloadHandler.text + "\n" + e.Message;
                }
            }
        }
    }

    class BalanceOfRequest: ProgramRequest<uint> {

        public BalanceOfRequest(BigInteger programAddress) : base(programAddress) { }

        protected override uint ParseResult(string json)
        {
            return UintResult.FromJson(json).value;
        }

        public IEnumerator BalanceOf(BigInteger tokenOwner)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"balanceOf\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + tokenOwner.ToHexString() + "\"" );
            yield return SendJson(json);
        }
    }
    class AllowanceRequest: ProgramRequest<uint> {

        public AllowanceRequest(BigInteger programAddress) : base(programAddress) { }

        protected override uint ParseResult(string json)
        {
            return UintResult.FromJson(json).value;
        }

        public IEnumerator Allowance(BigInteger tokenOwner, BigInteger spender)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"allowance\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}, {{ \"value\": {2}, \"tpe\": \"bigint\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + tokenOwner.ToHexString() + "\"" ,  "\"" + spender.ToHexString() + "\"" );
            yield return SendJson(json);
        }
    }
    class TransferRequest: ProgramRequest<object> {

        public TransferRequest(BigInteger programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Transfer(BigInteger to, uint tokens)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"transfer\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}, {{ \"value\": {2}, \"tpe\": \"uint32\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + to.ToHexString() + "\"" , tokens);
            yield return SendJson(json);
        }
    }
    class ApproveRequest: ProgramRequest<object> {

        public ApproveRequest(BigInteger programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Approve(BigInteger spender, uint tokens)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"approve\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}, {{ \"value\": {2}, \"tpe\": \"uint32\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + spender.ToHexString() + "\"" , tokens);
            yield return SendJson(json);
        }
    }
    class TransferFromRequest: ProgramRequest<object> {

        public TransferFromRequest(BigInteger programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator TransferFrom(BigInteger from, BigInteger to, uint tokens)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"transferFrom\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}, {{ \"value\": {2}, \"tpe\": \"bigint\" }}, {{ \"value\": {3}, \"tpe\": \"uint32\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + from.ToHexString() + "\"" ,  "\"" + to.ToHexString() + "\"" , tokens);
            yield return SendJson(json);
        }
    }
}