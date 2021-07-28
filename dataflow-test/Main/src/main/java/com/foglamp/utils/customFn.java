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
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;

public class customFn {
  public static class CreateKey extends DoFn<TableRow, KV<String, TableRow>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();
      String key = (String) row.get("device_id");
      c.output(KV.of(key, row));
    }
  }

  public static class RemoveKey extends DoFn<KV<String, Iterable<TableRow>>, Iterable<TableRow>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      Iterable<TableRow> rows = c.element().getValue();
      c.output(rows);
    }
  }

  public static class CreateTimestamp extends DoFn<TableRow, TableRow> {
    @ProcessElement
    public void process(@Element TableRow input, @Timestamp Instant timestamp, OutputReceiver<TableRow> o) {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
      String window_id = timestamp.toString(formatter);
      input.set("window_id", window_id);
      o.output(input);
    }
  }
}