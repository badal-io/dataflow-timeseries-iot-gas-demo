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

import java.util.UUID;


public class LoopingStatefulTimer extends DoFn<KV<String, TableRow>, TableRow> {

    @StateId("key")
    private final StateSpec<ValueState<String>> key = StateSpecs.value(StringUtf8Coder.of());

    @StateId("eventKey")
    private final StateSpec<ValueState<String>> eventKey = StateSpecs.value(StringUtf8Coder.of());

    @TimerId("loopingTimer")
    private final TimerSpec loopingTime = TimerSpecs.timer(TimeDomain.EVENT_TIME);

    @ProcessElement
    public void process(
        ProcessContext c, 
        @StateId("key") ValueState<String> key,
        @StateId("eventKey") ValueState<String> eventKey,
        @TimerId("loopingTimer") Timer loopingTimer) {

            TableRow row = c.element().getValue();

            Instant nextTimerTimeBasedOnCurrentElement = c.timestamp().plus(Duration.standardSeconds(10));
            loopingTimer.set(nextTimerTimeBasedOnCurrentElement);

            if (key.read() == null) {
                key.write(c.element().getKey());
            }

            String eventKeyString = new String();
            if (eventKey.read() == null) {
                String uuid = UUID.randomUUID().toString();
                eventKey.write(uuid);
                eventKeyString = uuid;
            } else {
                eventKeyString = eventKey.read();
            }

            row.set("event_id", eventKeyString);
            c.output(row);
    }

    @OnTimer("loopingTimer")
    public void onTimer(
        OnTimerContext c,
        @StateId("key") ValueState<String> key,
        @StateId("eventKey") ValueState<String> eventKey,
        @TimerId("loopingTimer") Timer loopingTimer) {

            Instant nextTimer = c.timestamp().plus(Duration.standardSeconds(10));
            loopingTimer.set(nextTimer);

            String nextUUID = UUID.randomUUID().toString();
            eventKey.write(nextUUID);
        }       
}