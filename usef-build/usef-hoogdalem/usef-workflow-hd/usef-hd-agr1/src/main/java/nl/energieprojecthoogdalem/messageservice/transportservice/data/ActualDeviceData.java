/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.energieprojecthoogdalem.messageservice.transportservice.data;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.math.BigInteger;

/**
 * pojo of json :
 * {
 *      "device": "PV"
 *      , "value" : "500"
 * }
 *
 * */
public class ActualDeviceData
{
    private final String device;
    private final BigInteger value;

    @JsonCreator
    public ActualDeviceData
            (
                    @JsonProperty("device") String device
                    ,@JsonProperty("value") BigInteger value
            )
    {
        this.device = device;
        this.value = value;
    }

    public String getDevice(){return device;}
    public BigInteger getValue(){return value;}
}
