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

import energy.usef.agr.model.device.capability.DeviceCapability;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Entity class representing a UDI Event.
 */
@Entity
@Table(name = "UDI_EVENT")
public class UdiEvent {

    @Id
    @Column(name = "ID", nullable = false, length = 36)
    private String id;

    @Column(name = "PERIOD", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date period;

    @ManyToOne
    @JoinColumn(name = "UDI_ID", foreignKey = @ForeignKey(name = "UE_UDI_FK"), nullable = false)
    private Udi udi;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "udiEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeviceCapability> deviceCapabilities;

    @Column(name = "DEVICE_SELECTOR")
    private String deviceSelector;

    @Column(name = "UDI_EVENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private UdiEventType udiEventType;

    @Column(name = "START_DTU", nullable = false)
    private Integer startDtu;

    @Column(name = "END_DTU", nullable = false)
    private Integer endDtu;

    @Column(name = "FINISH_BEFORE_DTU")
    private Integer finishBeforeDtu;

    @Column(name = "START_AFTER_DTU")
    private Integer startAfterDtu;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getPeriod() {
        if (period == null) {
            return null;
        }
        return new LocalDate(period);
    }

    public void setPeriod(LocalDate period) {
        if (period == null) {
            this.period = null;
        } else {
            this.period = period.toDateMidnight().toDate();
        }
    }

    public Udi getUdi() {
        return udi;
    }

    public void setUdi(Udi udi) {
        this.udi = udi;
    }

    public List<DeviceCapability> getDeviceCapabilities() {
        if (deviceCapabilities == null) {
            deviceCapabilities = new ArrayList<>();
        }
        return deviceCapabilities;
    }

    public String getDeviceSelector() {
        return deviceSelector;
    }

    public void setDeviceSelector(String deviceSelector) {
        this.deviceSelector = deviceSelector;
    }

    public UdiEventType getUdiEventType() {
        return udiEventType;
    }

    public void setUdiEventType(UdiEventType udiEventType) {
        this.udiEventType = udiEventType;
    }

    public Integer getStartDtu() {
        return startDtu;
    }

    public void setStartDtu(Integer startDtu) {
        this.startDtu = startDtu;
    }

    public Integer getEndDtu() {
        return endDtu;
    }

    public void setEndDtu(Integer endDtu) {
        this.endDtu = endDtu;
    }

    public Integer getFinishBeforeDtu() {
        return finishBeforeDtu;
    }

    public void setFinishBeforeDtu(Integer finishBeforeDtu) {
        this.finishBeforeDtu = finishBeforeDtu;
    }

    public Integer getStartAfterDtu() {
        return startAfterDtu;
    }

    public void setStartAfterDtu(Integer startAfterDtu) {
        this.startAfterDtu = startAfterDtu;
    }
}
