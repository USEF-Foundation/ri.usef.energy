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

package energy.usef.pbcfeeder.dto;

/**
 * Data transfer Object to pass PBCFeeder info through web service.
 */
public class PbcStubDataDto {

    private int index;
    private Double congestionPointOne;
    private Double congestionPointTwo;
    private Double congestionPointThree;
    private Double congestionPointAvg;
    private Double pvLoadForecast;
    private Double pvLoadActual;
    private Double apx;
    private PbcPtuContainerDto ptuContainer;

    public PbcStubDataDto() {
    }

    public Double getApx() {
        return apx;
    }

    public void setApx(Double apx) {
        this.apx = apx;
    }

    public Double getPvLoadActual() {
        return pvLoadActual;
    }

    public void setPvLoadActual(Double pvLoadActual) {
        this.pvLoadActual = pvLoadActual;
    }

    public Double getPvLoadForecast() {
        return pvLoadForecast;
    }

    public void setPvLoadForecast(Double pvLoadForecast) {
        this.pvLoadForecast = pvLoadForecast;
    }

    public Double getCongestionPointAvg() {
        return congestionPointAvg;
    }

    public void setCongestionPointAvg(Double congestionPointAvg) {
        this.congestionPointAvg = congestionPointAvg;
    }

    public Double getCongestionPointThree() {
        return congestionPointThree;
    }

    public void setCongestionPointThree(Double congestionPointThree) {
        this.congestionPointThree = congestionPointThree;
    }

    public Double getCongestionPointTwo() {
        return congestionPointTwo;
    }

    public void setCongestionPointTwo(Double congestionPointTwo) {
        this.congestionPointTwo = congestionPointTwo;
    }

    public Double getCongestionPointOne() {
        return congestionPointOne;
    }

    public void setCongestionPointOne(Double congestionPointOne) {
        this.congestionPointOne = congestionPointOne;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public PbcPtuContainerDto getPtuContainer() {
        return ptuContainer;
    }

    public void setPtuContainer(PbcPtuContainerDto ptuContainer) {
        this.ptuContainer = ptuContainer;
    }

    @Override
    public String toString() {
        return "PbcStubDataDto" + "[" +
                "apx=" + apx +
                ", index=" + index +
                ", congestionPointOne=" + congestionPointOne +
                ", congestionPointTwo=" + congestionPointTwo +
                ", congestionPointThree=" + congestionPointThree +
                ", congestionPointAvg=" + congestionPointAvg +
                ", pvLoadForecast=" + pvLoadForecast +
                ", pvLoadActual=" + pvLoadActual +
                "]";
    }
}
