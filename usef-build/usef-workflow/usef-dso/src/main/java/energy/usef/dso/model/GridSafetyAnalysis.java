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

package energy.usef.dso.model;

import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.Document;
import energy.usef.core.model.PtuPrognosis;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity class {@link GridSafetyAnalysis}: This class is an implementation of {@link Document} class for a GridSafetyAnalysis.
 *
 */
@Entity
@Table(name = "GRID_SAFETY_ANALYSIS")
public class GridSafetyAnalysis extends Document {
    @Column(name = "POWER", nullable = false)
    private Long power;

    @Column(name = "DISPOSITION", nullable = false)
    @Enumerated(EnumType.STRING)
    private DispositionAvailableRequested disposition;

    @JoinTable(
            name = "GRID_SAFETY_ANALYSIS_TO_PROGNOSIS",
            joinColumns = @JoinColumn(name = "GRID_SAFETY_ANALYSIS_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "PROGNOSIS_ID", referencedColumnName = "ID"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "GRID_SAFETY_ANALYSIS_ID", "PROGNOSIS_ID" }))
    @ManyToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<PtuPrognosis> prognoses;

    public Long getPower() {
        return power;
    }

    public void setPower(Long power) {
        this.power = power;
    }

    public DispositionAvailableRequested getDisposition() {
        return disposition;
    }

    public void setDisposition(DispositionAvailableRequested disposition) {
        this.disposition = disposition;
    }

    public List<PtuPrognosis> getPrognoses() {
        if (prognoses == null) {
            prognoses = new ArrayList<>();
        }
        return prognoses;
    }

    public void setPrognoses(List<PtuPrognosis> prognoses) {
        this.prognoses = prognoses;
    }

    @Override
    public String toString() {
        return "GridSafetyAnalysis" + "[" +
                "disposition=" + disposition +
                ", power=" + power +
                "]";
    }
}
