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

package energy.usef.mdc.pbcfeederimpl;

import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;
import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;

/**
 * This class is the entry point for all data that is being 'fed' to the MDC PBCs.
 */
@Stateless
public class PbcFeederService {

    @Inject
    private PbcFeederClient pbcFeederClient;

    /**
     * For each connection given as input, this method builds partially constructed {@link ConnectionMeterDataDto} which will
     * contain {@link PtuMeterDataDto} with the uncontrolled load information for all the required ptus for each given connection.
     *
     * @param date {@link LocalDate} period.
     * @param startPtuIndex {@link Integer} index of the starting ptu.
     * @param amountOfPtus {@link Integer} amout of ptus (>= 1)
     * @param congestionPointToConnectionAddresses {@link List} of connection entity addresses.
     * @return a {@link List} of {@link ConnectionMeterDataDto}.
     */
    public List<ConnectionMeterDataDto> fetchUncontrolledLoad(LocalDate date, int startPtuIndex, int amountOfPtus,
            List<String> congestionPointToConnectionAddresses) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        return congestionPointToConnectionAddresses.stream().map(connectionEntityAddress -> {
            ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
            connectionMeterDataDto.setEntityAddress(connectionEntityAddress);
            pbcStubDataDtoList.stream().forEach(pbcStubData -> {
                PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
                ptuMeterDataDto.setStart(BigInteger.valueOf(pbcStubData.getPtuContainer().getPtuIndex()));
                BigDecimal rawUncontrolledLoad = fetchRawAverageUncontrolledLoad(pbcStubData);
                ptuMeterDataDto.setPower(BigInteger.valueOf(rawUncontrolledLoad.longValue()));
                ptuMeterDataDto.setDuration(BigInteger.ONE);
                connectionMeterDataDto.getPtuMeterDataDtos().add(ptuMeterDataDto);
            });
            return connectionMeterDataDto;
        }).collect(Collectors.toList());
    }

    private BigDecimal fetchRawAverageUncontrolledLoad(PbcStubDataDto pbcStubData) {
        return new BigDecimal(pbcStubData.getCongestionPointAvg());
    }
}
