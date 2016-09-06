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

package energy.usef.agr.model;

import java.math.BigInteger;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.LocalDate;

/**
 * Object used to save a Connection Forecast Snapshot (power per connection per PTU per period). This object is annotated to be
 * serialized in JSON.
 */
public class ConnectionForecastSnapshot {

    @JsonProperty(value = "connectionEntityAddress")
    private String connectionEntityAddress;
    @JsonProperty(value = "power")
    private BigInteger power;
    @JsonProperty(value = "ptuDate")
    private Date ptuDate;
    @JsonProperty(value = "ptuIndex")
    private Integer ptuIndex;
    @JsonProperty(value = "changed")
    private Boolean changed;

    public ConnectionForecastSnapshot() {
        // empty constructor for JPA
    }

    public String getConnectionEntityAddress() {
        return connectionEntityAddress;
    }

    public void setConnectionEntityAddress(String connectionEntityAddress) {
        this.connectionEntityAddress = connectionEntityAddress;
    }

    public BigInteger getPower() {
        return power;
    }

    public void setPower(BigInteger power) {
        this.power = power;
    }

    public LocalDate getPtuDate() {
        if (ptuDate == null) {
            return null;
        }
        return new LocalDate(ptuDate);
    }

    public void setPtuDate(LocalDate ptuDate) {
        if (ptuDate == null) {
            this.ptuDate = null;
        } else {
            this.ptuDate = ptuDate.toDateMidnight().toDate();
        }
    }

    public Integer getPtuIndex() {
        return ptuIndex;
    }

    public void setPtuIndex(Integer ptuIndex) {
        this.ptuIndex = ptuIndex;
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    @Override
    public String toString() {
        return "ConnectionForecastSnapshot" + "[" +
                "connectionEntityAddress='" + connectionEntityAddress + "'" +
                ", power=" + power +
                ", ptuDate=" + ptuDate +
                ", ptuIndex=" + ptuIndex +
                ", changed=" + changed +
                "]";
    }
}
