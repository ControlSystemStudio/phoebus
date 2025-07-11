# Java Bluesky Interface (JBI)

Prototype Java client for the [Bluesky QueueServer](https://blueskyproject.io/bluesky-queueserver/) REST API.  
It includes:

* **BlueskyHttpClient** – thread-safe singleton with rate-limiting, automatic retries and JUL logging
* **BlueskyService**   – one blocking call per API route
* **CLI / REPL** – *testing utilities only*
* **JavaFX UI** – the part you actually launch in normal use

---

##   Prerequisites

* Java 17
* Maven
* Running Bluesky HTTP Server at `http://localhost:60610`  
  *single-user API key `a` assumed below*

```bash
# 1) Start RE Manager
start-re-manager --use-ipython-kernel=ON --zmq-publish-console=ON

# 2) Start HTTP Server
QSERVER_HTTP_SERVER_SINGLE_USER_API_KEY=a \
    uvicorn --host localhost --port 60610 bluesky_httpserver.server:app
````

---

##   Configure

```bash
export BLUESKY_API_KEY=a 
```

---

##  Build & run

```bash
mvn clean javafx:run
```

### Verbose logging

```bash
mvn -Djava.util.logging.config.file=src/main/resources/logging.properties javafx:run
```

---

##   CLI / REPL (testing only)

| Tool     | Start (quiet)                                                                         | Start with request tracing (`FINE`)                                                                                                                        |
| -------- | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **CLI**  | `mvn -q -Dexec.mainClass=util.org.phoebus.applications.queueserver.RunEngineCli  -Dexec.args="STATUS" exec:java` | `mvn -Djava.util.logging.config.file=src/main/resources/logging.properties -q -Dexec.mainClass=util.org.phoebus.applications.queueserver.RunEngineCli -Dexec.args="STATUS" exec:java` |
| **REPL** | `mvn -q -Dexec.mainClass=util.org.phoebus.applications.queueserver.RunEngineRepl exec:java`                          | `mvn -Djava.util.logging.config.file=src/main/resources/logging.properties -q -Dexec.mainClass=util.org.phoebus.applications.queueserver.RunEngineRepl -Dexec.args="STATUS" exec:java`                                                                                                                         |

*CLI examples*

```bash
# list endpoints
mvn -q -Dexec.mainClass=com.jbi.util.RunEngineClili -Dexec.args="list" exec:java

# start the queue
mvn -q -Dexec.mainClass=com.jbi.util.RunEngineClili -Dexec.args="QUEUE_START" exec:java
```

`ENDPOINT [body]` accepts a JSON literal, `@file.json`, or `@-` for stdin.

---

##   How logging works

* Logger name: **`com.jbi.bluesky`**
* Levels

  * `INFO` – API errors
  * `WARNING` – transport retries
  * `FINE` – each HTTP call + latency
* Enable by passing JVM flag
  `-Djava.util.logging.config.file=src/main/resources/logging.properties`

---

##   Tuning

```java
// rate limit (req/sec)
BlueskyHttpClient.initialize("http://localhost:60610",
                             System.getenv("BLUESKY_API_KEY"),
                             3.0);

// retry/back-off
// edit src/main/java/com/jbi/util/HttpSupport.java
MAX_RETRIES        = 5;
INITIAL_BACKOFF_MS = 500;
BACKOFF_MULTIPLIER = 1.5;
```
