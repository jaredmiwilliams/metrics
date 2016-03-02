package com.yammer.metrics.util;

import com.yammer.metrics.core.Metric;

public interface MetricTracker {
  void track(Metric metric);
}
