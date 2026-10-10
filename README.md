# ContractHarbor

Java 17 + REST Assured API testing reference framework by Manoj Perla.

## Run

Install JDK 17 and Maven 3.9+, then run `mvn --batch-mode verify`.
No hosted API, Docker, account, or secret is needed. Tests start a fresh loopback HTTP
server on a random port for each case and dispose of it afterward.

## What is covered

- Create → read → complete → delete lifecycle, including persisted updates and 404 after deletion.
- JSON Schema validation, status codes, Location header, and body assertions.
- Blank/missing titles, malformed JSON, missing/invalid authentication, unknown IDs, and Unicode.
- Ten test cases, JUnit XML reports, and GitHub Actions on pushes and pull requests.

`TaskApi` holds a reusable request specification per client. `DemoApi` is a small,
in-memory teaching fixture, not a production service or a substitute for testing a real backend.
Tests deliberately use actual HTTP, rather than mocking REST Assured responses.
`schemas/task.json` is the checked response contract. Reports are under
`target/surefire-reports` and uploaded by CI.

## Adapt to your service

Replace the fixture setup with your dedicated test environment URL and obtain a token
from an environment variable. Keep credentials out of source and request logs. Adapt
the client routes and schemas, create unique test data, and clean up only records the
tests own. The supplied suite intentionally cannot target an arbitrary production URL.

## Structure

`src/test/java/com/manojperla/contractharbor` contains the client, demo server and tests;
`src/test/resources/schemas` contains contracts. Dependencies and build plugins are pinned
in `pom.xml`. MIT licensed.

## Windows JDK socket troubleshooting

If Java reports `Unable to establish loopback connection` with a
`UnixDomainSockets.connect` stack trace before any HTTP assertions run, use:

```powershell
mvn verify "-DargLine=-Djdk.net.unixdomain.tmpdir=target/tcp-fallback"
```

Leave that directory absent: the JDK falls back to TCP for its internal selector
pipe when Unix-domain socket binding is unavailable. This is a local workaround
for affected Windows environments; the normal command is used on Linux CI.
