package com.yammer.metrics.util;

import com.yammer.metrics.core.Metric;

import java.util.concurrent.atomic.AtomicBoolean;

public class RunOnceMetricTracker implements MetricTracker {
  private final MetricTracker delegate;
  private final AtomicBoolean hasRun;

  public RunOnceMetricTracker(MetricTracker delegate) {
    this.delegate = delegate;
    this.hasRun = new AtomicBoolean(false);
  }

  @Override
  public void track(Metric metric) {
    if (!hasRun.get()) {
      synchronized (this) {
        if (!hasRun.get()) {
          try {
            delegate.track(metric);
          } finally {
            hasRun.set(true);
          }
        }
      }
    }
  }
}
