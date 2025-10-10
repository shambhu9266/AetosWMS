package com.example.backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PerformanceMonitoringService {

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer responseTimer;

    @Autowired
    public PerformanceMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounter = Counter.builder("api.requests.total")
                .description("Total number of API requests")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("api.errors.total")
                .description("Total number of API errors")
                .register(meterRegistry);
        this.responseTimer = Timer.builder("api.response.time")
                .description("API response time")
                .register(meterRegistry);
    }

    public void incrementRequestCount() {
        requestCounter.increment();
    }

    public void incrementErrorCount() {
        errorCounter.increment();
    }

    public void recordResponseTime(long duration, TimeUnit unit) {
        responseTimer.record(duration, unit);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTimer(Timer.Sample sample) {
        sample.stop(responseTimer);
    }
}
