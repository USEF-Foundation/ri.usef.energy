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
package nl.energieprojecthoogdalem.agr.dtos;


/**
 * Proposition configuration of a connection
 * */
public class Proposition
{
    private String pv, battery;

    /**
     * Initialize proposition with pv and/or battery
     *
     * @param pv if value is "y" the connection contains solar panels
     * @param battery if value is "y" then the connection contains a battery
     * */
    public Proposition(String pv, String battery)
    {
        this.battery = battery;
        this.pv = pv;
    }

    /**
     * connection has pv
     * @return true if proposition for connection contains pv
     * */
    public boolean hasPv(){return "y".equals(pv);}

    /**
     * connection has battery
     * @return true if proposition for connection contains battery
     * */
    public boolean hasBattery(){return "y".equals(battery);}

}
