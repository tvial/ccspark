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

import io.github.tvial.ccspark.model.Sample;


public class InMemorySampleProvider implements SampleProvider {
    private Sample[] samples;
    private int index;
    
    public InMemorySampleProvider(Sample[] samples) {
        this.samples = samples;
        this.index = 0;
    }

    @Override
    public Sample sample() {
        return samples[index++];
    }
}
