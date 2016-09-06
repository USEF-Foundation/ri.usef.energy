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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import org.joda.time.LocalDate;

/**
 * This class maintains the flags, whether Portfolio ReOptimization should be run again, or is already running.
 */
@Singleton
public class ReOptimizeFlagHolder {

    private Map<LocalDate, Boolean> isRunningMap = new HashMap<>();
    private Map<LocalDate, Boolean> toBeReoptimizedMap = new HashMap<>();

    /**
     * Returns the Running Flag.
     *
     * @param period
     * @return
     */
    @Lock(LockType.WRITE)
    public boolean isRunning(LocalDate period) {
        return isRunningMap.getOrDefault(period, false);
    }

    /**
     * Returns the ToBeReoptimized Flag.
     *
     * @param period
     * @return
     */
    @Lock(LockType.WRITE)
    public boolean toBeReoptimized(LocalDate period) {
        return toBeReoptimizedMap.getOrDefault(period, false);
    }

    /**
     * Sets the flag if true, other wise removes it.
     *
     * @param period
     * @param value
     */
    @Lock(LockType.WRITE)
    public void setIsRunning(LocalDate period, boolean value) {
        if (value) {
            isRunningMap.put(period, value);
        } else {
            isRunningMap.remove(period);
        }
    }

    /**
     * Sets the flag if true, other wise removes it.
     *
     * @param period
     * @param value
     */
    @Lock(LockType.WRITE)
    public void setToBeReoptimized(LocalDate period, boolean value) {
        if (value) {
            toBeReoptimizedMap.put(period, value);
        } else {
            toBeReoptimizedMap.remove(period);
        }
    }

}
