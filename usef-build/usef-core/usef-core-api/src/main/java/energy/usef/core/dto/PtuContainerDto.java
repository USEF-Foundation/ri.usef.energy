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

package energy.usef.core.dto;

import org.joda.time.LocalDate;

/**
 * Data Transfer Object conveying the PTU container.
 */
public class PtuContainerDto {
    private LocalDate ptuDate;
    private int ptuIndex;

    /**
     * Default constructor.
     *
     * @param ptuDate {@link LocalDate} The Date of this ptu.
     * @param ptuIndex {@link Integer} The index of this ptu.
     */
    public PtuContainerDto(LocalDate ptuDate, Integer ptuIndex) {
        this.ptuDate = ptuDate;
        this.ptuIndex = ptuIndex;
    }

    public LocalDate getPtuDate() {
        return ptuDate;
    }

    public void setPtuDate(LocalDate ptuDate) {
        this.ptuDate = ptuDate;
    }

    public int getPtuIndex() {
        return ptuIndex;
    }

    public void setPtuIndex(int ptuIndex) {
        this.ptuIndex = ptuIndex;
    }

    @Override
    public String toString() {
        return "PtuContainerDto{" +
                "ptuDate=" + ptuDate +
                ", ptuIndex=" + ptuIndex +
                '}';
    }
}
