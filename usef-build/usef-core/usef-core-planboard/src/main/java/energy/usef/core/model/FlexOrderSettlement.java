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

package energy.usef.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Entity carrying information about the settlement of one flex order.
 */
@Entity
@Table(name = "FLEX_ORDER_SETTLEMENT")
public class FlexOrderSettlement {

    @Id
    @Column(name = "ID", unique = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "PERIOD", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date period;
    @Column(name = "SEQUENCE_NUMBER", nullable = false)
    private Long sequence;
    @OneToMany(mappedBy = "flexOrderSettlement")
    private List<PtuSettlement> ptuSettlements;
    @ManyToOne
    @JoinColumn(name = "CONNECTION_GROUP_ID", nullable = false, foreignKey = @ForeignKey(name = "FOS_CONNECTION_GROUP_FK"))
    private ConnectionGroup connectionGroup;
    @OneToOne
    @JoinColumn(name = "FLEX_ORDER_MESSAGE_ID", nullable = false, foreignKey = @ForeignKey(name = "FOS_PM_FLEX_ORDER_FK"))
    private PlanboardMessage flexOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriod() {
        if (period == null) {
            return null;
        }
        return new LocalDate(period);
    }

    public void setPeriod(LocalDate period) {
        if (period == null) {
            this.period = null;
        } else {
            this.period = period.toDateMidnight().toDate();
        }
    }

    public List<PtuSettlement> getPtuSettlements() {
        if (ptuSettlements == null) {
            ptuSettlements = new ArrayList<>();
        }
        return ptuSettlements;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public ConnectionGroup getConnectionGroup() {
        return connectionGroup;
    }

    public void setConnectionGroup(ConnectionGroup connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

    public PlanboardMessage getFlexOrder() {
        return flexOrder;
    }

    public void setFlexOrder(PlanboardMessage flexOrder) {
        this.flexOrder = flexOrder;
    }

}
