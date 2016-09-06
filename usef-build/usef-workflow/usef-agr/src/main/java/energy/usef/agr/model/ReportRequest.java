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

import energy.usef.agr.util.ListUtil;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.joda.time.LocalDate;

/**
 * Class representing a Report Request for a device. This class inherits from {@link DeviceRequest}.
 */
@Entity
@Table(name = "REPORT_REQUEST")
public class ReportRequest extends DeviceRequest {

    @Column(name = "DTU_INDEXES", scale = 0, precision = 18, nullable = true)
    private BigInteger dtuIndexes;

    /**
     * Default constructor.
     */
    public ReportRequest() {
        super(null);
    }

    /**
     * Creates a new {@link ReportRequest} with the given period.
     *
     * @param period {@link LocalDate} period.
     */
    public ReportRequest(LocalDate period) {
        super(period);
    }

    public List<Integer> getDtuIndexes() {
        return ListUtil.bigIntegerToList(dtuIndexes);
    }

    public void setDtuIndexes(List<Integer> dtuIndexes) {
        this.dtuIndexes = ListUtil.listToBigInteger(dtuIndexes);
    }
}
