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

import com.google.dataflow.sample.timeseriesflow.metrics.core.TSMetricsOptions;
import com.google.dataflow.sample.timeseriesflow.options.TFXOptions;
import com.google.dataflow.sample.timeseriesflow.options.TSOutputPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation.Required;

public interface TimeSeriesOptions
    extends TSOutputPipelineOptions,
        TSMetricsOptions,
        TFXOptions,
        PipelineOptions,
        StreamingOptions {
  @Description("The Cloud Pub/Sub topic to read from")
  @Required
  String getInputTopic();

  void setInputTopic(String value);

  @Description("BigQuery output table")
  @Required
  String getOutputTable(); // "[project_id]:[dataset_id].[table_id]"

  void setOutputTable(String value);

  @Description(
      "In order to see easy output of metrics for demos set this to true. This will result in all values being 'printed' to logs.")
  @Default.Boolean(false)
  Boolean getEnablePrintMetricsToLogs();

  void setEnablePrintMetricsToLogs(Boolean value);

  @Description(
      "In order to see easy output of TF.Examples for demos set this to true. This will result in all values being 'printed' to logs.")
  @Default.Boolean(false)
  Boolean getEnablePrintTFExamplesToLogs();

  void setEnablePrintTFExamplesToLogs(Boolean value);
}
