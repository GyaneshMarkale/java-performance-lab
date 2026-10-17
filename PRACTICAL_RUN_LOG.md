# Practical Run Log — Java Performance Lab

This file records the commands and results actually observed while completing the lab on Windows PowerShell. Re-run the commands on your machine before using the numbers as a benchmark; latency and heap figures vary by JVM, machine, and workload.

## Environment and build

- OS/shell: Windows PowerShell
- Project path used: `C:\Users\Admin\OneDrive\Documents\java-performance-lab\java-performance-lab`
- Maven: installed and `mvn clean test` completed successfully (the detailed Maven test summary was not captured in the chat).
- Python: Python 3.12.10 installed with WinGet. The `python` and `py` command aliases did not work in the current shell, so the full executable path was used:

```powershell
& "$env:LocalAppData\Programs\Python\Python312\python.exe" --version
```

If your Python installation is in a different directory, locate it with:

```powershell
Get-ChildItem "$env:LocalAppData\Programs\Python" -Filter python.exe -Recurse -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName
```

## Load test — observed output

Command:

```powershell
& "$env:LocalAppData\Programs\Python\Python312\python.exe" tools/load_test.py --url http://localhost:8080/api/leak --requests 500 --concurrency 10
```

Observed output:

```text
URL: http://localhost:8080/api/leak
Requests: 500; concurrency: 10
Successes: 500; failures: 0
Elapsed: 0.37s; throughput: 1366.47 req/s
Latency ms: p50=4.5, p95=15.3, p99=31.4, max=81.7
```

**Interpretation:** all 500 requests succeeded in this run. This was a run against `/api/leak`, not a clean baseline against `/api/normal`; treat the figures as an observed sample, not a stable benchmark or proof of a leak.

For a normal endpoint baseline, run:

```powershell
& "$env:LocalAppData\Programs\Python\Python312\python.exe" tools/load_test.py --url http://localhost:8080/api/normal --requests 300 --concurrency 10
```

## JVM process and heap snapshot — observed output

Command:

```powershell
jcmd -l
```

Relevant process line observed:

```text
14132 dev.gyanesh.perflab.PerformanceLabApplication
```

Other Java processes were also present. The PID can change after every restart; always re-run `jcmd -l` and use the PID for `dev.gyanesh.perflab.PerformanceLabApplication`.

Command:

```powershell
jcmd 14132 GC.heap_info
```

Observed output:

```text
14132:
 garbage-first heap   total 200704K, used 89779K [0x0000000704000000, 0x0000000800000000)
  region size 2048K, 10 young (20480K), 1 survivors (2048K)
 Metaspace       used 32906K, committed 33920K, reserved 1114112K
  class space    used 4633K, committed 5056K, reserved 1048576K
```

Approximate interpretation:

- JVM heap used: 89,779 KiB (about 87.7 MiB)
- JVM heap committed: 200,704 KiB (about 196 MiB)
- Metaspace used: 32,906 KiB (about 32.1 MiB)
- Collector reported: G1

Heap `used` is not equivalent to leaked memory; it can include objects that are eligible for collection. Confirm retention using a histogram and heap-dump analysis.

## Heap dump — observed result

Commands used:

```powershell
New-Item -ItemType Directory -Force C:\temp | Out-Null
jcmd 14132 GC.heap_dump C:\temp\performance-lab.hprof
Get-Item C:\temp\performance-lab.hprof
```

Observed file:

```text
Directory: C:\temp
Name: performance-lab.hprof
Length: 95120927 bytes
LastWriteTime: 17-10-2026 17:24
```

The file was created successfully (about 90.7 MiB). A heap dump can pause the JVM and may contain sensitive in-memory data; keep this lab local. The dump itself is intentionally not committed to GitHub.

## Remaining investigation checklist

- [ ] Capture `Invoke-RestMethod http://localhost:8080/api/status | ConvertTo-Json -Depth 5` and add the actual output below.
- [ ] Capture `jcmd <PID> GC.class_histogram` and inspect `byte[]` (`[B`) and `ConcurrentLinkedQueue` entries.
- [ ] Open `C:\temp\performance-lab.hprof` in Eclipse Memory Analyzer (MAT).
- [ ] Inspect Leak Suspects, Dominator Tree, and Path to GC Roots; verify whether the static leak queue retains byte arrays.
- [ ] Run allocation-pressure experiment and compare GC logs.
- [ ] Run contention experiment and capture thread dumps while requests are in flight.
- [ ] Record a JFR profile and inspect allocation, GC, method profiling, and monitor contention in Java Mission Control.
- [ ] Repeat the same workloads after remediation and compare p50/p95/p99, throughput, heap, and GC behavior.

### Commands for the next session

Replace `<PID>` with the current application PID from `jcmd -l`.

```powershell
Invoke-RestMethod "http://localhost:8080/api/status" | ConvertTo-Json -Depth 5
jcmd <PID> GC.class_histogram
jcmd <PID> Thread.print > C:\temp\threads.txt
jcmd <PID> JFR.start name=PerfLab settings=profile duration=90s filename=C:\temp\perflab.jfr
```

To run the other controlled experiments:

```powershell
& "$env:LocalAppData\Programs\Python\Python312\python.exe" tools/load_test.py --url "http://localhost:8080/api/allocation?mb=32" --requests 60 --concurrency 4
& "$env:LocalAppData\Programs\Python\Python312\python.exe" tools/load_test.py --url "http://localhost:8080/api/contention?holdMs=150" --requests 80 --concurrency 10
```

## What has and has not been verified

Verified from the work session: Maven build/tests command completed successfully; the Python load test completed with 500 successes and 0 failures; `jcmd -l` identified the application; a heap snapshot was captured; and the `.hprof` file exists at the path above.

Not yet verified in the work session: the contents of the `/api/status` response, histogram evidence, MAT's retained-object paths, allocation experiment, contention thread states, or JFR analysis. Do not mark those steps complete until you collect and record their actual evidence.
