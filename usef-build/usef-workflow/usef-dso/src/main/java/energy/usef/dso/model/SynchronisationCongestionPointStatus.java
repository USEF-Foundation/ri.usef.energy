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

package energy.usef.dso.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity for the SYNCHRONISATION_CONNECTION_STATUS Table.
 */
@Entity
@Table(name = "SYNCHRONISATION_CONGESTION_POINT_STATUS", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "COMMON_REFERENCE_OPERATOR_ID", "SYNCHRONISATION_CONGESTION_POINT_ID" }) })
public class SynchronisationCongestionPointStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "COMMON_REFERENCE_OPERATOR_ID", foreignKey = @ForeignKey(name = "SCS_CRO_FK"), referencedColumnName = "ID", nullable = false)
    private CommonReferenceOperator commonReferenceOperator;

    @ManyToOne
    @JoinColumn(name = "SYNCHRONISATION_CONGESTION_POINT_ID", foreignKey = @ForeignKey(name = "SCS_SCP_FK"), referencedColumnName = "ID", nullable = false)
    private SynchronisationCongestionPoint synchronisationCongestionPoint;

    @Column(name = "SYNCHRONISATION_CONGESTION_POINT_STATUS")
    @Enumerated(EnumType.STRING)
    private SynchronisationConnectionStatusType status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SynchronisationConnectionStatusType getStatus() {
        return status;
    }

    public void setStatus(SynchronisationConnectionStatusType status) {
        this.status = status;
    }

    public CommonReferenceOperator getCommonReferenceOperator() {
        return commonReferenceOperator;
    }

    public void setCommonReferenceOperator(CommonReferenceOperator commonReferenceOperator) {
        this.commonReferenceOperator = commonReferenceOperator;
    }

    public SynchronisationCongestionPoint getSynchronisationCongestionPoint() {
        return synchronisationCongestionPoint;
    }

    public void setSynchronisationCongestionPoint(SynchronisationCongestionPoint synchronisationCongestionPoint) {
        this.synchronisationCongestionPoint = synchronisationCongestionPoint;
    }

    @Override
    public String toString() {
        return "SynchronisationCongestionPointStatus" + "[" +
                "id=" + id +
                ", status=" + status +
                "]";
    }
}
