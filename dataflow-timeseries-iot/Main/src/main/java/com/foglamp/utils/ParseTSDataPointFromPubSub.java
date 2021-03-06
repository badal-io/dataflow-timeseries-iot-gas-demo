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
package com.foglamp.utils;

import com.google.api.services.bigquery.model.TableRow;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.Data;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSDataPoint;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSKey;
import com.google.protobuf.util.Timestamps;
import org.apache.beam.sdk.transforms.DoFn;
import org.joda.time.Instant;

public class ParseTSDataPointFromPubSub extends DoFn<TableRow, TSDataPoint> {

  public static ParseTSDataPointFromPubSub create() {
    return new ParseTSDataPointFromPubSub();
  }

  @ProcessElement
  public void process(
      @Element TableRow input, @Timestamp Instant sourceTimestamp, OutputReceiver<TSDataPoint> o) {
    String device_id = (String) input.get("device_id");
    String property = (String) input.get("property_measured");
    Double value = (Double) input.get("value");

    if (property != null && value != null) {
      TSKey key = TSKey.newBuilder().setMajorKey(device_id).setMinorKeyString(property).build();

      TSDataPoint dataPoint =
          TSDataPoint.newBuilder()
              .setKey(key)
              .setTimestamp(Timestamps.fromMillis(sourceTimestamp.getMillis()))
              .setData(Data.newBuilder().setDoubleVal(value))
              .build();

      o.output(dataPoint);
    }
  }
}