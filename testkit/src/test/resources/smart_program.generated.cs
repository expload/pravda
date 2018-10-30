using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;
using Newtonsoft.Json;

namespace Expload.Pravda.Program
{
    public static class ExploadTypeConverters
    {
        public static int ParseInt32(string elem)
        {
            if (elem.StartsWith("int32.")) {
                return int.Parse(elem.Substring("int32.".Length));
            } else {
                throw new ArgumentException("Wrong format for int32 type: " + elem);
            }
        }

        public static string PrintInt32(int elem)
        {
            return "int32." + elem.ToString();
        }

        public static byte[] ParseBytes(string elem)
        {
            if (elem.StartsWith("bytes."))
            {
                string hex = elem.Substring("bytes.".Length);
                byte[] bytes = new byte[hex.Length / 2];
                for (int i = 0; i < hex.Length; i += 2) {
                    bytes[i / 2] = Convert.ToByte(hex.Substring(i, 2), 16);
                }
                return bytes;
            }
            else
            {
                throw new ArgumentException("Wrong format for int32 type: " + elem);
            }
        }

        public static string PrintBytes(byte[] elem)
        {
            return "bytes." + BitConverter.ToString(elem).Replace("-", "");
        }
    }

    public class ExploadMethodRequest
    {
        public string address { get; set; }
        public string method { get; set; }
        public string[] args { get; set; }

        public ExploadMethodRequest(string address, string method, string[] args)
        {
            this.address = address;
            this.method = method;
            this.args = args;
        }
    }

    public class ExploadResponseFinalState
    {
        public long spentWatts { get; set; }
        public long refundWatts { get; set; }
        public long totalWatts { get; set; }
        public string[] stack { get; set; }
    }

    public class ExploadResponseData
    {
        public ExploadResponseFinalState finalState { get; set; }
    }

    public class ExploadResponse
    {
        public string error { get; set; }
        public string errorCode { get; set; }
        public ExploadResponseData data { get; set; }
    }

    public abstract class ProgramRequest<T>
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

        protected abstract T ParseResult(string elem);

        protected IEnumerator SendRequest(string method, string[] args)
        {
            var request = new ExploadMethodRequest(BitConverter.ToString(ProgramAddress).Replace("-", ""), method, args);
            string json = JsonConvert.SerializeObject(request);
            Debug.Log(json);
            UnityWebRequest www = UnityWebRequest.Put("localhost:8087/api/program/method", json);
            www.method = "POST";
            www.SetRequestHeader("Content-Type", "application/json");

            yield return www.SendWebRequest();

            if (www.isNetworkError || www.isHttpError)
            {
                IsError = true;
                Error = www.error + "\nHttp code: " + www.responseCode + "\nText: " + www.downloadHandler.text;
            }
            else
            {
                try
                {
                    Debug.Log(www.downloadHandler.text);
                    var response = JsonConvert.DeserializeObject<ExploadResponse>(www.downloadHandler.text);
                    if (response.error.Length != 0) {
                        IsError = true;
                        Error = "Error from response: " + response.error;
                    } else if (response.data.finalState.stack.Length > 1) {
                        IsError = true;
                        Error = "Invalid method result:\n[" + String.Join(", ", response.data.finalState.stack) + "]";
                    }
                    if (response.data.finalState.stack.Length == 1) {
                        Result = ParseResult(response.data.finalState.stack[0]);
                    }
                }
                catch (JsonSerializationException e)
                {
                    IsError = true;
                    Error = "Invalid JSON:\n" + www.downloadHandler.text + "\nError: " + e.Message;
                }
                catch (ArgumentException e)
                {
                    IsError = true;
                    Error = "Invalid JSON:\n" + www.downloadHandler.text + "\nError: " + e.Message;
                }
            }
        }
    }

    public class BalanceOfRequest : ProgramRequest<int>
    {
        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override int ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseInt32(elem);
        }

        public IEnumerator BalanceOf(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) });
        }
    }
    public class EmitRequest : ProgramRequest<object>
    {
        public EmitRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Emit(byte[] arg0, int arg1)
        {
            yield return SendRequest("Emit", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) });
        }
    }
    public class TransferRequest : ProgramRequest<object>
    {
        public TransferRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Transfer(byte[] arg0, int arg1)
        {
            yield return SendRequest("Transfer", new string[] { ExploadTypeConverters.PrintBytes(arg0), ExploadTypeConverters.PrintInt32(arg1) });
        }
    }
}