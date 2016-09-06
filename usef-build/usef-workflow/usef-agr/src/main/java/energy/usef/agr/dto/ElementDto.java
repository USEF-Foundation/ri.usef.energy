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

import java.util.ArrayList;
import java.util.List;

import energy.usef.agr.model.Element;

/**
 * An ElementDto contains all Element related to {@link Element}.
 */
public class ElementDto {

    private String id;
    private String connectionEntityAddress;
    private ElementTypeDto elementType;
    private String profile;
    private Integer dtuDuration;
    private List<ElementDtuDataDto> elementDtuData;

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

    public ElementTypeDto getElementType() {
        return elementType;
    }

    public void setElementType(ElementTypeDto elementType) {
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

    public List<ElementDtuDataDto> getElementDtuData() {
        if (elementDtuData == null) {
            elementDtuData = new ArrayList<>();
        }
        return elementDtuData;
    }

    @Override
    public String toString() {
        return "ElementDto" + "[" +
                "id='" + id + "'" +
                ", connectionEntityAddress='" + connectionEntityAddress + "'" +
                ", elementType=" + elementType +
                ", profile='" + profile + "'" +
                ", dtuDuration=" + dtuDuration +
                "]";
    }
}
