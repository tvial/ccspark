/*
    Copyright 2024, Thomas VIAL

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package io.github.tvial.ccspark.monitoring;

import java.io.IOException;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tvial.ccspark.model.ExecutorUsageMetrics;
import io.github.tvial.ccspark.model.Sample;
import io.github.tvial.ccspark.sampling.SampleProvider;


public class UsageMonitor extends TimerTask {
    final Logger logger = LoggerFactory.getLogger(UsageMonitor.class);

    private double tdp;
    private SampleProvider provider;
    private UsageMetricsSink sink;
    private Sample lastSample;

    public UsageMonitor(SampleProvider provider, UsageMetricsSink sink, double tdp) {
        this.provider = provider;
        this.sink = sink;
        this.tdp = tdp;

        this.lastSample = null;
    }

    public void initialize() {
        try {
            lastSample = provider.sample();
        }
        catch (IOException e) {
            logger.warn(String.format("Cannot get first sample, reason: %s", e.getMessage()));
        }
    }

    protected void sampleAndSendMetrics() {
        if (lastSample == null) {
            logger.warn("First sample is null, ignoring");
            return;
        }

        try {
            ExecutorUsageMetrics usage = sampleUsage();
            sink.send(usage);
        }
        catch (IOException e) {
            logger.warn(String.format("Could not get or send metrics, reason: %s", e.getMessage()));
        }
    }

    public ExecutorUsageMetrics sampleUsage() throws IOException {
        Sample newSample = provider.sample();
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Received sample: %s", newSample));
        }

        ExecutorUsageMetrics metrics = computeUsageMetrics(lastSample, newSample);
        lastSample = newSample;
        return metrics;
    }

    protected ExecutorUsageMetrics computeUsageMetrics(Sample sampleBefore, Sample sampleAfter) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Computing load for %s -> %s", sampleBefore, sampleAfter));
        }

        long cpuDelta = sampleAfter.cpu.minus(sampleBefore.cpu).total();

        if (cpuDelta == 0) {
            return new ExecutorUsageMetrics(0.0, 0.0);
        }

        long processDelta = sampleAfter.process.minus(sampleBefore.process).total();
        long timeDelta = sampleAfter.timestampMillis - sampleBefore.timestampMillis;
        double load = (double)processDelta / (double)cpuDelta;

        double energy = load * tdp * timeDelta / (3600. * 1000.);

        return new ExecutorUsageMetrics(load, energy);
    }

    // TimerTask implementation

    @Override
    public boolean cancel() {
        logger.info("Cancelling usage monitoring task");
        sampleAndSendMetrics();
        return super.cancel();
    }

    @Override
    public void run() {
        sampleAndSendMetrics();
    }
}
