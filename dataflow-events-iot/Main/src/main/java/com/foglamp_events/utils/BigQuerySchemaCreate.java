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
package com.foglamp_events.utils;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.Arrays;

public class BigQuerySchemaCreate {
  public static TableSchema createSchema() {
    TableSchema schema =
        new TableSchema()
            .setFields(
                Arrays.asList(
                    new TableFieldSchema()
                        .setName("device_id")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("event_id")
                        .setType("TIMESTAMP")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("event_type")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("severity")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("device_version")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("comments")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("timestamp")
                        .setType("TIMESTAMP")
                        .setMode("NULLABLE"),                        
                    new TableFieldSchema()
                        .setName("property_measured")
                        .setType("STRING")
                        .setMode("NULLABLE"),
                    new TableFieldSchema()
                        .setName("value")
                        .setType("FLOAT64")
                        .setMode("NULLABLE")));
    return schema;
  }
}
