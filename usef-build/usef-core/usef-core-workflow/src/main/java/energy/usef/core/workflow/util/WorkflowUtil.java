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

package energy.usef.core.workflow.util;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.exception.WorkflowException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Util class to do simple workflow checks.
 */
public class WorkflowUtil {

    private WorkflowUtil() {
        // private constructor for util class
    }

    /**
     * Validate the context based on all Enum values.
     * 
     * @param name - The name of the Workflow Step we are currently in.
     * @param context - The {@link WorkflowContext} to validate.
     * @param values - The required enum keys.
     */
    public static void validateContext(String name, WorkflowContext context, @SuppressWarnings("rawtypes") Enum[] values) {
        List<String> nullProperties = new ArrayList<>();
        for (@SuppressWarnings("rawtypes")
        Enum value : values) {
            String requiredProperty = value.name();
            if (context.getValue(requiredProperty) == null) {
                nullProperties.add(requiredProperty);
            }
        }
        if (!nullProperties.isEmpty()) {
            StringBuilder message = new StringBuilder("WorkflowContext validation failed for ");
            message.append(name);
            message.append(System.lineSeparator());
            message.append("The following propeties are missing: ");
            message.append(StringUtils.join(nullProperties, ", "));
            throw new WorkflowException(message.toString());
        }
    }
}
