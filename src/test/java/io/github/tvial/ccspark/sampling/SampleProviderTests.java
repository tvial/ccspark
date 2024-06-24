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

package io.github.tvial.ccspark.sampling;

import static java.nio.file.Files.newBufferedReader;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.github.tvial.ccspark.model.CPUScheduling;
import io.github.tvial.ccspark.model.ProcessScheduling;
import io.github.tvial.ccspark.sampling.ProcFileSystemSampleProvider;


class SampleProviderTests {
    private final ProcFileSystemSampleProvider inspector = new ProcFileSystemSampleProvider(
        "src/test/resources/proc_stat.txt",
        "src/test/resources/proc_pid_stat.txt"
    );

    @Test
    void getPIDOfCurrentProcess() {
        assertInstanceOf(Long.class, inspector.getPID());
    }

    @Test
    void getCPUScheduling() throws IOException {
        BufferedReader reader = newBufferedReader(Paths.get("src/test/resources/proc_stat.txt"));

        try {
            CPUScheduling[] expectedScheduling = {
                new CPUScheduling(15734714L, 1244708L, 5143147L),
                new CPUScheduling(1987937L, 146781L, 643300L),
                new CPUScheduling(1986254L, 157558L, 635453L),
                new CPUScheduling(2090208L, 183868L, 635510L),
                new CPUScheduling(1999813L, 170203L, 636611L),
                new CPUScheduling(1983114L, 174199L, 659014L),
                new CPUScheduling(1984878L, 155284L, 656067L),
                new CPUScheduling(1788224L, 110937L, 652197L),
                new CPUScheduling(1914284L, 145876L, 624992L),
            };
            CPUScheduling[] actualScheduling = inspector.getCPUSchedulings(reader);
            assertArrayEquals(expectedScheduling, actualScheduling);
        }
        finally {
            reader.close();
        }
    }

    @Test
    void getProcessScheduling() throws IOException {
        BufferedReader reader = newBufferedReader(Paths.get("src/test/resources/proc_pid_stat.txt"));

        try {
            ProcessScheduling expectedScheduling = new ProcessScheduling(175L, 107L, 59079L, 4776L);
            ProcessScheduling actualScheduling = inspector.getProcessScheduling(reader);
            assertEquals(expectedScheduling, actualScheduling);
        }
        finally {
            reader.close();
        }
    }
}