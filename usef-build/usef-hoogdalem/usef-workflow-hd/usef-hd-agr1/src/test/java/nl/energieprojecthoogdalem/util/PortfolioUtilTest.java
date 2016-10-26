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

package nl.energieprojecthoogdalem.util;

import info.usef.agr.dto.ConnectionPortfolioDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.usef.agr.dto.UdiPortfolioDto;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import org.junit.Test;

import static org.junit.Assert.*;

public class PortfolioUtilTest
{
    private static final int CONNECTION_COUNT = 24;

    @Test
    public void testPortfolioUtil() throws Exception
    {
        List<ConnectionPortfolioDto> allConnections = buildConnectionPortfolioDtos();
        List<ConnectionPortfolioDto> zihConnections = new ArrayList<>();
        List<ConnectionPortfolioDto> nodConnections = new ArrayList<>();

        PortfolioUtil.splitZIHNOD(allConnections, zihConnections, nodConnections);

        assertEquals(CONNECTION_COUNT - CONNECTION_COUNT/2, zihConnections.size());
        assertEquals(CONNECTION_COUNT/2, nodConnections.size());

        PortfolioUtil.joinZIHNOD(allConnections, zihConnections, nodConnections);

        assertEquals(CONNECTION_COUNT, allConnections.size());
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtos()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT)
                .mapToObj(index ->
                {
                    ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EANUtil.EAN_PREFIX + index);
                    connection.getUdis().add(buildUdis(index));
                    return connection;
                })
                .collect(Collectors.toList());
    }

    private UdiPortfolioDto buildUdis(int idx)
    {
        if(idx <= CONNECTION_COUNT/2)
            return new UdiPortfolioDto("", 96, ElementType.BATTERY_NOD);

        else
           return new UdiPortfolioDto("", 96, ElementType.BATTERY_ZIH);
    }
}