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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity class representing an ADS Device Message.
 */
@Entity
@Table(name = "DEVICE_MESSAGE")
public class DeviceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private DeviceMessageStatus deviceMessageStatus;

    @Column(name = "ENDPOINT", nullable = false)
    private String endpoint;

    @ManyToOne
    @JoinColumn(name = "UDI_ID", foreignKey = @ForeignKey(name = "DM_UDI_FK"), nullable = false)
    private Udi udi;

    @OneToMany(mappedBy = "deviceMessage", targetEntity = DeviceRequest.class, fetch = FetchType.EAGER)
    private List<DeviceRequest> deviceRequests;

    public List<DeviceRequest> getDeviceRequests() {
        if (deviceRequests == null) {
            deviceRequests = new ArrayList<>();
        }
        return deviceRequests;
    }

    public Long getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Udi getUdi() {
        return udi;
    }

    public void setUdi(Udi udi) {
        this.udi = udi;
    }

    public DeviceMessageStatus getDeviceMessageStatus() {
        return deviceMessageStatus;
    }

    public void setDeviceMessageStatus(DeviceMessageStatus deviceMessageStatus) {
        this.deviceMessageStatus = deviceMessageStatus;
    }

    @Override
    public String toString() {
        return "DeviceMessage" + "[" +
                "id=" + id +
                ", deviceMessageStatus=" + deviceMessageStatus +
                "]";
    }
}
