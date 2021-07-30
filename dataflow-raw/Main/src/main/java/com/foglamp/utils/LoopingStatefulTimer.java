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
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.coders.BigEndianLongCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;


import org.joda.time.Duration;
import org.joda.time.Instant;
import com.google.protobuf.util.Timestamps;


public class LoopingStatefulTimer extends DoFn<KV<String, TableRow>, TableRow> {

    int timerExpiry;

    public LoopingStatefulTimer(int duration) {
        this.timerExpiry = duration;
    }

    @StateId("key")
    private final StateSpec<ValueState<String>> key = StateSpecs.value(StringUtf8Coder.of());

    @StateId("deviceVersion")
    private final StateSpec<ValueState<String>> deviceVersion = StateSpecs.value(StringUtf8Coder.of());

    @TimerId("loopingTimer")
    private final TimerSpec loopingTime = TimerSpecs.timer(TimeDomain.EVENT_TIME);

    @ProcessElement
    public void process(
        ProcessContext c, 
        @StateId("key") ValueState<String> key,
        @StateId("deviceVersion") ValueState<String> deviceVersion,
        @TimerId("loopingTimer") Timer loopingTimer) {

            TableRow row = c.element().getValue();

            String newDeviceVersion = (String) row.get("device_version");
            String oldDeviceVersion = deviceVersion.read();

            Instant nextTimerTimeBasedOnCurrentElement = c.timestamp().plus(Duration.standardSeconds(timerExpiry));

            loopingTimer.set(nextTimerTimeBasedOnCurrentElement);

            if (key.read() == null) {
                key.write(c.element().getKey());
            }

            if (oldDeviceVersion == null) {
                deviceVersion.write(newDeviceVersion);
            } else if ( !newDeviceVersion.equals(oldDeviceVersion) ) {
                Instant timestamp = c.timestamp();
                String ts = timestamp.toString();

                String comment = String.format("Device version changed from %s to %s", oldDeviceVersion, newDeviceVersion);

                TableRow output = new TableRow()
                    .set("device_id", c.element().getKey())
                    .set("event_type", "Device Version Change")
                    .set("timestamp", ts)
                    .set("value", null)
                    .set("severity","Low")
                    .set("property_measured", "Device Version Change")
                    .set("device_version", newDeviceVersion)
                    .set("comments", comment);
                deviceVersion.write(newDeviceVersion);
                c.output(output);     
            }

            c.output(row);
    }

    @OnTimer("loopingTimer")
    public void onTimer(
        OnTimerContext c,
        @StateId("key") ValueState<String> key,
        @StateId("deviceVersion") ValueState<String> deviceVersion,
        @TimerId("loopingTimer") Timer loopingTimer) {

            String device_id = key.read();

            Instant timestamp = c.timestamp();
            String ts = timestamp.toString();

            TableRow new_row = new TableRow()
                .set("device_id", device_id)
                .set("event_type", "Device Error")
                .set("timestamp", ts)
                .set("value", null)
                .set("severity","High")
                .set("property_measured", "Device Error")
                .set("comments", "Connectivity Lost")
                .set("device_version", deviceVersion.read());
            c.output(new_row);

            Instant nextTimer = c.timestamp().plus(Duration.standardSeconds(timerExpiry));

            loopingTimer.set(nextTimer);
        }       
}