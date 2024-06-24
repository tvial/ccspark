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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.github.tvial.ccspark.plugin.CCSparkExecutorPlugin;

public class CCSparkExecutorPluginTests {
    @Test
    public void overrideTDP() {
        HashMap<String, String> conf = new HashMap<String, String>();
        conf.put("cpu.tdp", "-100");
        double tdp = new CCSparkExecutorPlugin().getCPUTDP(conf);
        assertEquals(-100.0, tdp);
    }

    @Test
    public void dontOverrideTDP() {
        HashMap<String, String> conf = new HashMap<String, String>();
        double tdp = new CCSparkExecutorPlugin().getCPUTDP(conf);
        assertTrue(tdp > 0.0);
    }
}
