## How to publish to nuget.org

Note: This doc intended for Expload team members.

* Ensure you have been invided to nuget.org Expload orgnization.
* Create a new API key and set it using `nuget setapikey <key>`
* Run `dotnet pack`
* Run `nuget push bin/Debug/Pravda.<version>.nupkg -Source https://api.nuget.org/v3/index.json` 
