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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Entity class {@link PtuState}: contains the regime and PTU container state data for a PTU container and a connection group.
 * 
 */
@Entity
@Table(name = "PTU_STATE")
public class PtuState extends Document {
    @Column(name = "REGIME", nullable = false)
    @Enumerated(EnumType.STRING)
    private RegimeType regime;

    @Column(name = "STATE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PtuContainerState state;

    /**
     * Default constructor.
     */
    public PtuState() {
        // do nothing.
    }

    /**
     * Constructor.
     * 
     * @param regime regime
     * @param state state
     */
    public PtuState(RegimeType regime, PtuContainerState state) {
        this.regime = regime;
        this.state = state;
    }

    public RegimeType getRegime() {
        return regime;
    }

    public void setRegime(RegimeType regime) {
        this.regime = regime;
    }

    public PtuContainerState getState() {
        return state;
    }

    public void setState(PtuContainerState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "PtuState" + "[" +
                "regime=" + regime +
                ", state=" + state +
                "]";
    }
}
