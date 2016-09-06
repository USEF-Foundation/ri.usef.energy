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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity class {@link Document}: This class is a base class for documents inside a {@link PtuContainer}.
 *
 */
@Entity
@Table(name = "DOCUMENT")
@Inheritance(strategy = InheritanceType.JOINED)
public class Document {
    @Id
    @Column(name = "ID", nullable = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PTU_CONTAINER_ID", foreignKey = @ForeignKey(name = "DOC_PCN_FK"), nullable = false)
    private PtuContainer ptuContainer;

    @Column(name = "SEQUENCE_NUMBER", nullable = false)
    private Long sequence;

    @ManyToOne
    @JoinColumn(name = "CONNECTION_GROUP_ID", foreignKey = @ForeignKey(name = "DOC_CONNECTION_GROUP_FK"), nullable = false)
    private ConnectionGroup connectionGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PtuContainer getPtuContainer() {
        return ptuContainer;
    }

    public void setPtuContainer(PtuContainer ptuContainer) {
        this.ptuContainer = ptuContainer;
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

    @Override
    public String toString() {
        return "Document" + "[" +
                "id=" + id +
                ", sequence=" + sequence +
                "]";
    }
}
