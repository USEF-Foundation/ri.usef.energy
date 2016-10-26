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

/**
 * utility to convert between Hoogdalem home ID and usef EAN
 * */
public final class EANUtil {
    public static String EAN_PREFIX = "ean.800000000000000";

    /**
     * returns ean from number, valid range 0 &#60;-&#62; 999
     *
     * @param home home number in integer format
     * @return  EAN string of home id
     * */
    public static String toEAN(int home)    {   return EAN_PREFIX + String.format("%03d", home);}

    /**
     * returns ean from string, valid range "000" &#60;-&#62; "999"
     *
     * @param home home number in string format
     * @return  EAN string of home id
     * */
    public static String toEAN(String home)
    {
        return EAN_PREFIX + home;
    }

    /**
     * returns home id from ean string
     *
     * @param EAN EAN string of home id
     * @return home number in integer format
     * */
    public static int toHomeInt(String EAN)  {   return Integer.parseInt(EAN.substring(EAN.length() -3));}

    /**
     * returns home id from ean string
     *
     * @param EAN EAN string of home id
     * @return home number in string format
     * */
    public static String toHomeString(String EAN)  {   return EAN.substring(EAN.length() -3);}
}
