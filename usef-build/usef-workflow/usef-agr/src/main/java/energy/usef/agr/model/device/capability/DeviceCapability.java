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

package energy.usef.agr.model.device.capability;

import energy.usef.agr.model.UdiEvent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Abstract entity class representing the capability of a UDI device.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class DeviceCapability {

    @Id
    @Column(name = "ID", nullable = false, length = 40)
    private String id;

    @ManyToOne
    @JoinColumn(name = "UDI_EVENT_ID", nullable = false)
    private UdiEvent udiEvent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UdiEvent getUdiEvent() {
        return udiEvent;
    }

    public void setUdiEvent(UdiEvent udiEvent) {
        this.udiEvent = udiEvent;
    }
}
