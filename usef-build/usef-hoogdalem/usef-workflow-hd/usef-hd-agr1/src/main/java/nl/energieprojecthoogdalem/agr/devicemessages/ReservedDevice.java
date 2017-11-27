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

package nl.energieprojecthoogdalem.agr.devicemessages;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * POJO of
 *  {
 *      "startIndex": 70
 *      ,"period": "yyyy-MM-dd"
 *  }
 * */
public class ReservedDevice
{
    private final int startIndex;
    private final LocalDate period;

    @JsonCreator
    public ReservedDevice
    (
        @JsonProperty("startIndex") int startIndex
        ,@JsonProperty("period") String period
    )
    {
        this.startIndex = startIndex;
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
        this.period = dateFormat.parseDateTime(period).toLocalDate();
    }

    public int getStartIndex(){return startIndex;}
    public LocalDate getPeriod()
    {
        return period;
    }
}
