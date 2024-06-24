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

import static java.nio.file.Files.newBufferedReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CPUInfo {
    final Logger logger = LoggerFactory.getLogger(CPUInfo.class);

    final static Pattern[] CPU_MODEL_SIMPLIFICATIONS = {
        Pattern.compile(" cpu @ [\\d.]+ *ghz"),
        Pattern.compile("\\((r|tm)\\)")
    };

    private String infoPath;

    public CPUInfo(String infoPath) {
        this.infoPath = infoPath;
    }

    public CPUInfo() {
        this("/proc/cpuinfo");
    }

    public Optional<Double> getTDP(String cpuModel) {
        String simplifiedCPUModel = simplify(cpuModel);
        try {
            ClassLoader cl = CPUInfo.class.getClassLoader();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(cl.getResourceAsStream("cpu_power_averages.csv"))
            );

            return reader.lines()
                .map(line -> line.split(","))
                .filter(tokens -> tokens.length > 1)
                .filter(tokens -> simplify(tokens[0]).equals(simplifiedCPUModel))
                .map(tokens -> Double.parseDouble(tokens[1]))
                .findFirst();
        }
        catch (Exception e) {
            logger.warn(String.format("Could not load TDP database, reason: %s", e.getMessage()));
            return Optional.empty();
        }
    }

    public Optional<Double> getTDP() {
        Optional<String> cpuModel = getCPUModel();

        if (!cpuModel.isPresent()) {
            return Optional.empty();
        }
        else {
            return getTDP(cpuModel.get());
        }
    }

    protected Optional<String> getCPUModel() {
        try {
            BufferedReader reader = newBufferedReader(Paths.get(infoPath));

            try {
                Optional<String> cpuModel = reader.lines()
                    .filter(line -> line.startsWith("model name"))
                    .map(line -> getCPUModelFromLine(line))
                    .findFirst();

                logger.info(String.format("Detected CPU model: %s", cpuModel));
                return cpuModel;
            }
            finally {
                reader.close();
            }

        }
        catch (Exception e) {
            logger.warn(String.format("Could not get CPU model, reason: %s", e.getMessage()));
            return Optional.empty();
        }
    }

    private String getCPUModelFromLine(String line) {
        String[] tokens = line.split(":", 2);

        return tokens[1].trim();
    }

    private String simplify(String cpuModel) {
        String simplified = cpuModel.toLowerCase();

        for (Pattern pattern : CPU_MODEL_SIMPLIFICATIONS) {
            Matcher matcher = pattern.matcher(simplified);
            simplified = matcher.replaceAll("");
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Simplify /%s/ -> /%s/", cpuModel, simplified));
        }

        return simplified;
    }
}
