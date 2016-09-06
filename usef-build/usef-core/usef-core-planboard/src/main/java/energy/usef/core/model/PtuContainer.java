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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.joda.time.LocalDate;

/**
 * Entity class {@link PtuContainer}: This class is a representation of a PTUContainer.
 *
 */
@Entity
@Table(name = "PTU_CONTAINER", uniqueConstraints = { @UniqueConstraint(columnNames = { "PTU_DATE", "PTU_INDEX" }) })
public class PtuContainer {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(mappedBy = "ptuContainer")
    private Set<Document> documents;

    @Column(name = "PTU_DATE", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date ptuDate;

    @Column(name = "PTU_INDEX", nullable = false)
    private Integer ptuIndex;

    @Column(name = "PHASE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PhaseType phase;

    /**
     * Default constructor.
     */
    public PtuContainer() {
        this.phase = PhaseType.Plan;
    }

    /**
     * Specific constructor for a PTU Container.
     * 
     * @param ptuDate {@link LocalDate} period of the {@link PtuContainer};
     * @param ptuIndex {@link Integer} index of the {@link PtuContainer}. Starts at 1.
     */
    public PtuContainer(LocalDate ptuDate, Integer ptuIndex) {
        setPtuDate(ptuDate);
        this.ptuIndex = ptuIndex;
        this.phase = PhaseType.Plan;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Document> getDocuments() {
        if (documents == null) {
            documents = new HashSet<>();
        }
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents;
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

    public PhaseType getPhase() {
        return phase;
    }

    public void setPhase(PhaseType phase) {
        this.phase = phase;
    }

    @Override
    public String toString() {
        return "PtuContainer" + "[" +
                "id=" + id +
                ", ptuDate=" + ptuDate +
                ", ptuIndex=" + ptuIndex +
                ", phase=" + phase +
                "]";
    }
}
