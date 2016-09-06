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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Base entity for the Element Data store. This stores the portfolio elements associated with AGR connections.
 */
@Entity
@Table(name = "ELEMENT")
public class Element {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "CONNECTION_ENTITY_ADDRESS", nullable = false)
    private String connectionEntityAddress;

    @Column(name = "ELEMENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ElementType elementType;

    @Column(name = "PROFILE", nullable = false)
    private String profile;

    @Column(name = "DTU_DURATION", nullable = false)
    private Integer dtuDuration;

    @OneToMany(mappedBy = "element",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ElementDtuData> elementDtuData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectionEntityAddress() {
        return connectionEntityAddress;
    }

    public void setConnectionEntityAddress(String connectionEntityAddress) {
        this.connectionEntityAddress = connectionEntityAddress;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Integer getDtuDuration() {
        return dtuDuration;
    }

    public void setDtuDuration(Integer dtuDuration) {
        this.dtuDuration = dtuDuration;
    }

    public List<ElementDtuData> getElementDtuData() {
        if (elementDtuData == null) {
            elementDtuData = new ArrayList<>();
        }
        return elementDtuData;
    }

    public void setElementDtuData(List<ElementDtuData> elementDtuData) {
        this.elementDtuData = elementDtuData;
    }
}
