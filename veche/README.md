# MyTimeCoin Blockchin Node

So work in progress. Very still in development. Much bugs.

1. Configure `development.conf`. See `application.conf` for details
2. Run SBT via `TC_CONFIG_FILE=`pwd`/development.conf sbt`
3. Do `reStart`
4. Open `localhost:8080/ui`

## REST API

By default node listen 8080 tcp port.

### Track action

```bash
curl -s -X POST -H "Content-Type: application/json" \
  -d '"SmokeVape"' \
  http://localhost:8080/api/private/accounts/<address>/actions/track
```

### Commit action package

```bash
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"valuableAction": "SayMonadIsAMonoidInTheCategoryOfEndofunctors", "reward": 1.5, "tariff": 1, "fee": 0.1 }' \
  http://localhost:8080/api/private/accounts/<address>/actions/commit
```

### List offers

```
curl -s -X GET \
  http://localhost:8080/api/private/accounts/<address>/offers
```

### Purchase data

```
curl -s -X GET \
  http://localhost:8080/api/private/accounts/<address>/offers/<data-ref>/purchase
```

## Available actions

1. `SmokeVape`
2. `DrinkSmoothie`
3. `DriveGyroscooter`
4. `WorkInCoworking`
5. `LiveInColiving`
6. `SayMonadIsAMonoidInTheCategoryOfEndofunctors`

## Participation

We're glad to see any Pull Requests, especially it they solves issues labeled with `good first issue` or `help wanted`. Also we will accepts PRs which are fixes typos, mistakes, broken links, etc. Regardless of the nature of your PR, we have to ask you to digitally sign the Mytime CLA. Please send us email with GPG signed text of CLA to contributing@mytc.io. If you're want to send PR, make sure that this requirements are satisfied:

1. You already sent GPG-signed Mytime CLA to contributing@mytc.io
2. Commints are signed with same GPG-key as CLA
3. Content of Pull Request satisfy Code Of Conduct
4. Any PR should resolve an issue
5. PR name matches "Close/Fix #issue: Summanry"
6. PR doesn't contain merge commits
7. Committs matches "verb in present simple subject (#issue)"
