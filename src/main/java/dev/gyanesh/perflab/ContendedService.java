package dev.gyanesh.perflab;

import org.springframework.stereotype.Service;

@Service
public class ContendedService {
    private final Object monitor = new Object();

    public void doWork(long holdMs) throws InterruptedException {
        synchronized (monitor) {
            Thread.sleep(holdMs);
        }
    }
}
