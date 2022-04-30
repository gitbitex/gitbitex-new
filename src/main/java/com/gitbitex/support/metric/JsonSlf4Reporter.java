package com.gitbitex.support.metric;

import com.alibaba.fastjson.JSON;
import com.codahale.metrics.*;
import org.slf4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class JsonSlf4Reporter extends ScheduledReporter {
    private final Logger logger;

    public JsonSlf4Reporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit,
                            TimeUnit durationUnit, Logger logger) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.logger = logger;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            logMeter(entry.getKey(), entry.getValue());
        }
    }

    private void logMeter(String name, Meter meter) {
        Map<String, Object> params = new HashMap<>();
        params.put("time", new Date().toInstant().toString());
        params.put("type", "METER");
        params.put("name", name);
        params.put("count", meter.getCount());
        params.put("mean_rate", meter.getMeanRate());
        params.put("m1_rate", meter.getOneMinuteRate());
        params.put("m5_rate", meter.getFiveMinuteRate());
        params.put("m15_rate", meter.getFifteenMinuteRate());
        logger.info("{}", JSON.toJSONString(params));
    }
}
