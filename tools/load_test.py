#!/usr/bin/env python3
"""Small standard-library load generator. Use only against your local lab."""
import argparse
import concurrent.futures
import time
import urllib.error
import urllib.request


def request_once(url, timeout):
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            response.read()
            ok = 200 <= response.status < 400
    except (urllib.error.URLError, TimeoutError, OSError):
        ok = False
    return (time.perf_counter() - start) * 1000, ok


def percentile(values, p):
    if not values:
        return float("nan")
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, int(round((p / 100) * (len(ordered) - 1)))))
    return ordered[index]


def main():
    parser = argparse.ArgumentParser(description="Local-only HTTP load test")
    parser.add_argument("--url", default="http://localhost:8080/api/normal")
    parser.add_argument("--requests", type=int, default=100)
    parser.add_argument("--concurrency", type=int, default=5)
    parser.add_argument("--timeout", type=float, default=10)
    args = parser.parse_args()
    if args.requests < 1 or args.concurrency < 1:
        parser.error("--requests and --concurrency must be >= 1")
    if not args.url.startswith(("http://localhost", "http://127.0.0.1", "http://[::1]")):
        parser.error("Safety guard: URL must point to localhost/127.0.0.1/[::1]")
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(lambda _: request_once(args.url, args.timeout), range(args.requests)))
    elapsed = time.perf_counter() - started
    latencies = [latency for latency, _ in results]
    successes = sum(ok for _, ok in results)
    print(f"URL: {args.url}")
    print(f"Requests: {args.requests}; concurrency: {args.concurrency}")
    print(f"Successes: {successes}; failures: {len(results) - successes}")
    print(f"Elapsed: {elapsed:.2f}s; throughput: {len(results) / elapsed:.2f} req/s")
    print(f"Latency ms: p50={percentile(latencies, 50):.1f}, "
          f"p95={percentile(latencies, 95):.1f}, p99={percentile(latencies, 99):.1f}, "
          f"max={max(latencies):.1f}")


if __name__ == "__main__":
    main()
