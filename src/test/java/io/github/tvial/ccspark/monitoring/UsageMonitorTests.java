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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.tvial.ccspark.model.CPUScheduling;
import io.github.tvial.ccspark.model.ExecutorUsageMetrics;
import io.github.tvial.ccspark.model.ProcessScheduling;
import io.github.tvial.ccspark.model.Sample;
import io.github.tvial.ccspark.monitoring.UsageMetricsSink;
import io.github.tvial.ccspark.monitoring.UsageMonitor;
import io.github.tvial.ccspark.sampling.InMemorySampleProvider;


public class UsageMonitorTests {
    Sample[] samples = {
        new Sample(
            10000L,
            new CPUScheduling(50, 60, 70),
            new ProcessScheduling(20, 30, 10, 20)
        ),
        new Sample(
            30000L,
            new CPUScheduling(80, 75, 100),
            new ProcessScheduling(50, 40, 20, 35)
        ),
        new Sample(
            40000L,
            new CPUScheduling(85, 175, 120),
            new ProcessScheduling(80, 45, 40, 80)
        ),
        new Sample(
            50000L,
            new CPUScheduling(185, 275, 220),
            new ProcessScheduling(180, 145, 140, 180)
        )
    };

    public List<ExecutorUsageMetrics> collectedMetrics = new LinkedList<ExecutorUsageMetrics>();
    
    UsageMetricsSink sink = new UsageMetricsSink() {
        public void send(ExecutorUsageMetrics usage) {
            collectedMetrics.add(usage);
        }
    };

    UsageMonitor monitor = new UsageMonitor(new InMemorySampleProvider(samples), sink, 150.0);

    private static void assertUsageMetricsEqual(ExecutorUsageMetrics expected, ExecutorUsageMetrics actual) {
        assertEquals(expected.load, actual.load, 1e-5);
        assertEquals(expected.energy_Wh, actual.energy_Wh, 1e-5);
    }

    @Test
    public void computeUsageMetrics() {
        ExecutorUsageMetrics expected, usage;

        expected = new ExecutorUsageMetrics(0.86666667, 0.72222222);
        usage = monitor.computeUsageMetrics(samples[0], samples[1]);
        assertUsageMetricsEqual(expected, usage);
    }

    @Test
    public void computeUsageMetricsWhenNoChange() {
        ExecutorUsageMetrics expected, usage;

        expected = new ExecutorUsageMetrics(0.0, 0.0);
        usage = monitor.computeUsageMetrics(samples[0], samples[0]);
        assertUsageMetricsEqual(expected, usage);
    }


    @Test
    public void sampleMetricsTwice() throws IOException {
        ExecutorUsageMetrics expected, usage;

        monitor.initialize();

        expected = new ExecutorUsageMetrics(0.86666667, 0.72222222);
        usage = monitor.sampleUsage();
        assertUsageMetricsEqual(expected, usage);

        expected = new ExecutorUsageMetrics(0.8, 0.3333333);
        usage = monitor.sampleUsage();
        assertUsageMetricsEqual(expected, usage);
    }

    @Test
    public void sampleAndSendMetricsTwice() throws IOException {
        ExecutorUsageMetrics expected, usage;

        collectedMetrics.clear();
        
        monitor.initialize();
        monitor.sampleAndSendMetrics();
        monitor.sampleAndSendMetrics();

        assertEquals(2, collectedMetrics.size());

        expected = new ExecutorUsageMetrics(0.86666667, 0.72222222);
        usage = collectedMetrics.get(0);
        assertUsageMetricsEqual(expected, usage);
        
        expected = new ExecutorUsageMetrics(0.8, 0.3333333);
        usage = collectedMetrics.get(1);
        assertUsageMetricsEqual(expected, usage);
    }
}
