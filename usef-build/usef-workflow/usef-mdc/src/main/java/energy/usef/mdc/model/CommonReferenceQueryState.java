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

package energy.usef.mdc.model;

import energy.usef.core.model.Message;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Entity class representing the state of the Common Reference Query of the MDC.
 */
@Entity
@Table(name = "COMMON_REFERENCE_QUERY_STATE")
public class CommonReferenceQueryState {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "PERIOD", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date period;

    @OneToOne
    @JoinColumn(name = "MESSAGE_ID", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private CommonReferenceQueryStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriod() {
        return new LocalDate(period);
    }

    public void setPeriod(LocalDate period) {
        this.period = period == null ? null : period.toDateMidnight().toDate();
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public CommonReferenceQueryStatus getStatus() {
        return status;
    }

    public void setStatus(CommonReferenceQueryStatus status) {
        this.status = status;
    }
}
