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

package energy.usef.cro.service.business;

/**
 * The enumeration represents the Common Reference run mode: open or closed.
 */
public enum CommonReferenceMode {
    OPEN("OPEN"), CLOSED("CLOSED");

    private final String value;

    /**
     * Constructor using a defined value.
     * 
     * @param value value to be parsed
     */
    CommonReferenceMode(String value) {
        this.value = value;
    }

    /**
     * Return the value of the enumeration.
     * 
     * @return string representation
     */
    public String value() {
        return value;
    }

    /**
     * Return the enumeration for a value.
     * 
     * @param value value to be parsed
     * @return corresponding object of type CommonReferenceMode
     */
    public static CommonReferenceMode fromValue(String value) {
        for (CommonReferenceMode c : CommonReferenceMode.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
