package com.yammer.metrics.core;

import com.yammer.metrics.util.MetricTracker;
import com.yammer.metrics.util.RunOnceMetricTracker;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LazyTimer extends Timer {
  private final MetricTracker tracker;

  LazyTimer(MetricTracker tracker, ScheduledExecutorService tickThread, TimeUnit durationUnit, TimeUnit rateUnit, Clock clock) {
    super(tickThread, durationUnit, rateUnit, clock);
    this.tracker = new RunOnceMetricTracker(tracker);
  }

  @Override
  public void update(long duration, TimeUnit unit) {
    try {
      super.update(duration, unit);
    } finally {
      tracker.track(this);
    }
  }

  @Override
  public <T> T time(Callable<T> event) throws Exception {
    try {
      return super.time(event);
    } finally {
      tracker.track(this);
    }
  }

  @Override
  public TimerContext time() {
    try {
      return super.time();
    } finally {
      tracker.track(this);
    }
  }
}
