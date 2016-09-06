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

import javax.persistence.Entity;
import javax.persistence.Table;

import org.joda.time.LocalDate;

/**
 * Class representing a Power Increase request (production or consumption), child of {@link VariationRequest}.
 */
@Entity
@Table(name = "INCREASE_REQUEST")
public class IncreaseRequest extends VariationRequest {

    /**
     * Default constructor.
     */
    public IncreaseRequest() {
        super(null);
    }

    /**
     * Constructor with a period.
     *
     * @param period {@link LocalDate}.
     */
    public IncreaseRequest(LocalDate period) {
        super(period);
    }

    /**
     * {@inheritDoc}
     */
    public IncreaseRequest(LocalDate period, String eventId, Integer startDtu, Integer endDtu, BigInteger power,
            ConsumptionProductionType consumptionProductionType) {
        super(period, eventId, startDtu, endDtu, power, consumptionProductionType);
    }
}
