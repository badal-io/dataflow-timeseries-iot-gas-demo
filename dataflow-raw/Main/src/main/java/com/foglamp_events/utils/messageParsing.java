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
import com.google.protobuf.ByteString;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.beam.sdk.coders.Coder.Context;
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;
import java.util.Map.Entry;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class messageParsing {
  public static TableRow convertJsonToTableRow(String json) {
    TableRow row;

    try {
      InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
      row = TableRowJsonCoder.of().decode(inputStream, Context.OUTER);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize json to table row: " + json, e);
    }

    return row;
  }

  public static class JsonToTableRow
      extends PTransform<PCollection<String>, PCollection<TableRow>> {

    @Override
    public PCollection<TableRow> expand(PCollection<String> stringPCollection) {
      return stringPCollection.apply(
          "Convert JSON to TableRow",
          MapElements.via(
              new SimpleFunction<String, TableRow>() {
                @Override
                public TableRow apply(String json) {
                  return convertJsonToTableRow(json);
                }
              }));
    }
  }

  /*
  public static class TableRowToPubSubMessage extends DoFn<TableRow, PubsubMessage> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();

      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      try {
        TableRowJsonCoder.of().encode(row, outStream, Context.OUTER);
      } catch (IOException e) {
      throw new RuntimeException("Failed to product output stream");
      }
      ByteString data = ByteString.copyFrom(outStream.toByteArray());
      PubsubMessage message = PubsubMessage.setData(data);
      c.output(message);
    }
  }
  */

  public static class FormatJson extends DoFn<String, String> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      String input = c.element();
      c.output(input.replaceAll("}]\"", "}],\""));
    }
  }

  public static class TableRowFormat extends DoFn<TableRow, Iterable<TableRow>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();
      List<TableRow> TableRowList = new ArrayList<TableRow>();

      String property_measured = "";
      Double value = 0.0;
      for (Entry<String, Object> field : row.entrySet()) {
        String device_id = field.getKey();

        List list_of_measurements = (List) field.getValue();
        Iterator iterator = list_of_measurements.iterator();
        while (iterator.hasNext()) {
          LinkedHashMap map = (LinkedHashMap) iterator.next();
          Set EntrySet = (Set) map.entrySet();
          Iterator iterator_set = EntrySet.iterator();
          while (iterator_set.hasNext()) {
            Entry element = (Entry) iterator_set.next();
            if (element.getKey() != "ts") {
              property_measured = (String) element.getKey();
              value = (Double) element.getValue();
            }
          }
          TableRow new_row = new TableRow()
                        .set("device_id", device_id)
                        .set("timestamp", map.get("ts"))
                        .set("value", value)
                        .set("property_measured", property_measured)
                        .set("units_of_measurement", null);
          TableRowList.add(new_row);
        }
      }
      Iterable<TableRow> iterable = TableRowList;
      c.output(iterable);
    }
  }

  public static class ConvertToString extends DoFn<TableRow, String> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();
      c.output(row.toString());
    }
  }

  public static class GsonConvertToString extends DoFn<TableRow, String> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();
      Gson gson = new Gson();	
      c.output(gson.toJson(row));
    }
  }
}