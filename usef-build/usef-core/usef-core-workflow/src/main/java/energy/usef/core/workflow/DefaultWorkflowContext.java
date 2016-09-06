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

package energy.usef.core.workflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class contains the context of the workflow.
 */
public class DefaultWorkflowContext implements WorkflowContext {

    private Map<String, Object> map = new HashMap<>();

    /**
     * Gets a value by a key.
     *
     * @param key key
     * @return value
     */
    public Object getValue(String key) {
        return map.get(key);
    }

    /**
     * Sets a key/value.
     *
     * @param key key
     * @param value value
     */
    public void setValue(String key, Object value) {
        map.put(key, value);
    }

    /**
     * Removes a value.
     *
     * @param key key
     */
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public String toString() {
        return map.keySet().stream().map(key -> {
            if (map.get(key) != null && map.get(key) instanceof Collection) {
                return key + "=" + ((Collection) map.get(key)).size() + " elements ";
            }
            return key + "=" + map.get(key);
        }).collect(Collectors.joining("\n\t # ", "\n\t # ", ""));
    }
}
