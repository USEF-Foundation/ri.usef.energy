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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.joda.time.LocalDate;

/**
 * Class representing a Shift Request for a device. This class inherits from {@link DeviceRequest}.
 */
@Entity
@Table(name = "SHIFT_REQUEST")
public class ShiftRequest extends DeviceRequest {

    @Column(name = "EVENT_ID", nullable = false)
    private String eventId;
    @Column(name = "START_DTU", nullable = false)
    private Integer startDtu;

    /**
     * Default constructor for JPA.
     */
    public ShiftRequest() {
        super(null);
    }

    /**
     * Default constructor.
     *
     * @param period {@link LocalDate} period of the {@link ShiftRequest}.
     */
    public ShiftRequest(LocalDate period) {
        super(period);
    }

    /**
     * Creates a new {@link ShiftRequest} with the given parameters.
     *
     * @param period {@link LocalDate} period.
     * @param eventId {@link String} event ID as specified by the UDI.
     * @param startDtu {@link Integer} Start DTU of the {@link ShiftRequest}.
     */
    public ShiftRequest(LocalDate period, String eventId, Integer startDtu) {
        super(period);
        this.eventId = eventId;
        this.startDtu = startDtu;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Integer getStartDtu() {
        return startDtu;
    }

    public void setStartDtu(Integer startDtu) {
        this.startDtu = startDtu;
    }
}
