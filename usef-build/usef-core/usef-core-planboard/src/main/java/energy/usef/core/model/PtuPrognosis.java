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

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Entity class {@link PtuPrognosis}: This class is a representation of a Prognosis.
 *
 */
@Entity
@Table(name = "PTU_PROGNOSIS")
public class PtuPrognosis extends Exchange {

    @Column(name = "TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PrognosisType type;

    @Column(name = "POWER", precision=18, scale=0, nullable = false)
    private BigInteger power;

    @Column(name = "SUBSTITUTE")
    private Boolean substitute;

    public PtuPrognosis() {
        this.substitute = false;
    }

    public PrognosisType getType() {
        return type;
    }

    public void setType(PrognosisType type) {
        this.type = type;
    }

    public BigInteger getPower() {
        return power;
    }

    public void setPower(BigInteger power) {
        this.power = power;
    }

    public Boolean isSubstitute() {
        return substitute;
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    @Override
    public String toString() {
        return "PtuPrognosis" + "[" +
                "type=" + type +
                ", power=" + power +
                ", substitute=" + substitute +
                "]";
    }

}
