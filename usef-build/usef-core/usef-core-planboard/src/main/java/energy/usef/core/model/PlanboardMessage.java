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

import energy.usef.core.util.DateTimeUtil;

import java.util.Date;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Entity class {@link PlanboardMessage}: This class is a representation of a PlanboardMessage.
 *
 * PlanboardMessage is unique for the combination: sequence, documentType and participantDomain.
 * Except for the type: FLEX_ORDER_SETTLEMENT
 *
 */
@Entity
@Table(name = "PLAN_BOARD_MESSAGE")
public class PlanboardMessage {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "CREATION_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDateTime;

    /**
     * The sequence number of this message. So, when a flex request is sent, it contains a sequence number which is set in this
     * field.
     */
    @Column(name = "SEQUENCE_NUMBER", nullable = false)
    private Long sequence;

    /**
     * The sequence number of the origin message. So, when a flex offer is send, it refers to a flex request. The sequence number of
     * the flex request is set in the origin sequence number. In case of a flex request the prognosis sequence is the origin
     * sequence number.
     */
    @Column(name = "ORIGIN_SEQUENCE_NUMBER", nullable = true)
    private Long originSequence;

    @Column(name = "PARTICIPANT_DOMAIN", nullable = false)
    private String participantDomain;

    @Column(name = "DOCUMENT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;

    @ManyToOne
    @JoinColumn(name = "CONNECTION_GROUP_ID", foreignKey = @ForeignKey(name = "PBM_CNG_FK"), nullable = true)
    private ConnectionGroup connectionGroup;

    @Column(name = "DOCUMENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "PTU_DATE")
    @Temporal(TemporalType.DATE)
    private Date period;

    @Column(name = "EXPIRATION_DATE")
    private Date expirationDate;

    @ManyToOne
    @JoinColumn(name = "MESSAGE_ID", foreignKey = @ForeignKey(name = "PBM_MSG_FK"), nullable = true)
    private Message message;

    public PlanboardMessage() {
        this.creationDateTime = DateTimeUtil.getCurrentDateTime().toDateTime().toDate();
    }

    /**
     * The constructor to initiate a planboardMessage with all its 'required' fields.
     *
     * @param documentType document type ({@link DocumentType})
     * @param sequence the sequence number
     * @param initialStatus the initial status of the planboard message ({@link DocumentStatus})
     * @param participantDomain the participant domain name
     * @param period the period ({@link LocalDate})
     * @param originSequence the original sequence number
     * @param connectionGroup This field is nullable in the database but required if possible.
     * @param expirationDate This field is nullable in the database but required if possible.
     */
    public PlanboardMessage(DocumentType documentType, Long sequence, DocumentStatus initialStatus,
            String participantDomain, LocalDate period, Long originSequence, ConnectionGroup connectionGroup,
            LocalDateTime expirationDate) {
        this();
        this.sequence = sequence;
        this.documentStatus = initialStatus;
        this.documentType = documentType;
        this.participantDomain = participantDomain;
        setPeriod(period);
        this.originSequence = originSequence;
        this.connectionGroup = connectionGroup;
        this.setExpirationDate(expirationDate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Long getOriginSequence() {
        return originSequence;
    }

    public void setOriginSequence(Long originSequence) {
        this.originSequence = originSequence;
    }

    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentStatus status) {
        this.documentStatus = status;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public LocalDateTime getExpirationDate() {
        if (expirationDate == null) {
            return null;
        }
        return LocalDateTime.fromDateFields(expirationDate);
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        if (expirationDate == null) {
            this.expirationDate = null;
        } else {
            this.expirationDate = expirationDate.toDateTime().toDate();
        }
    }

    public LocalDate getPeriod() {
        if (period == null) {
            return null;
        }
        return LocalDate.fromDateFields(period);
    }

    public void setPeriod(LocalDate period) {
        if (period == null) {
            this.period = null;
        } else {
            this.period = period.toDateMidnight().toDate();
        }
    }

    public String getParticipantDomain() {
        return participantDomain;
    }

    public void setParticipantDomain(String participantDomain) {
        this.participantDomain = participantDomain;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public ConnectionGroup getConnectionGroup() {
        return connectionGroup;
    }

    public void setConnectionGroup(ConnectionGroup connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

    public LocalDateTime getCreationDateTime() {
        if (creationDateTime == null) {
            return null;
        }
        return LocalDateTime.fromDateFields(creationDateTime);
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        if (creationDateTime == null) {
            this.creationDateTime = null;
        } else {
            this.creationDateTime = creationDateTime.toDateTime().toDate();
        }
    }

    @Override
    public String toString() {
        return "PlanboardMessage" + "[" +
                "id=" + id +
                ", creationDateTime=" + creationDateTime +
                ", sequence=" + sequence +
                ", originSequence=" + originSequence +
                ", participantDomain='" + participantDomain + "'" +
                ", documentStatus=" + documentStatus +
                ", documentType=" + documentType +
                ", period=" + period +
                ", expirationDate=" + expirationDate +
                "]";
    }
}
