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


public class CPUScheduling {
    public long userTime;
    public long nicedTime;
    public long systemTime;

    public CPUScheduling(long userTime, long nicedTime, long systemTime) {
        this.userTime = userTime;
        this.nicedTime = nicedTime;
        this.systemTime = systemTime;
    }

    public boolean equals(Object other) {
        CPUScheduling otherScheduling = (CPUScheduling)other;
        return
            (userTime == otherScheduling.userTime) &&
            (nicedTime == otherScheduling.nicedTime) &&
            (systemTime == otherScheduling.systemTime);
    }

    public long total() {
        return userTime + nicedTime + systemTime;
    }

    public CPUScheduling minus(CPUScheduling other) {
        return new CPUScheduling(
            wrapUnsigned(userTime - other.userTime),
            wrapUnsigned(nicedTime - other.nicedTime),
            wrapUnsigned(systemTime - other.systemTime)
        );
    }

    private long wrapUnsigned(long value) {
        return value < 0 ? value + 4294967295L : value;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("<user: ").append(userTime)
            .append(", niced: ").append(nicedTime)
            .append(", system: ").append(systemTime)
            .append(">")
            .toString();
    }
}