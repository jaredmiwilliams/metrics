package com.yammer.metrics.spring;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:metered-class.xml")
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class MeteredClassTest {

	@Autowired
	MeteredClass meteredClass;

	@Autowired
	MetricsRegistry metricsRegistry;

	@SuppressWarnings(value = "unchecked")
	private Gauge<Object> fieldGauge() {
		return (Gauge<Object>) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "gaugedField"));
	}

	@SuppressWarnings(value = "unchecked")
	private Gauge<Object> methodGauge() {
		return (Gauge<Object>) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "gaugedMethod"));
	}

	@SuppressWarnings(value = "unchecked")
	private Timer methodTimer() {
		return (Timer) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "timedMethod"));
	}

	@SuppressWarnings(value = "unchecked")
	private Meter methodMeter() {
		return (Meter) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "meteredMethod"));
	}

	@SuppressWarnings(value = "unchecked")
	private Meter methodExceptionMeter() {
		return (Meter) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "exceptionMeteredMethodExceptions"));
	}

	@SuppressWarnings(value = "unchecked")
	private Timer triplyMeteredMethodTimer() {
		return (Timer) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "triplyMeteredMethod-timed"));
	}

	@SuppressWarnings(value = "unchecked")
	private Meter triplyMeteredMethodMeter() {
		return (Meter) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "triplyMeteredMethod-metered"));
	}

	@SuppressWarnings(value = "unchecked")
	private Meter triplyMeteredMethodExceptionMeter() {
		return (Meter) metricsRegistry.allMetrics().get(new MetricName(MeteredClass.class, "triplyMeteredMethod-exceptionMetered"));
	}

	@Test
	public void gauges() {
		assertEquals(999, fieldGauge().value());
		assertEquals(999, methodGauge().value());

		meteredClass.setGaugedField(1000);

		assertEquals(1000, fieldGauge().value());
		assertEquals(1000, methodGauge().value());
	}

	@Test
	public void timedMethod() throws Throwable {
		meteredClass.timedMethod(false);
		assertEquals(1, methodTimer().count());

		// count increments even when the method throws an exception
		try {
			meteredClass.timedMethod(true);
			fail();
		} catch (Throwable e) {
			assertTrue(e instanceof BogusException);
		}
		assertEquals(2, methodTimer().count());
	}

	@Test
	public void meteredMethod() throws Throwable {
		meteredClass.meteredMethod();
		assertEquals(1, methodMeter().count());
	}

	@Test
	public void exceptionMeteredMethod() throws Throwable {
		// throws the right exception
		try {
			meteredClass.exceptionMeteredMethod(BogusException.class);
			fail();
		} catch (Throwable t) {
			assertTrue(t instanceof BogusException);
		}
		assertEquals(1, methodExceptionMeter().count());
	}

	@Test
	public void triplyMeteredMethod() throws Throwable {
		// doesn't throw an exception
		meteredClass.triplyMeteredMethod(false);
		assertEquals(1, triplyMeteredMethodMeter().count());
		assertEquals(1, triplyMeteredMethodTimer().count());

		// throws an exception
		try {
			meteredClass.triplyMeteredMethod(true);
			fail();
		} catch (Throwable t) {
			assertTrue(t instanceof BogusException);
		}
		assertEquals(2, triplyMeteredMethodMeter().count());
		assertEquals(2, triplyMeteredMethodTimer().count());
		assertEquals(1, triplyMeteredMethodExceptionMeter().count());
	}
}
