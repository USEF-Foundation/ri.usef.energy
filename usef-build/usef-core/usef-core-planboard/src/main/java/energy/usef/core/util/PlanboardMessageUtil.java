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

import energy.usef.core.model.PlanboardMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This utility class provides methods to work with {@link PlanboardMessage}.
 */
public class PlanboardMessageUtil {

    private PlanboardMessageUtil() {
        // private constructor
    }

    /**
     * Sort a list of {@link PlanboardMessage} by date with newest first.
     *
     * @param planboardMessageList
     * @return
     */
    public static List<PlanboardMessage> sortPlanboardMessageListDescByCreationTime(List<PlanboardMessage> planboardMessageList) {
        return planboardMessageList.stream()
                .sorted((e1, e2) -> e2.getCreationDateTime().compareTo(e1.getCreationDateTime()))
                .collect(Collectors.toList());
    }

}
