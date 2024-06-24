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

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.SparkContext;
import org.apache.spark.api.plugin.DriverPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.util.DoubleAccumulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.DefaultSettableGauge;

import io.github.tvial.ccspark.model.ExecutorUsageMetrics;
import scala.Tuple2;


public class CCSparkDriverPlugin implements DriverPlugin {
    final Logger logger = LoggerFactory.getLogger(CCSparkDriverPlugin.class);

    final static String CCSPARK_CONF_PREFIX = "spark.ccspark.";
    final static String METRIC_PREFIX = "energy.total_Wh";

    private DoubleAccumulator totalEnergy;
    private DefaultSettableGauge<Double> totalEnergyGauge;

    // Interface implementation

    @Override
    public Map<String, String> init(SparkContext sc, PluginContext pluginContext) {
        totalEnergy = sc.doubleAccumulator("totalEnergy");
        totalEnergyGauge = new DefaultSettableGauge<Double>(0.0);

        Map<String, String> extraConf = new HashMap<String, String>();
        for (Tuple2<String, String> confItem : sc.getConf().getAllWithPrefix(CCSPARK_CONF_PREFIX)) {
            extraConf.put(confItem._1(), confItem._2());
        }

        logger.info(String.format("Found %d configuration parameters", extraConf.size()));
        for (Map.Entry<String, String> entry : extraConf.entrySet()) {
            logger.info(String.format("- %s = %s", entry.getKey(), entry.getValue()));
        }

        return extraConf;
    }
    
    @Override
    public Object receive(Object message) {
        if (message instanceof ExecutorUsageMetrics) {
            ExecutorUsageMetrics usageMetrics = (ExecutorUsageMetrics)message;

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Received estimate %s", usageMetrics));
            }

            totalEnergy.add(usageMetrics.energy_Wh);
            totalEnergyGauge.setValue(totalEnergy.value());
            logger.info(String.format("Total energy: %f Wh", totalEnergy.value()));
        }
        else {
            logger.warn(String.format("Unexpected message: %s", message));
        }

        return null;
    }

    @Override
    public void registerMetrics(String appId, PluginContext pluginContext) {
        pluginContext.metricRegistry().register(METRIC_PREFIX, totalEnergyGauge);
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down plugin");
    }
}
