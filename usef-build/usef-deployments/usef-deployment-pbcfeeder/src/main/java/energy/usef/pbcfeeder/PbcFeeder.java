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

package energy.usef.pbcfeeder;

import energy.usef.pbcfeeder.dto.PbcPtuContainerDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PBCFeeder class reads excel file with step data for PBCs.
 */
@Singleton
public class PbcFeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeeder.class);
    private static final int DAYS_IN_SPREADSHEET = 7;
    private static final String DATA_SHEET = "Data";
    private static final String CPLIMITS_SHEET = "CPLimits";
    private static final String LOWER_LIMIT = "LowerLimit";
    private static final String UPPER_LIMIT = "UpperLimit";
    private static final String CONGESTION_POINT_PREFIX = "CongestionPoint";

    private Map<String, List<Double>> stubColInputMap;
    private Map<String, BigDecimal> congestionPointUpperLimitMap;
    private Map<String, BigDecimal> congestionPointLowerLimitMap;
    private List<PbcStubDataDto> stubRowInputList;

    private static final int PTU_PER_DAY = 96;

    /**
     * This method reads the excel file based on the location given in the config file. The method started when the during
     * deployment and fills the field stubColInputMap and stubRowInputList;
     */
    public void readFile(Path pathToExcelSheet) {
        LOGGER.debug("Looking for excel sheet filename: {}", pathToExcelSheet);
        try (InputStream pbcInput = new FileInputStream(pathToExcelSheet.toFile())) {
            HSSFWorkbook pbcWorkbook = new HSSFWorkbook(pbcInput);
            HSSFSheet pbcDataSheet = pbcWorkbook.getSheet(DATA_SHEET);
            HSSFSheet pbcCongestionPointLimitsSheet = pbcWorkbook.getSheet(CPLIMITS_SHEET);
            fillColStubInputMap(pbcDataSheet);
            fillCongestionPointLimitsMaps(pbcCongestionPointLimitsSheet);
            fillStubRowInputList(pbcDataSheet);
            pbcWorkbook.close();
            LOGGER.debug("Created step data based on excel sheet.");
        } catch (IOException e) {
            LOGGER.error("Caught exception while parsing the PBC feeder data sheet.", e);
        }
    }

    /**
     * Return UncontrolledLoad control for a certain congestion point from map.
     *
     * @param cpIndex to pick a congestionPoint (1-3), when passing >3 the avg of the three is returned.
     */
    public List<Double> getUncontrolledLoadForCongestionPoint(int cpIndex) {
        String colName;
        switch (cpIndex) {
        case 1:
            colName = "CongestionPointOne";
            break;
        case 2:
            colName = "CongestionPointTwo";
            break;
        case 3:
            colName = "CongestionPointThree";
            break;
        default:
            colName = "CongestionPointAvg";
            break;
        }
        return stubColInputMap.get(colName);
    }

    /**
     * Method to return PVForecast column from map.
     *
     * @return
     */
    public List<Double> getPvForecast() {
        return stubColInputMap.get("PVLoadForecast");
    }

    /**
     * Method to return PVActual column from map.
     *
     * @return
     */
    public List<Double> getPvActual() {
        return stubColInputMap.get("PVLoad");
    }

    /**
     * Method to return APX column from map.
     *
     * @return
     */
    public List<Double> getApx() {
        return stubColInputMap.get("APX");
    }

    /**
     * Method to return PtuStartTime column from map.
     *
     * @return
     */
    public List<Double> getPtuStartTime() {
        return stubColInputMap.get("PTU");
    }

    /**
     * Gets the power limits for a congestion point (first element of the list will be the LOWER_LIMIT, second element of the list
     * will be the UPPER_LIMIT).
     *
     * @param congestionPointId {@link Integer} id of the congestion point (1,2 or 3).
     * @return a {@link List} of {@link BigDecimal}.
     */
    public List<BigDecimal> getCongestionPointPowerLimits(Integer congestionPointId) {
        if (!congestionPointUpperLimitMap.containsKey(CONGESTION_POINT_PREFIX + congestionPointId) ||
                !congestionPointLowerLimitMap.containsKey(CONGESTION_POINT_PREFIX + congestionPointId)) {
            return new ArrayList<>();
        }
        LOGGER.debug("Get power limits for congestion point with id [{}]: Lower=[{}], Upper=[{}].", congestionPointId,
                congestionPointLowerLimitMap.get(CONGESTION_POINT_PREFIX + congestionPointId),
                congestionPointUpperLimitMap.get(CONGESTION_POINT_PREFIX + congestionPointId));
        return Arrays.asList(
                congestionPointLowerLimitMap.get(CONGESTION_POINT_PREFIX + congestionPointId),
                congestionPointUpperLimitMap.get(CONGESTION_POINT_PREFIX + congestionPointId));
    }

    /**
     * @param date
     * @param startPtuIndex of the list starting from 1 till 96 (PTU_PER_DAY).
     * @param amount
     * @return
     */
    public List<PbcStubDataDto> getStubRowInputList(LocalDate date, int startPtuIndex, int amount) {
        if (startPtuIndex > PTU_PER_DAY) {
            date = date.plusDays((int) Math.floor(startPtuIndex / PTU_PER_DAY));
            startPtuIndex = startPtuIndex % PTU_PER_DAY;
        }

        // Match PTU-index with requested startIndex and date from ExcelSheet.
        LocalDate epochDate = new LocalDate("1970-01-01");
        int daysDif = Days.daysBetween(epochDate, date).getDays();
        int ptuOffset = (daysDif % DAYS_IN_SPREADSHEET) * PTU_PER_DAY + startPtuIndex - 1;
        List<PbcStubDataDto> pbcStubDataDtoList = new ArrayList<>();

        // Loop over stubRowInputList, if necessary, to get requested amount of ptus.
        do {
            int toIndex = 0;
            if (ptuOffset + amount > stubRowInputList.size()) {
                toIndex = stubRowInputList.size();
            } else {
                toIndex = ptuOffset + amount;
            }
            amount -= (toIndex - ptuOffset);

            pbcStubDataDtoList.addAll(stubRowInputList.subList(ptuOffset, toIndex));
            ptuOffset = 0;
        } while (amount > 0);

        // Create and set PtuContainer for pbcStubDataDto.

        int lastPtuIndex = 0;
        for (PbcStubDataDto pbcStubDataDto : pbcStubDataDtoList) {
            int ptuIndex = pbcStubDataDto.getIndex() % PTU_PER_DAY;
            if (ptuIndex == 0) {
                ptuIndex = PTU_PER_DAY;
            }
            if (ptuIndex < lastPtuIndex) {
                date = date.plusDays(1);
            }
            PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), ptuIndex);
            pbcStubDataDto.setPtuContainer(ptuContainerDto);
            lastPtuIndex = ptuIndex;
        }
        return pbcStubDataDtoList;
    }

    private void fillColStubInputMap(HSSFSheet pbcDataSheet) {
        stubColInputMap = new HashMap<>();
        for (Cell topRowCell : pbcDataSheet.getRow(0)) {
            if (topRowCell.getStringCellValue().equals("")) {
                break;
            }
            String columnName = topRowCell.getStringCellValue();
            List<Double> colValues = new ArrayList<>();
            for (Row r : pbcDataSheet) {
                if (r.getRowNum() == 0) {
                    continue;
                }
                Cell c = r.getCell(topRowCell.getColumnIndex());
                if (c != null) {
                    if (c.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        colValues.add(c.getNumericCellValue());
                    }
                }
            }
            stubColInputMap.put(columnName, colValues);
        }
    }

    private void fillStubRowInputList(HSSFSheet pbcDataSheet) {
        stubRowInputList = new ArrayList<>();
        for (Row r : pbcDataSheet) {
            if (r.getRowNum() == 0) {
                continue;
            }
            PbcStubDataDto row = new PbcStubDataDto();
            for (Cell c : r) {
                if (c != null) {
                    if (c.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        fillPbcStubDataDto(c, row);
                    }
                }
            }
            stubRowInputList.add(row);
        }
    }

    /**
     * This method fills in the map with CongestionPoint-->Lower Power Limit and the map CongestionPoint-->Upper Power Limit.
     *
     * @param pbcCongestionPointLimitsSheet the excel page with the power limits.
     */
    private void fillCongestionPointLimitsMaps(HSSFSheet pbcCongestionPointLimitsSheet) {
        congestionPointLowerLimitMap = new HashMap<>();
        congestionPointUpperLimitMap = new HashMap<>();
        for (Cell columnHeading : pbcCongestionPointLimitsSheet.getRow(0)) {
            // stop when one encounters the first empty cell.
            if (StringUtils.isBlank(columnHeading.getStringCellValue())) {
                break;
            }
            for (Row row : pbcCongestionPointLimitsSheet) {
                // skip first row.
                if (row.getRowNum() == 0) {
                    continue;
                }
                String congestionPoint = row.getCell(0).getStringCellValue();
                Cell cell = row.getCell(columnHeading.getColumnIndex());
                if (LOWER_LIMIT.equals(columnHeading.getStringCellValue())) {
                    congestionPointLowerLimitMap.put(congestionPoint, new BigDecimal(cell.getNumericCellValue()));
                }
                if (UPPER_LIMIT.equals(columnHeading.getStringCellValue())) {
                    congestionPointUpperLimitMap.put(congestionPoint, new BigDecimal(cell.getNumericCellValue()));
                }
            }
        }
    }

    private void fillPbcStubDataDto(Cell c, PbcStubDataDto row) {
        switch (c.getColumnIndex()) {
        case 0:
            row.setIndex((int) c.getNumericCellValue());
            break;
        case 1:
            row.setCongestionPointOne(c.getNumericCellValue());
            break;
        case 2:
            row.setCongestionPointTwo(c.getNumericCellValue());
            break;
        case 3:
            row.setCongestionPointThree(c.getNumericCellValue());
            break;
        case 4:
            row.setCongestionPointAvg(c.getNumericCellValue());
            break;
        case 5:
            row.setPvLoadForecast(c.getNumericCellValue());
            break;
        case 6:
            row.setPvLoadActual(c.getNumericCellValue());
            break;
        case 7:
            row.setApx(c.getNumericCellValue());
            break;
        default:
            break;
        }
    }

}
