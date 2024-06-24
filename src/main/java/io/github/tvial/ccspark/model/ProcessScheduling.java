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


public class ProcessScheduling {
    public long userTime;
    public long systemTime;
    public long childrenUserTime;
    public long childrenSystemTime;

    public ProcessScheduling(long userTime, long systemTime, long childrenUserTime, long childrenSystemTime) {
        this.userTime = userTime;
        this.systemTime = systemTime;
        this.childrenUserTime = childrenUserTime;
        this.childrenSystemTime = childrenSystemTime;
    }

    public boolean equals(Object other) {
        ProcessScheduling otherScheduling = (ProcessScheduling)other;
        return
            (userTime == otherScheduling.userTime) &&
            (systemTime == otherScheduling.systemTime) &&
            (childrenUserTime == otherScheduling.childrenUserTime) &&
            (childrenSystemTime == otherScheduling.childrenSystemTime);
    }

    public long total() {
        return userTime + systemTime + childrenUserTime + childrenSystemTime;
    }

    public ProcessScheduling minus(ProcessScheduling other) {
        return new ProcessScheduling(
            wrapUnsigned(userTime - other.userTime),
            wrapUnsigned(systemTime - other.systemTime),
            wrapSigned(childrenUserTime - other.childrenUserTime),
            wrapSigned(childrenSystemTime - other.childrenSystemTime)
        );
    }

    private long wrapUnsigned(long value) {
        return value < 0 ? value + 4294967295L : value;
    }

    private long wrapSigned(long value) {
        return value < 0 ? value + 2147483647 : value;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("<user: ").append(userTime)
            .append(", system: ").append(systemTime)
            .append(">")
            .toString();
    }
}