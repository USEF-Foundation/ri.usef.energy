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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;

/**
 * Transformer for {@link PowerContainer} Related Objects.
 */
public class PowerContainerTransformer {
    /*
     * Hide implicit public constructor.
     */
    private PowerContainerTransformer() {
    }

    /**
     * Transform the PowerContainer to a DTO.
     *
     * @param powerContainer {@link PowerContainer} powerContainer.
     * @return a {@link ConnectionGroupPortfolioDto}.
     */
    public static PowerContainerDto transform(PowerContainer powerContainer) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(powerContainer.getPeriod(), powerContainer.getTimeIndex());
        if (powerContainer.getProfile() == null) {
            powerContainerDto.setProfile(null);
        } else {
            PowerContainerTransformer.updateDTOValues(powerContainer.getProfile(), powerContainerDto.getProfile());
        }
        if (powerContainer.getForecast() == null) {
            powerContainerDto.setForecast(null);
        } else {
            PowerContainerTransformer.updateForecastDTOValues(powerContainer.getForecast(), powerContainerDto.getForecast());
        }

        if (powerContainer.getObserved() == null) {
            powerContainerDto.setObserved(null);
        } else {
            PowerContainerTransformer.updateDTOValues(powerContainer.getObserved(), powerContainerDto.getObserved());
        }

        return powerContainerDto;
    }

    /**
     * Copy the values from the {@link PowerData} to the {@link PowerDataDto}.
     *
     * @param fromPowerContainerModel
     * @param toPowerContainerDto
     */
    private static void updateDTOValues(PowerData fromPowerContainerModel, PowerDataDto toPowerContainerDto) {
        toPowerContainerDto.setUncontrolledLoad(fromPowerContainerModel.getUncontrolledLoad());
        toPowerContainerDto.setAverageConsumption(fromPowerContainerModel.getAverageConsumption());
        toPowerContainerDto.setAverageProduction(fromPowerContainerModel.getAverageProduction());
        toPowerContainerDto.setPotentialFlexConsumption(fromPowerContainerModel.getPotentialFlexConsumption());
        toPowerContainerDto.setPotentialFlexProduction(fromPowerContainerModel.getPotentialFlexProduction());
    }

    /**
     * Copy the values from the {@link ForecastPowerData} to the {@link ForecastPowerDataDto}.
     *
     * @param fromPowerContainerModel
     * @param toPowerContainerDto
     */
    private static void updateForecastDTOValues(ForecastPowerData fromPowerContainerModel,
            ForecastPowerDataDto toPowerContainerDto) {
        updateDTOValues(fromPowerContainerModel, toPowerContainerDto);
        toPowerContainerDto.setAllocatedFlexProduction(fromPowerContainerModel.getAllocatedFlexProduction());
        toPowerContainerDto.setAllocatedFlexConsumption(fromPowerContainerModel.getAllocatedFlexConsumption());
    }

    /**
     * Copy the values from the {@link PowerDataDto} to the {@link PowerData}.
     *
     * @param fromPowerContainerDTO
     * @param toPowerContainerModel
     */
    public static void updateValues(PowerDataDto fromPowerContainerDTO, PowerData toPowerContainerModel) {
        toPowerContainerModel.setUncontrolledLoad(fromPowerContainerDTO.getUncontrolledLoad());
        toPowerContainerModel.setAverageConsumption(fromPowerContainerDTO.getAverageConsumption());
        toPowerContainerModel.setAverageProduction(fromPowerContainerDTO.getAverageProduction());
        toPowerContainerModel.setPotentialFlexConsumption(fromPowerContainerDTO.getPotentialFlexConsumption());
        toPowerContainerModel.setPotentialFlexProduction(fromPowerContainerDTO.getPotentialFlexProduction());
    }

    /**
     * Copy the values from the {@link PowerDataDto} to the {@link PowerData}.
     *
     * @param fromPowerContainerDTO source {@link ForecastPowerDataDto} object.
     * @param toPowerContainerModel target {@link ForecastPowerData} object.
     */
    public static void updateForecastValues(ForecastPowerDataDto fromPowerContainerDTO, ForecastPowerData toPowerContainerModel) {
        updateValues(fromPowerContainerDTO, toPowerContainerModel);
        toPowerContainerModel.setAllocatedFlexConsumption(fromPowerContainerDTO.getAllocatedFlexConsumption());
        toPowerContainerModel.setAllocatedFlexProduction(fromPowerContainerDTO.getAllocatedFlexProduction());
    }
}
