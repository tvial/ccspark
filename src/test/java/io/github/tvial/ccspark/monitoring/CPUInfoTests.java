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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.tvial.ccspark.monitoring.CPUInfo;


public class CPUInfoTests {
    @Test
        public void getCPUModel() {
            CPUInfo info = new CPUInfo("src/test/resources/proc_cpuinfo.txt");
            Optional<String> model = info.getCPUModel();

            assertTrue(model.isPresent());
            assertEquals("Intel(R) Core(TM) i7-8550U CPU @ 1.80GHz", model.get());
        }

    @Test
    public void getCPUModelFromInvalidPath() {
        CPUInfo info = new CPUInfo("hope/this/is/invalid!");
        Optional<String> model = info.getCPUModel();

        assertFalse(model.isPresent());
    }

    @Test
    public void getTDPFromExactCPUModel() {
        CPUInfo info = new CPUInfo();

        Optional<Double> tdp = info.getTDP("Intel Core i7-8550U");
        assertEquals(Optional.of(15.0), tdp);
    }

    @Test
    public void getTDPFromApproximateCPUModel() {
        CPUInfo info = new CPUInfo();

        Optional<Double> tdp = info.getTDP("Intel(R) Core(TM) i7-8550U CPU @ 1.80GHz");
        assertEquals(Optional.of(15.0), tdp);
    }

    @Test
    public void getDefaultTDPCPUModelWhenMissing() {
        CPUInfo info = new CPUInfo();

        Optional<Double> tdp = info.getTDP("Zebulon 3000");
        assertFalse(tdp.isPresent());
    }
}
