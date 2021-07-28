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

import java.util.Map;
import java.util.HashMap;


public class ConditionEvaluator {

    public static Boolean Evaluate(Double value, Double baseline_value, String opString) {
        Map<String, Operator> operatorMap = ConditionEvaluator.createOperatorMap();
        Operator op = operatorMap.get(opString);
        return op.compute(value, baseline_value);
    }

    abstract class Operator {
        public abstract Boolean compute(Double value, Double baseline_value);
    }

    class Lesser extends Operator {
        public Boolean compute(Double value, Double baseline_value) {
            return value < baseline_value;
        }
    }

    class Greater extends Operator {
        public Boolean compute(Double value, Double baseline_value) {
            return value > baseline_value;
        }
    }

    class Equal extends Operator {
        public Boolean compute(Double value, Double baseline_value) {
            return value == baseline_value;
        }
    }
    
    static Map createOperatorMap() {
        ConditionEvaluator evaluator = new ConditionEvaluator();

        Map<String, Operator> map = new HashMap<String, Operator>();
        map.put("<", evaluator.new Lesser());
        map.put(">", evaluator.new Greater());
        map.put("==", evaluator.new Equal());
        return map;
    }
}