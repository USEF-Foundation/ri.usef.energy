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

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;

import org.joda.time.LocalDate;

/**
 * Abstract class child of {@link DeviceRequest}. A {@link VariationRequest} can either be a request to reduce the power for a
 * certain duration or increase the power for a certain duration (both durations given by a Start DTU and a End DTU).
 */
@MappedSuperclass
public abstract class VariationRequest extends DeviceRequest {

    @Column(name = "EVENT_ID", nullable = false)
    private String eventId;
    @Column(name = "START_DTU", nullable = true)
    private Integer startDtu;
    @Column(name = "END_DTU", nullable = true)
    private Integer endDtu;
    @Column(name = "POWER", nullable = false, precision=18, scale=0)
    private BigInteger power;
    @Column(name = "CONSUMPTION_PRODUCTION_TYPE",
            length = 16)
    @Enumerated(value = EnumType.STRING)
    private ConsumptionProductionType consumptionProductionType;

    /**
     * Default constructor.
     *
     * @param period {@link String} period of the request.
     */
    protected VariationRequest(LocalDate period) {
        // do nothing
        super(period);
    }

    /**
     * Creates a new {@link VariationRequest} with the given fields.
     *  @param period {@link LocalDate} period of the request.
     * @param eventId {@link String} Event ID.
     * @param startDtu {@link Integer} Start DTU. Can be <code>null</code>.
     * @param endDtu {@link Integer} End DTU. Can be <code>null</code>.
     * @param power {@link BigInteger} amount of power to vary. Negative value indicates production of power.
     * @param consumptionProductionType {@link ConsumptionProductionType} the value CONSUMPTION or PRODUCTION.
     */
    protected VariationRequest(LocalDate period, String eventId, Integer startDtu, Integer endDtu, BigInteger power,
            ConsumptionProductionType consumptionProductionType) {
        this(period);
        this.eventId = eventId;
        this.startDtu = startDtu;
        this.endDtu = endDtu;
        this.power = power;
        this.consumptionProductionType = consumptionProductionType;
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

    public Integer getEndDtu() {
        return endDtu;
    }

    public void setEndDtu(Integer endDtu) {
        this.endDtu = endDtu;
    }

    public BigInteger getPower() {
        return power;
    }

    public void setPower(BigInteger power) {
        this.power = power;
    }

    public ConsumptionProductionType getConsumptionProductionType() {
        return consumptionProductionType;
    }

    public void setConsumptionProductionType(ConsumptionProductionType consumptionProductionType) {
        this.consumptionProductionType = consumptionProductionType;
    }
}
