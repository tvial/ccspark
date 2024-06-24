# What it is about

This project is directly inspired by [CodeCarbon](http://codecarbon.io). One current limitation
of CodeCarbon is its inability to track the emissions of a computation distributed on a cluster.
For example, if one submits a PySpark job to a Spark cluster, monitoring its emission with
`EmissionTracker` will only estimate the emissions of the driver process, whereas most of the
work is done by remote machines.

The project specifically aims at tracking the energy consumption of a Spark job.

The version of Spark used for development and testing is 3.5.1. The source code is compatible with the
Java 8 SDK, and should be compiled for this target, as it seems to match the Databricks runtime.


# What it is

Concretely, it takes the form of a Spark plugin, written in Java. It is enabled by adding its JAR
on the classpath at submit time and referencing its plugin class as a configuration parameter; Spark
will take care of the rest.

Each executor taking part in the job will periodically sample its own load on the worker, convert it
to an energy estimation, and report the measure to driver where it will be aggregated over time and
across all workers. The final figure is the energy consumption of the cluster over the lifetime of the
Spark context.

The ingredient used to convert from loaded seconds to energy (a number of Wh) is the Thermal Design
Power (TDP) of the processors. On a given processor model, the TDP is an estimation of the power
drawn by some benchmark. The benchmark is not known, it can vary between models, but since it serves
as a soft limit for engineering the heat dissipation system, it is considered a reliable estimate
of the consumption under heavy load.

The plugin is very lightweight, with no third-party dependencies. This is to avoid JAR conflicts
leading to unpredictable behaviour at deploy time.


# What it is not (shortcomings)

The plugin will not factor in the PUE of the datacenter where the job executes, nor will it convert
it to CO2eq by looking at the local carbon intensity. I believe that CodeCarbon does this very well
already, and working on a tighter integration with the latter is preferrable over rewriting parts of
its logic.

So, the plugin focuses on what it only can do: monitoring at places that CodeCarbon cannot see.


# Using the plugin

You can build the plugin with Maven:

```
cd /path/to/source/code/java
mvn package
```

This will create the JAR in the `target/` directory.

Here is a typical command to run it (using PySpark):

```
spark-submit \
    --jars path/to/jar/ccspark-XXX.jar \
    --conf spark.plugins=io.github.tvial.ccspark.plugin.CCSparkPlugin \
    entrypoint.py
```

If you want to override the TDP (details below), because the detected or default one is incorrect:

```
spark-submit \
    --jars path/to/jar/ccspark-XXX.jar \
    --conf spark.plugins=io.github.tvial.ccspark.plugin.CCSparkPlugin \
    --conf spark.ccspark.cpu.tdp=123 \
    entrypoint.py
```

This will assume a global, constant TDP of 123 W.

## Reading the estimated energy from PySpark code

All the calculations are done in the driver, and there must be a way to get the information back. For
example, one would like to store the estimated consumption somewhere, such as MLFlow. For this, the driver
registers a metric with name `"energy.total_Wh" with Spark's metric system.

From a PySpark perspective, the metrics are accessible through the `/metrics` REST endpoint of the Spark
master (there is no simple access via the Spark context). Here is some sample code to read the energy
consumption from there:

```
from pyspark.sql import SparkSession

import requests


def read_energy(sc) -> float:
    # The value returned is the energy spent since the Spark context was created
    # We should do some basic error checking
    metrics_url = sc.uiWebUrl + '/metrics/json'
    gauges = requests.get(metrics_url).json()['gauges']
    key = next(key for key in gauges.keys() if key.endswith('energy.total_Wh'))
    return gauges[key]['value']


spark = SparkSession.builder.getOrCreate()

energy_before = read_energy(spark.sparkContext)

# ... do something sparky

energy_after = read_energy(spark.sparkContext)
print('>>> Energy consumed:', energy_after - energy_before, 'Wh')
```


# Methodology & limitations

Currently, the calculation method is crude and naive; there is only one such method. But others could
be added rather easily; the code is written for extensibility. Also, it assumes a Linux environment.
And we only track the CPU, not the GPU or RAM.

So, how does it work? It's based on the following formula:

```
Energy (Wh) = TDP (W) * Time (h) * Load
```

## The TDP

The TDP is either read from the configuration, or obtained from the worker host. Note that in the first
case, the cluster should be homogeneous, otherwise the calculations will be biased.

In the second case, we take the same approach as CodeCarbon, with simplifications: the CPU model is
read from the first `"model name"` entry in `proc/cpuinfo`, then looked up in a CSV file. The file
is shamelessly taken from CodeCarbon (`data/hardware/cpu_power.csv`), after a tiny bit of
preprocessing -- the TDPs of identical model names are averaged.

Looking up means simplifying names by removing meaningless tokens such as `(R)`, `(TM)` or
`CPU @ x.xxGHz`. CodeCarbon, on the other hand, relies on fuzzy string matching, which is probably
way more robust. Our solution is too sensitive to variations on model names. In the absence of a
match, we read a cluster-wide default value from configuration, or, if not specified, the same default
as CodeCarbon, i.e. 85 W.

## The time

The time is simply the interval between two consecutive samples, as given by Java's
`System.currentTimeMillis()`. It should be close to the sampling period.

## The load

For this, we read the time alloted to all CPUs on the machine (from `proc/stat`), and that alloted to
the process of the executor (from `/proc/<pid>/stat`). More precisely:

- for the CPU, we take the first `"cpu"` entry, which aggregates all cores. We sum user, system and
  niced time
- for the process, we sum the user, system, children user and children system time

This assumes that one executor is materialized by one process, or at least one process plus children
that are effectively tracked by the children user & system times in `/proc/<pid>/stat`. As a Spark 
executor plugin, we are only aware of ourselves, so if there are other processes lying around, we
ignore them.

The load associated with the executor is simply the ratio: difference of process time over difference of
CPU time. The differences are computed between two sampling events; if the period is not too large, this
would make a good estimate of the average load.

A nice touch is that it makes the calculation independent of where executors are hosted -- if two
run side by side on the same worker node, we can add their loads confidently.

A not-so-nice side effect happens when the cluster is shared by several applications: the executors will
be shared too, and so will the driver; the contributions of all jobs will be aggregated by the latter. In
that case, it's better to monitor the energy consumption globally and not by each job. There is no way to
break down that consumption after the fact.


# Other possible methodologies, and their shortcomings

Other ideas were considered, and left aside for various reasons.

## Using RAPL to track actual energy consumption

This is a preferred method, as the energy is directly reported by the hardware and is much more precise
than our estimations (one notable source of uncertainty being the TDP, which represents a "typical"
workload, whatever that means).

It's CodeCarbon's default way of doing and should be easy to replicate in the plugin code. One limitation
is that the measurements are not accessible by a non-root process; when using a managed cluster, one
has very limited control on the underlying OS and permissions. Take that with a grain of salt: some Databricks
execution modes ("Shared Isolation") seem to run jobs as root.

Overall, the best approach would be to try RAPL by default, and fall back on our method.

## Using CPU scheduled time only

This one would ignore the time when the process is scheduled, considering the CPU time only. It would
make a lot of sense for dedicated clusters, where one is interested in its complete consumption,
including the OS and possibly other processes outside of the Spark job. After all, they contribute too.

What prevents it is that the time is given in clock ticks, and the clock resolution is not easily
accessible in Java. Some libs, such as JNR-POSIX, wrap `sysconf()` calls, but it behaves weirdly.
For example, a small standalone program querying `_SC_CLK_TCK` will report 100 (Hz), but with an uber
jar including Spark and its dependencies, the reported value would become 1000. I believe this is
because the `_SC_CLK_TCK` value ultimately comes from a C header, and so was baked in some Java
class as compilation time on a random machine somewhere. In this scenario, in case of conflicting
values the classpath order would determine the one reported.

TL;DR: I could not find a way to implement this method in Java.


# The code

As per Spark's plugin architecture, there are two parts:
- the driver plugin code (one for the whole job)
- the executor plugin code (one per executor)

The entry point is found in `io.github.tvial.ccspark.plugin.CCSparkPlugin` -- it instantiates the two legs
of the plugin.

## The driver part

Its first role is to register an accumulator with Spark, which will track the cumlative energy consumption
of the job over time, and a metric for reporting by Sparl's metric system.

The spot consumptions sent by the workers are received thanks to the `receive()` method. They are
simply added together. So the driver plugin does not do any calculations beside aggregation.

## The executor part

The executor plugin is the glue between:
- load sampling (`io.github.tvial.ccspark.sampling.ProcFileSystemSampleProvider` class)
- interface with the driver plugin (`io.github.tvial.ccspark.plugin.SparkDriverUsageMetricsSink.SparkDriverUsageMetricsSink`)
- a monitoring thread that polls the former and notifies the latter (`io.github.tvial.ccspark.monitoring.UsageMonitor.UsageMonitor`)

The `UsageMonitor` is also where the energy calculations happen. It periodically measures the variation
of load on the executor, multiples it by the TDP, and sends that estimation to the metrics sink.

## General considerations on the code

The code is rather simple. The use of interfaces for seemingly trivial tasks might look like
over-engineering, but it makes the code easier to test. Also, if several calculation methods become
available in the future, it should make the modifications smooth.


# License

Licensed under the Apache 2.0 terms (see `LICENSE` file).