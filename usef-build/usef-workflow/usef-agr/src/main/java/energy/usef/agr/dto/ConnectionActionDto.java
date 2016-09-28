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

/**
 * A data transfer object for relevant Participant data.
 */
public class ConnectionActionDto {
    private String method;
    private String entityAddress;
    private boolean isCustomer;

    public ConnectionActionDto() {
        // Default constructor required for json deserialisation.
    }

    public ConnectionActionDto(String method, String entityAddress, boolean isCustomer) {
        this.method = method;
        this.entityAddress = entityAddress;
        this.isCustomer = isCustomer;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public boolean isCustomer() {
        return isCustomer;
    }

    public void setIsCustomer(boolean isCustomer) {
        this.isCustomer = isCustomer;
    }
}
