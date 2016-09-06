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

package energy.usef.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.joda.time.Hours;

/**
 * Util class to do some transformations between power and energy.
 */
public class PowerUtil {

    private static final int MEGA_WATT_FACTOR = 1000000;
    private static final int DECIMAL_PRECISION = 5;

    private PowerUtil() {
    }

    /**
     * Transforms a power (Watt) of a PTU to an energy (Watt hour).
     *
     * @param power       {@link BigInteger} power expressed in Watts
     * @param ptuDuration PTU duration
     * @return the energy expressed in Watt hours
     */
    public static BigInteger powerToEnergy(BigInteger power, Integer ptuDuration) {
        BigDecimal hoursPerPtu = new BigDecimal(Hours.ONE.toStandardMinutes().getMinutes())
                .divide(new BigDecimal(ptuDuration), DECIMAL_PRECISION, RoundingMode.HALF_UP);
        return new BigDecimal(power).divide(hoursPerPtu, DECIMAL_PRECISION, RoundingMode.HALF_UP).toBigInteger();
    }

    /**
     * Transforms price for an amount of power (Watt) per ptu into price of one (Mega Watt hour) based on ptu duration.
     *
     * @param power       (@link BigInteger) power expressed in Watts
     * @param price       (@link BigInteger) price for the power
     * @param ptuDuration PTU duration (in minutes)
     * @return the price of one Mega Watt hours (MWh)
     */
    public static BigDecimal wattPricePerPTUToMWhPrice(BigInteger power, BigDecimal price, Integer ptuDuration) {
        BigDecimal ptusPerHour = new BigDecimal(Hours.ONE.toStandardMinutes().getMinutes())
                .divide(new BigDecimal(ptuDuration), DECIMAL_PRECISION, RoundingMode.HALF_UP);
        if (power.compareTo(BigInteger.ZERO) == 0) {
            return BigDecimal.ZERO;
        } else {
            return price.multiply(new BigDecimal(MEGA_WATT_FACTOR)).multiply(ptusPerHour)
                    .divide(new BigDecimal(power.abs()), DECIMAL_PRECISION, RoundingMode.HALF_UP);
        }
    }

    /**
     * Converts the price of a MWh to the price for a Watt during for a PTU.
     *
     * @param price {@link BigDecimal} price for 1 MWh.
     * @param ptuDuration {@link Integer} duration of a PTU in minutes.
     * @return the price for 1 W during a PTU.
     */
    public static BigDecimal megaWattHourPriceToWattPricePerPtu(BigDecimal price, Integer ptuDuration) {
        BigDecimal ptusPerHour = new BigDecimal(Hours.ONE.toStandardMinutes().getMinutes())
                .divide(new BigDecimal(ptuDuration), DECIMAL_PRECISION, RoundingMode.HALF_UP);
        return price.divide(BigDecimal.valueOf(MEGA_WATT_FACTOR).multiply(ptusPerHour), DECIMAL_PRECISION, RoundingMode.HALF_UP);
    }
}
