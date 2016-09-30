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
package energy.usef.agr.dto;

import energy.usef.agr.model.SynchronisationConnection;

/**
 * A data transfer object for relevant {@link SynchronisationConnection} data.
 */
public class SynchronisationConnectionDto {
    private Long id;
    private String entityAddress;
    private Boolean isCustomer;
    private String lastModificationTime;

    public SynchronisationConnectionDto(Long id, String entityAddress, Boolean isCustomer, String lastModificationTime) {
        this.id = id;
        this.entityAddress = entityAddress;
        this.isCustomer = isCustomer;
        this.lastModificationTime = lastModificationTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public Boolean getCustomer() {
        return isCustomer;
    }

    public void setCustomer(Boolean customer) {
        isCustomer = customer;
    }

    public String getLastModificationTime() {
        return lastModificationTime;
    }

    public void setLastModificationTime(String lastModificationTime) {
        this.lastModificationTime = lastModificationTime;
    }
}
