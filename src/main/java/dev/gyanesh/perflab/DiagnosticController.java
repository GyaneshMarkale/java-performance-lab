package dev.gyanesh.perflab;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class DiagnosticController {
    private static final int RETAINED_BYTES_PER_REQUEST = 128 * 1024;
    private static final ConcurrentLinkedQueue<byte[]> LEAKING_CACHE =
            new ConcurrentLinkedQueue<>();
    private static final AtomicLong LEAK_REQUESTS = new AtomicLong();

    private final ContendedService contendedService;

    public DiagnosticController(ContendedService contendedService) {
        this.contendedService = contendedService;
    }

    @GetMapping("/normal")
    public Map<String, Object> normal() {
        return Map.of("status", "ok", "message", "Normal endpoint");
    }

    /** Intentional defect: retains 128 KiB per call. Local, bounded tests only. */
    @GetMapping("/leak")
    public Map<String, Object> leak() {
        LEAKING_CACHE.add(new byte[RETAINED_BYTES_PER_REQUEST]);
        long requests = LEAK_REQUESTS.incrementAndGet();
        return Map.of(
                "warning", "Intentional memory-retention demo; local lab only",
                "leakRequests", requests,
                "retainedPayloadKiB", requests * (RETAINED_BYTES_PER_REQUEST / 1024L)
        );
    }

    /** Temporary allocation pressure; request value is capped to 1–64 MiB. */
    @GetMapping("/allocation")
    public Map<String, Object> allocation(@RequestParam(defaultValue = "32") int mb) {
        int boundedMb = Math.max(1, Math.min(mb, 64));
        long checksum = 0;
        for (int i = 0; i < boundedMb; i++) {
            byte[] temporary = new byte[1024 * 1024];
            temporary[0] = (byte) i;
            checksum += temporary[0];
        }
        return Map.of(
                "status", "completed",
                "temporaryMiBAllocatedApprox", boundedMb,
                "checksum", checksum
        );
    }

    /** Intentional synchronized bottleneck to demonstrate monitor contention. */
    @GetMapping("/contention")
    public Map<String, Object> contention(
            @RequestParam(defaultValue = "150") long holdMs) throws InterruptedException {
        long boundedHoldMs = Math.max(0, Math.min(holdMs, 1000));
        contendedService.doWork(boundedHoldMs);
        return Map.of("status", "completed", "monitorHoldMs", boundedHoldMs);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "warning", "Local diagnostic lab; do not expose publicly",
                "leakRequests", LEAK_REQUESTS.get(),
                "leakPayloadKiBApprox",
                LEAK_REQUESTS.get() * (RETAINED_BYTES_PER_REQUEST / 1024L)
        );
    }
}
