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

import com.foglamp.utils.ConditionEvaluator;
import com.foglamp.utils.LoopingStatefulTimer;

import java.util.Map;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;


public class EventFilter {
    static class GenerateKeys extends DoFn<TableRow, KV<String, TableRow>> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            TableRow row = c.element();
            String device_id = (String) row.get("device_id");
            String property_measured = (String) row.get("property_measured");
            String key = String.format("%s#%s", property_measured, device_id);
            c.output(
                KV.of(key, row)
            );
        }
    }

    public static PCollectionTuple FilterRows(
        PCollection<TableRow> flat_rows,
        PCollection<TableRow> event_definitions,
        TupleTag<TableRow> all_measurements,
        TupleTag<TableRow> event_measurements
        ) {

            PCollection<KV<String, TableRow>> flat_rows_keyed = flat_rows.apply(ParDo.of(new GenerateKeys()));
            PCollection<KV<String, TableRow>> event_definitions_keyed = event_definitions.apply(ParDo.of(new GenerateKeys()));

            PCollectionView<Map<String, Iterable<TableRow>>> view = event_definitions_keyed.apply(View.<String, TableRow>asMultimap());
            
            PCollectionTuple results = 
                flat_rows_keyed.apply("Detect Events", ParDo
                    .of(new DoFn<KV<String, TableRow>, TableRow>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        TableRow row = c.element().getValue();
                        String LookUpKey = c.element().getKey();

                        Iterable<TableRow> EventDefinitionRows = c.sideInput(view).get(LookUpKey);

                        String property_measured = (String) row.get("property_measured");
                        Double value = (Double) row.get("value");

                        String row_event_type = (String) row.get("event_type");

                        if (row_event_type == "Device Version Change") {
                            c.output(event_measurements, row);
                        } else if (row_event_type == "Device Error") {
                            c.output(event_measurements, row);
                            row.remove("event_type");
                            row.remove("severity");
                            row.remove("comments");
                            c.output(row);
                        } else {

                            c.output(row);

                            if (EventDefinitionRows != null ) {
                                for (TableRow entry : EventDefinitionRows) {
                                    String event_type = (String) entry.get("event_type");
                                    String condition = (String) entry.get("condition");
                                    String severity = (String) entry.get("severity");
                                    Double baseline_value = (Double) entry.get("baseline_value");
                                    
                                    Boolean evaluation = ConditionEvaluator.Evaluate(value, baseline_value, condition);

                                    if (evaluation == true) {
                                        row.set("event_type", event_type);
                                        row.set("severity", severity);
                                        c.output(event_measurements, row);
                                    }
                                }
                            }
                        }
                    }})
                    .withSideInputs(view)
                    .withOutputTags(all_measurements, TupleTagList.of(event_measurements))
                );
            
            return results;
    }
}