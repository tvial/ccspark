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

package io.github.tvial.ccspark.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.tvial.ccspark.model.CPUScheduling;
import io.github.tvial.ccspark.model.ProcessScheduling;


class ModelTests {
    @Test
    void cpuSchedulingTotal() {
        CPUScheduling sched = new CPUScheduling(12345L, 67890L, 44444L);

        assertEquals(124679, sched.total());
    }

    @Test
    void cpuSchedulingDifference() {
        CPUScheduling before = new CPUScheduling(12345L, 67890L, 44444L);
        CPUScheduling after = new CPUScheduling(22345L, 57890L, 34444L);
        
        CPUScheduling expected = new CPUScheduling(10000L, 4294957295L, 4294957295L);
        CPUScheduling actual = after.minus(before);

        assertEquals(expected, actual);
    }

    @Test
    void processSchedulingTotal() {
        ProcessScheduling sched = new ProcessScheduling(12345L, 67890L, 44444L, 77777L);

        assertEquals(202456, sched.total());
    }

    @Test
    void processSchedulingDifference() {
        ProcessScheduling before = new ProcessScheduling(12345L, 67890L, 44444L, 77777L);
        ProcessScheduling after = new ProcessScheduling(22345L, 57890L, 34444L, 87777L);
        
        ProcessScheduling expected = new ProcessScheduling(10000L, 4294957295L, 2147473647L, 10000L);
        ProcessScheduling actual = after.minus(before);

        assertEquals(expected, actual);
    }
}