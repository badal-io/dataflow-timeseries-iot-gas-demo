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

import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.Data;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSAccum;
import com.google.protobuf.util.Timestamps;
import org.apache.beam.sdk.extensions.protobuf.ProtoMessageSchema;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.FieldType;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.values.Row.FieldValueBuilder;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.joda.time.Instant;

public class TSAccumToRowPivot extends PTransform<PCollection<TSAccum>, PCollection<Row>> {

  public static final String TIMESERIES_MAJOR_KEY = "timeseries_key";
  public static final String TIMESERIES_MINOR_KEY = "timeseries_minor_key";
  public static final String UPPER_WINDOW_BOUNDARY = "upper_window_boundary";
  public static final String LOWER_WINDOW_BOUNDARY = "lower_window_boundary";
  public static final String IS_GAP_FILL_VALUE = "is_gap_fill_value";
  public static final String DATA = "data";

  public static Schema tsAccumRowSchema() {

    return Schema.builder()
        .addStringField(TIMESERIES_MAJOR_KEY)
        .addStringField(TIMESERIES_MINOR_KEY)
        .addDateTimeField(LOWER_WINDOW_BOUNDARY)
        .addDateTimeField(UPPER_WINDOW_BOUNDARY)
        .addBooleanField(IS_GAP_FILL_VALUE)
        .addNullableField("RELATIVE_STRENGTH_INDICATOR", FieldType.DOUBLE)
        .addNullableField("FIRST", FieldType.DOUBLE)
        .addNullableField("LAST", FieldType.DOUBLE)
        .addNullableField("SIMPLE_MOVING_AVERAGE", FieldType.DOUBLE)
        .build();
  }

  @Override
  public PCollection<Row> expand(PCollection<TSAccum> input) {

    input
        .getPipeline()
        .getSchemaRegistry()
        .registerSchemaProvider(TSAccum.class, new ProtoMessageSchema());

    return input
        .apply(MapElements.into(TypeDescriptors.rows()).via(toRow()))
        .setRowSchema(tsAccumRowSchema());
  }

  public static SerializableFunction<TSAccum, Row> toRow() {

    return new SerializableFunction<TSAccum, Row>() {
      @Override
      public Row apply(TSAccum input) {

        Data rsi = input.getDataStoreMap().get("RELATIVE_STRENGTH_INDICATOR");
        Data first = input.getDataStoreMap().get("FIRST");
        Data last = input.getDataStoreMap().get("LAST");
        Data avg = input.getDataStoreMap().get("SIMPLE_MOVING_AVERAGE");

        FieldValueBuilder row =
            Row.withSchema(tsAccumRowSchema())
                .withFieldValue(TIMESERIES_MAJOR_KEY, input.getKey().getMajorKey())
                .withFieldValue(TIMESERIES_MINOR_KEY, input.getKey().getMinorKeyString())
                .withFieldValue(IS_GAP_FILL_VALUE, input.getHasAGapFillMessage())
                .withFieldValue(
                    LOWER_WINDOW_BOUNDARY,
                    Instant.ofEpochMilli(Timestamps.toMillis(input.getLowerWindowBoundary())))
                .withFieldValue(
                    UPPER_WINDOW_BOUNDARY,
                    Instant.ofEpochMilli(Timestamps.toMillis(input.getUpperWindowBoundary())))
                .withFieldValue("RELATIVE_STRENGTH_INDICATOR", rsi.getDoubleVal())
                .withFieldValue("FIRST", first.getDoubleVal())
                .withFieldValue("LAST", last.getDoubleVal())
                .withFieldValue("SIMPLE_MOVING_AVERAGE", avg.getDoubleVal());

        return row.build();
      }
    };
  }
}
