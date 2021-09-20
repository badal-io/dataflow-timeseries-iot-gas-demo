/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foglamp;

import com.foglamp.utils.ParseTSDataPointFromPubSub;
import com.foglamp.utils.TSAccumToRowPivot;
import com.foglamp.utils.TimeSeriesOptions;
import com.foglamp.utils.messageParsing.JsonToTableRow;
import com.foglamp.utils.messageParsing.FilterRow;
import com.google.api.services.bigquery.model.TableRow;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSAccum;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSDataPoint;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSKey;
import com.google.dataflow.sample.timeseriesflow.graph.GenerateComputations;
import com.google.dataflow.sample.timeseriesflow.metrics.core.typetwo.basic.ma.MAFn.MAAvgComputeMethod;
import java.io.IOException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.vendor.grpc.v1p26p0.com.google.common.collect.ImmutableList;
import org.joda.time.Duration;
import org.joda.time.Instant;

public class IoTStreamBigQuery {

  public static void main(String[] args) throws IOException {

    TimeSeriesOptions options = PipelineOptionsFactory.fromArgs(args).as(TimeSeriesOptions.class);

    Instant now = Instant.parse("2000-01-01T00:00:00Z");

    options.setAppName("TimeSeriesIoTDataflow");
    options.setGapFillEnabled(true);
    options.setAbsoluteStopTimeMSTimestamp(now.plus(Duration.standardSeconds(43200)).getMillis());
    options.setTypeOneBasicMetrics(ImmutableList.of("typeone.Sum", "typeone.Min", "typeone.Max"));
    options.setTypeTwoBasicMetrics(
        ImmutableList.of("typetwo.basic.ma.MAFn", "typetwo.basic.stddev.StdDevFn"));
    options.setTypeTwoComplexMetrics(ImmutableList.of("complex.fsi.rsi.RSIGFn"));
    options.setMAAvgComputeMethod(MAAvgComputeMethod.SIMPLE_MOVING_AVERAGE);
    options.setStreaming(true);

    String destination_table = options.getOutputTable();
    Pipeline pipeline = Pipeline.create(options);

    PCollection<TableRow> messages = null;
    messages =
        pipeline
            .apply(
                "Pull PubSub Messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
            .apply("To TableRows", new JsonToTableRow())
            .apply("Filter Rows", ParDo.of(new FilterRow()));

    PCollection<TSDataPoint> stream =
        messages.apply("To TS Data Point", ParDo.of(new ParseTSDataPointFromPubSub()));

    GenerateComputations generateComputations =
        GenerateComputations.fromPiplineOptions(options).build();

    PCollection<KV<TSKey, TSAccum>> computations = stream.apply(generateComputations);

    computations
        .apply(Values.create())
        .apply(new TSAccumToRowPivot())
        .apply(
            "Write to BigQuery",
            BigQueryIO.<Row>write()
                .useBeamSchema()
                .withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(WriteDisposition.WRITE_APPEND)
                .to(destination_table));

    pipeline.run();
  }
}