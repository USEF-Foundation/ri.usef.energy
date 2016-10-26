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

package nl.energieprojecthoogdalem.forecastservice.element;

public final class ElementType
{
    public static final String PV = "PV"
                            ,NOD = "NOD"
                            ,ZIH = "ZIH"
                            ,BWZ = "BWZ"
                            ,BATTERY = "BATTERY"
                            ,BATTERY_ID = BATTERY +1
                            ,BATTERY_NOD = BATTERY + '_' + NOD
                            ,BATTERY_NOD_ID = BATTERY_NOD +1
                            ,BATTERY_ZIH = BATTERY + '_' + ZIH
                            ,BATTERY_ZIH_ID = BATTERY_ZIH +1
                            ,BATTERY_BWZ = BATTERY + '_' + BWZ
                            ,BATTERY_BWZ_ID = BATTERY_BWZ +1
                            ,HOME = "HOME"
                            ,HOME_ID = HOME +1
                        ;
}
