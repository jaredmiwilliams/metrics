package com.yammer.metrics.core;

import com.yammer.metrics.util.MetricTracker;
import com.yammer.metrics.util.RunOnceMetricTracker;

public class LazyHistogram extends Histogram {
  private final MetricTracker tracker;

  LazyHistogram(MetricTracker onUse, SampleType type) {
    super(type);
    this.tracker = new RunOnceMetricTracker(onUse);
  }

  @Override
  public void update(int value) {
    try {
      super.update(value);
    } finally {
      tracker.track(this);
    }
  }

  @Override
  public void update(long value) {
    try {
      super.update(value);
    } finally {
      tracker.track(this);
    }
  }
}
