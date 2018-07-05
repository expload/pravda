using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;
using Keiwando.BigInteger;

namespace Io.Mytc.{{programName}} {
    {{#parseClasses}}
    [System.Serializable]
    class {{resultTpeClass}}Result {
       public {{resultTpe}} value;
       public string tpe;

       public static {{resultTpeClass}}Result FromJson(string json) {
           return JsonUtility.FromJson<{{resultTpeClass}}Result>(json);
       }
    }
    {{/parseClasses}}

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
            UnityWebRequest www = UnityWebRequest.Put("{{client}}", json);
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

    {{#methods}}
    class {{methodName}}Request: ProgramRequest<{{methodTpe}}> {

        public {{methodName}}Request(BigInteger programAddress) : base(programAddress) { }

        protected override {{methodTpe}} ParseResult(string json)
        {
            return {{methodParseResult}};
        }

        public IEnumerator {{methodName}}({{methodArgsDef}})
        {
            String json = String.Format("{{{jsonFormat}}}", {{{argsFormat}}});
            yield return SendJson(json);
        }
    }
    {{/methods}}
}