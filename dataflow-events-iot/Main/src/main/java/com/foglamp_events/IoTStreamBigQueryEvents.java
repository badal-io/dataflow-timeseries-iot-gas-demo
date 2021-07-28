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
package com.foglamp_events;

import com.foglamp_events.utils.BigQuerySchemaCreate;
import com.foglamp_events.utils.Options;
import com.foglamp_events.utils.customFn.CreateKey;
import com.foglamp_events.utils.customFn.RemoveKey;
import com.foglamp_events.utils.customFn.CreateTimestamp;
import com.foglamp_events.utils.messageParsing.JsonToTableRow;
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
import org.apache.beam.sdk.transforms.windowing.Sessions;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

public class IoTStreamBigQueryEvents {

  public static void main(String[] args) throws IOException {

    Options options = PipelineOptionsFactory.fromArgs(args).as(Options.class);
    options.setStreaming(true);

    String destination_table = options.getOutputTable();
    String input_topic = options.getInputTopic();
    int gap_size = options.getGapSize();

    Pipeline pipeline = Pipeline.create(options);

    PCollection<TableRow> messages = null;
    messages =
        pipeline
            .apply("Pull PubSub Messages", PubsubIO.readStrings().fromTopic(input_topic))
            .apply("Convert PubSub messages to TableRow Type", new JsonToTableRow());

    PCollection<KV<String, TableRow>> keyed_messages =
        messages
            .apply("Create key for element", ParDo.of(new CreateKey()))
            .apply(Window.into(Sessions.withGapDuration(Duration.standardSeconds(gap_size))));

    PCollection<KV<String, Iterable<TableRow>>> grouped_messages =
        keyed_messages.apply(GroupByKey.<String, TableRow>create());

    PCollection<Iterable<TableRow>> rows =
        grouped_messages.apply("Extact values", ParDo.of(new RemoveKey()));

    PCollection<TableRow> flat_rows = rows.apply(Flatten.iterables()).apply("Create Timestamp", ParDo.of(new CreateTimestamp()));

    TableSchema schema = BigQuerySchemaCreate.createSchema();
    flat_rows.apply(
        BigQueryIO.writeTableRows()
            .withSchema(schema)
            .withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(WriteDisposition.WRITE_APPEND)
            .to(destination_table));

    pipeline.run().waitUntilFinish();
  }
}
