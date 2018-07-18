using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

namespace Com.Expload.ERC20 {
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
        public byte[] ProgramAddress { get; protected set; }

        public T Result { get; protected set; }
        public string Error { get; protected set; }
        public bool IsError { get; protected set; }

        protected ProgramRequest(byte[] programAddress)
        {
            ProgramAddress = programAddress;
            IsError = false;
            Error = "";
        }

        protected abstract T ParseResult(string json);

        protected IEnumerator SendJson(string json)
        {
            UnityWebRequest www = UnityWebRequest.Put("localhost:8087/api/program/method", json);
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

        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override uint ParseResult(string json)
        {
            return UintResult.FromJson(json).value;
        }

        public IEnumerator BalanceOf(byte[] tokenOwner)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"balanceOf\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bytes\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + BitConverter.ToString(tokenOwner).Replace("-","") + "\"" );
            yield return SendJson(json);
        }
    }
}