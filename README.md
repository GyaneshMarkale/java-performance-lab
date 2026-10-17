# Java Performance Lab

A deliberately imperfect Spring Boot service for learning to diagnose JVM memory, GC, thread, and latency problems. Intended for local development and interview preparation.

## Stack

- Java 21
- Spring Boot 3.5.6
- Maven
- JDK diagnostics: `jcmd`, GC unified logging, Java Flight Recorder (JFR)
- Python 3 standard library for bounded local load generation

## Safety first

**Run this project locally only. Do not deploy or expose it to a public network.** The `/api/leak` endpoint intentionally retains memory. Keep experiments bounded and monitor the process. Heap dumps and recordings can contain sensitive data; do not commit those artifacts or collect them from production without authorization and an incident procedure.

## Endpoints

| Endpoint | Purpose |
|---|---|
| `GET /api/normal` | Baseline response |
| `GET /api/leak` | Intentionally retains 128 KiB per request |
| `GET /api/allocation?mb=32` | Allocates temporary arrays to create allocation pressure (1–64 MiB per call) |
| `GET /api/contention?holdMs=150` | Serializes calls on a monitor to demonstrate contention |
| `GET /api/status` | Counters for the memory-retention demo |
| `GET /actuator/health` | Health endpoint |
| `GET /actuator/metrics` | Actuator metrics index |

## 1. Prerequisites

Install a JDK 21 distribution and Maven 3.9+. Confirm that Java, Maven, and Python are available:

```powershell
java -version
mvn -version
python --version
```

On Windows, if Python is installed but the `python` and `py` aliases are unavailable, locate `python.exe` under `$env:LocalAppData\Programs\Python` and invoke it by its full path.

## 2. Build and test

From the repository root:

```powershell
mvn clean test
mvn package
```

## 3. Start the service with GC logging

PowerShell (one line):

```powershell
java -Xms256m -Xmx512m "-Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags" -jar target/java-performance-lab-1.0.0.jar
```

Then check `http://localhost:8080/api/normal`.

## 4. Bounded load experiments

Run each command from a second terminal in the repository root. The supplied load generator rejects non-localhost URLs.

Baseline:

```powershell
python tools/load_test.py --url http://localhost:8080/api/normal --requests 300 --concurrency 10
```

Temporary allocation pressure:

```powershell
python tools/load_test.py --url "http://localhost:8080/api/allocation?mb=32" --requests 60 --concurrency 4
```

Intentional memory retention (small workload first):

```powershell
python tools/load_test.py --url http://localhost:8080/api/leak --requests 100 --concurrency 5
Invoke-RestMethod "http://localhost:8080/api/status" | ConvertTo-Json -Depth 5
```

Monitor/contention experiment:

```powershell
python tools/load_test.py --url "http://localhost:8080/api/contention?holdMs=150" --requests 80 --concurrency 10
```

The leak endpoint retains approximately 128 KiB per successful request, before collection/object overhead. Increase the workload cautiously and stop if memory rises too quickly.

## 5. Collect JVM evidence

Find the process and confirm the PID before every command; it changes after restarts:

```powershell
jcmd -l
jcmd <PID> VM.version
jcmd <PID> VM.command_line
jcmd <PID> GC.heap_info
jcmd <PID> GC.class_histogram
jcmd <PID> Thread.print > C:\temp\threads.txt
jcmd <PID> JFR.start name=PerfLab settings=profile duration=90s filename=C:\temp\perflab.jfr
```

Heap dump (can pause the JVM; ensure sufficient disk space):

```powershell
New-Item -ItemType Directory -Force C:\temp | Out-Null
jcmd <PID> GC.heap_dump C:\temp\performance-lab.hprof
```

Open the dump in [Eclipse Memory Analyzer (MAT)](https://eclipse.dev/mat/). Inspect the Leak Suspects Report, Dominator Tree, and Path to GC Roots. In Java Mission Control, inspect the JFR allocation, method profiling, monitor contention, thread, and GC events.

GC logging must be enabled at JVM startup. Restart the local service with the logging options above if required.

## 6. Interpret the experiments

- **`/api/normal`:** reference latency/throughput baseline.
- **`/api/leak`:** retained `byte[]` objects should be reachable through the static `LEAKING_CACHE`.
- **`/api/allocation`:** temporary allocation should increase allocation/GC activity, but arrays are not deliberately retained.
- **`/api/contention`:** concurrent calls queue on one synchronized monitor; thread dumps may show threads blocked entering `ContendedService.doWork`.

These are expected qualitative outcomes, not measured conclusions until you collect the matching evidence. Heap-used values alone do not prove a leak.

## 7. Investigation worksheet

For each run, record:

1. Hypothesis.
2. Workload and p50/p95/p99 latency, throughput, and failures.
3. Heap used/committed, process memory, CPU, GC frequency, and pause times.
4. Thread states and executor queue depth where relevant.
5. Artifacts collected (GC log, histogram, thread dump, heap dump, JFR).
6. Evidence supporting or rejecting the hypothesis.
7. Root cause and fix.
8. Same-workload before/after comparison.

## 8. Remediation exercises

- Bound or remove unnecessary retention; consider cache eviction policies.
- Reduce temporary allocations only when profiling shows they matter.
- Narrow synchronized critical sections or redesign shared state when evidence proves contention.
- Repeat the exact workload after each fix.
- Do not treat increasing `-Xmx` as a fix for unbounded retention.

## Practical progress log

See [`PRACTICAL_RUN_LOG.md`](PRACTICAL_RUN_LOG.md) for the Windows PowerShell commands and results actually observed so far. It distinguishes completed steps from investigations still pending.

## References

- [Java 21 troubleshooting](https://docs.oracle.com/en/java/javase/21/troubleshoot/)
- [Java 21 GC tuning](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Java Flight Recorder](https://docs.oracle.com/en/java/javase/21/jfapi/)
- [Eclipse Memory Analyzer](https://eclipse.dev/mat/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/)
