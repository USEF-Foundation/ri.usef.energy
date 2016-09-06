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

package energy.usef.agr.workflow.operate.reoptimize;

import org.joda.time.LocalDate;

/**
 * Excute Event for Re-optimizing portfolio of aggregator.
 *
 */
public class ExecuteReOptimizePortfolioEvent {
    private final LocalDate ptuDate;

    /**
     * Specific constructor for the {@link ExecuteReOptimizePortfolioEvent}.
     *
     * @param ptuDate {@link LocalDate} period for which the portfolio needs to be optimized.
     */
    public ExecuteReOptimizePortfolioEvent(LocalDate ptuDate) {
        this.ptuDate = ptuDate;
    }

    public LocalDate getPtuDate() {
        return ptuDate;
    }

    @Override
    public String toString() {
        return "ExecuteReOptimizePortfolioEvent" + "[" +
                "ptuDate=" + ptuDate +
                "]";
    }
}
