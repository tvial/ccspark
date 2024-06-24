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

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;

import io.github.tvial.ccspark.model.CPUScheduling;
import io.github.tvial.ccspark.model.ProcessScheduling;
import io.github.tvial.ccspark.model.Sample;


public class ProcFileSystemSampleProvider implements SampleProvider {
    String cpuPath;
    String processPath;

    public ProcFileSystemSampleProvider(String cpuPath, String processPath) {
        this.cpuPath = cpuPath;
        this.processPath = processPath;
    }

    public ProcFileSystemSampleProvider() {
        this.cpuPath = "/proc/stat";
        this.processPath = String.format("/proc/%d/stat", getPID());
    }

    @Override
    public Sample sample() throws IOException {
        BufferedReader reader = newBufferedReader(Paths.get(cpuPath));
        CPUScheduling[] cpuSchedulings;

        try {
            cpuSchedulings = getCPUSchedulings(reader);
        }
        finally {
            reader.close();
        }

        reader = newBufferedReader(Paths.get(processPath));
        ProcessScheduling processScheduling;
        try {
            processScheduling = getProcessScheduling(reader);
        }
        finally {
            reader.close();
        }

        return new Sample(System.currentTimeMillis(), cpuSchedulings[0], processScheduling);
    }

    protected CPUScheduling[] getCPUSchedulings(BufferedReader reader) {
        return reader.lines()
            .filter(line -> line.startsWith("cpu"))
            .map(line -> getCPUSchedulingFromLine(line))
            .toArray(CPUScheduling[]::new);
    }

    private CPUScheduling getCPUSchedulingFromLine(String line) {
        String[] tokens = line.split("\\s+");

        return new CPUScheduling(
            Long.parseLong(tokens[1]),
            Long.parseLong(tokens[2]),
            Long.parseLong(tokens[3])
        );
    }

    protected ProcessScheduling getProcessScheduling(BufferedReader reader) throws IOException {
        String[] tokens = reader.readLine().split("\\s+");

        return new ProcessScheduling(
            Long.parseLong(tokens[13]),
            Long.parseLong(tokens[14]),
            Long.parseLong(tokens[15]),
            Long.parseLong(tokens[16])
        );
    }

	protected long getPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Long.valueOf(name.split("@")[0]);
	}
}
