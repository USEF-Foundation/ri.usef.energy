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

package energy.usef.dso.workflow.transformer;

import energy.usef.dso.model.PtuGridMonitor;
import energy.usef.dso.workflow.dto.GridMonitoringDto;
import energy.usef.dso.workflow.dto.PtuGridMonitoringDto;

import java.math.BigInteger;
import java.util.List;

/**
 * Transformer class for {@link PtuGridMonitor} entities.
 */
public class GridMonitoringTransformer {

    private GridMonitoringTransformer() {
        // prevent instantiation
    }

    /**
     * Transforms a list of {@link PtuGridMonitor} to a {@link GridMonitoringDto} object.
     *
     * @param ptuGridMonitors {@link List} of {@link PtuGridMonitor}.
     * @return a {@link GridMonitoringDto}.
     */
    public static GridMonitoringDto transform(List<PtuGridMonitor> ptuGridMonitors) {
        if (ptuGridMonitors == null || ptuGridMonitors.isEmpty()) {
            return null;
        }
        PtuGridMonitor firstPtuGridMonitor = ptuGridMonitors.get(0);
        GridMonitoringDto gridMonitoringDto = new GridMonitoringDto(firstPtuGridMonitor.getConnectionGroup().getUsefIdentifier(),
                firstPtuGridMonitor.getPtuContainer().getPtuDate());
        ptuGridMonitors.sort((ptu1, ptu2) -> ptu1.getPtuContainer().getPtuIndex() < ptu2.getPtuContainer().getPtuIndex() ? -1 : 1);
        ptuGridMonitors.stream().map(ptuGridMonitor -> {
            PtuGridMonitoringDto ptuGridMonitoringDto = new PtuGridMonitoringDto();
            ptuGridMonitoringDto.setPtuIndex(ptuGridMonitor.getPtuContainer().getPtuIndex());
            ptuGridMonitoringDto.setActualPower(BigInteger.valueOf(ptuGridMonitor.getActualPower()));
            return ptuGridMonitoringDto;
        }).forEach(ptuGridMonitoringDto -> gridMonitoringDto.getPtuGridMonitoringDtos().add(ptuGridMonitoringDto));
        return gridMonitoringDto;
    }
}
