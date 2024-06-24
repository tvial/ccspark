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

package io.github.tvial.ccspark.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.Timer;

import org.apache.spark.TaskFailedReason;
import org.apache.spark.api.plugin.ExecutorPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tvial.ccspark.monitoring.CPUInfo;
import io.github.tvial.ccspark.monitoring.UsageMetricsSink;
import io.github.tvial.ccspark.monitoring.UsageMonitor;
import io.github.tvial.ccspark.sampling.ProcFileSystemSampleProvider;
import io.github.tvial.ccspark.sampling.SampleProvider;


public class CCSparkExecutorPlugin implements ExecutorPlugin {
    final Logger logger = LoggerFactory.getLogger(CCSparkExecutorPlugin.class);

    final static String CPU_TDP_KEY = "cpu.tdp";

    // Same as CodeCarbon (external/hardware.py)
    final static double DEFAULT_CPU_TDP = 85.0;

    private UsageMonitor monitor;
    private SampleProvider sampleProvider;
    private UsageMetricsSink sink;
    private Timer timer;

    double getCPUTDP(Map<String, String> extraConf) {
        if (extraConf.containsKey(CPU_TDP_KEY)) {
            double tdp = Double.valueOf(extraConf.get(CPU_TDP_KEY));
            logger.info(String.format("Using configured TDP of %f", tdp));
            return tdp;
        }

        Optional<Double> tdp = new CPUInfo().getTDP();

        if (!tdp.isPresent()) {
            logger.warn(String.format("Could not determine TDP from CPU database. Applying default TDP of %f", DEFAULT_CPU_TDP));
            return DEFAULT_CPU_TDP;
        }
        else {
            logger.info(String.format("Using discovered TDP of %f", tdp.get()));
            return tdp.get();
        }
    }

    // Interface implementation

    @Override
    public void init(PluginContext context, Map<String, String> extraConf) {
        sampleProvider = new ProcFileSystemSampleProvider();
        sink = new SparkDriverUsageMetricsSink(context);
        monitor = new UsageMonitor(sampleProvider, sink, getCPUTDP(extraConf));
        monitor.initialize();

        timer = new Timer("Usage monitor", true);
        logger.info("Starting timer");
        timer.schedule(monitor, 0L, 1000L);
    }

    @Override
    public void onTaskStart() {
    }

    @Override
    public void onTaskSucceeded() {
    }

    @Override
    public void onTaskFailed(TaskFailedReason failureReason) {
    }

    @Override
    public void shutdown() {
        logger.info("Cancelling timer");
        timer.cancel();

        // One last run
        monitor.run();
    }
}
