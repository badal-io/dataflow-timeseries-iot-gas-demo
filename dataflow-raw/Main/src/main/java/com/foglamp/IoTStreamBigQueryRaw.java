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

import com.foglamp.utils.BigQuerySchemaCreate;
import com.foglamp.utils.Options;
import com.foglamp.utils.messageParsing.JsonToTableRow;
import com.foglamp.utils.LoopingStatefulTimer;
import com.foglamp.utils.EventFilter;
import com.foglamp.utils.customFn.CreateKey;
import com.foglamp.utils.customFn.RemoveKey;
import com.foglamp.utils.messageParsing.FormatJson;
import com.foglamp.utils.messageParsing.TableRowFormat;
import com.foglamp.utils.messageParsing.GsonConvertToString;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.io.IOException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.windowing.Sessions;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.joda.time.Duration;

import org.apache.beam.sdk.io.TextIO;

public class IoTStreamBigQueryRaw {

  public static void main(String[] args) throws IOException {

    Options options = PipelineOptionsFactory.fromArgs(args).as(Options.class);
    options.setStreaming(true);

    String input_table = options.getInputTable();
    String destination_table = options.getOutputTable();
    String input_topic = options.getInputTopic();
    String output_topic = options.getOutputTopic();
    String output_event_topic = options.getOutputEventTopic();
    int timer_size = options.getTimerSize();

    Pipeline pipeline = Pipeline.create(options);

    String query = "SELECT event_type, device_id, property_measured, condition, baseline_value FROM `%s`, UNNEST(device_id) device_id";
    PCollection<TableRow> event_definitions = 
      pipeline
        .apply(
          "Event Definitions BigQuery", 
          BigQueryIO.readTableRows()
            .fromQuery(String.format(query, input_table))
            .usingStandardSql());

    PCollection<TableRow> messages = null;
    messages =
        pipeline
            .apply("Pull PubSub Messages", PubsubIO.readStrings().fromTopic(input_topic))
            .apply("Format JSON", ParDo.of(new FormatJson()))
            .apply("To TableRows", new JsonToTableRow());

    PCollection<Iterable<TableRow>> message_strings = messages.apply("Format TableRows", ParDo.of(new TableRowFormat()));
    PCollection<TableRow> flat_rows = message_strings.apply("Flatten TableRows", Flatten.iterables());

    PCollection<TableRow> flat_rows_with_timer = flat_rows
            .apply("Device-Id Keys", ParDo.of(new CreateKey()))
            .apply("Looping Stateful Timer", ParDo.of(new LoopingStatefulTimer(timer_size)));
    
    final TupleTag<TableRow> all_measurements = new TupleTag<TableRow>(){};
    final TupleTag<TableRow> event_measurements = new TupleTag<TableRow>(){};

    PCollectionTuple results = EventFilter.FilterRows(flat_rows_with_timer, event_definitions, all_measurements, event_measurements);
  
    PCollection<TableRow> all_measurement_rows = results.get(all_measurements);
    PCollection<TableRow> event_measurement_rows = results.get(event_measurements);

    TableSchema schema = BigQuerySchemaCreate.createSchema();
    all_measurement_rows.apply(
        "Write to BigQuery",
        BigQueryIO.writeTableRows()
            .withSchema(schema)
            .withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(WriteDisposition.WRITE_APPEND)
            .to(destination_table));

    PCollection<String> output_raw = all_measurement_rows.apply("Rows to String", ParDo.of(new GsonConvertToString()));
    PCollection<String> output_events = event_measurement_rows.apply("Events to String", ParDo.of(new GsonConvertToString()));
    
    output_raw.apply("Rows to PubSub", PubsubIO.writeStrings().to(output_topic)); 
    output_events.apply("Events to PubSub", PubsubIO.writeStrings().to(output_event_topic)); 

    pipeline.run();
  }
}