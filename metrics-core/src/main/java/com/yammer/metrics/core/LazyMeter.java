package com.yammer.metrics.core;

import com.yammer.metrics.util.MetricTracker;
import com.yammer.metrics.util.RunOnceMetricTracker;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LazyMeter extends Meter {
  private final MetricTracker tracker;

  LazyMeter(MetricTracker tracker, ScheduledExecutorService tickThread, String eventType, TimeUnit rateUnit, Clock clock) {
    super(tickThread, eventType, rateUnit, clock);
    this.tracker = new RunOnceMetricTracker(tracker);
  }

  @Override
  public void mark() {
    try {
      super.mark();
    } finally {
      tracker.track(this);
    }
  }

  @Override
  public void mark(long n) {
    try {
      super.mark(n);
    } finally {
      tracker.track(this);
    }
  }
}
