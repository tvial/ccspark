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

import java.io.IOException;

import org.apache.spark.api.plugin.PluginContext;

import io.github.tvial.ccspark.model.ExecutorUsageMetrics;
import io.github.tvial.ccspark.monitoring.UsageMetricsSink;


public class SparkDriverUsageMetricsSink implements UsageMetricsSink {
    PluginContext context;

    public SparkDriverUsageMetricsSink(PluginContext context) {
        this.context = context;
    }

    @Override
    public void send(ExecutorUsageMetrics metrics) throws IOException {
        context.send(metrics);
    }
}
