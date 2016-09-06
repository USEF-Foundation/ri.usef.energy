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

import energy.usef.core.workflow.transformer.DispositionTransformer;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO Transformer related to the {@link GridSafetyAnalysisDto}.
 */
public class GridSafetyAnalysisDtoTransformer {
    private GridSafetyAnalysisDtoTransformer() {
        //private constructor
    }

    /**
     * Transform a List of {@link GridSafetyAnalysis} to a {@link GridSafetyAnalysisDto}.
     *
     * @param gridSafetyAnalysisList
     * @return
     */
    public static GridSafetyAnalysisDto transform(List<GridSafetyAnalysis> gridSafetyAnalysisList) {
        if (gridSafetyAnalysisList == null || gridSafetyAnalysisList.isEmpty()) {
            return null;
        }
        GridSafetyAnalysis firstGridSafetyAnalysis = gridSafetyAnalysisList.get(0);
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(firstGridSafetyAnalysis.getConnectionGroup().getUsefIdentifier());
        gridSafetyAnalysisDto.setPtuDate(firstGridSafetyAnalysis.getPtuContainer().getPtuDate());

        gridSafetyAnalysisDto.setPtus(gridSafetyAnalysisList.stream()
                .map(GridSafetyAnalysisDtoTransformer::transform)
                .collect(Collectors.toList()));

        return gridSafetyAnalysisDto;
    }

    /**
     * Transforms a {@link GridSafetyAnalysis} to a {@link GridSafetyAnalysisDto}.
     *
     * @param gridSafetyAnalysis
     * @return
     */
    public static PtuGridSafetyAnalysisDto transform(GridSafetyAnalysis gridSafetyAnalysis) {
        if (gridSafetyAnalysis == null) {
            return null;
        }
        PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
        ptuGridSafetyAnalysisDto.setDisposition(DispositionTransformer.transform(gridSafetyAnalysis.getDisposition()));
        ptuGridSafetyAnalysisDto.setPower(gridSafetyAnalysis.getPower());
        ptuGridSafetyAnalysisDto.setPtuIndex(gridSafetyAnalysis.getPtuContainer().getPtuIndex());
        return ptuGridSafetyAnalysisDto;
    }
}
