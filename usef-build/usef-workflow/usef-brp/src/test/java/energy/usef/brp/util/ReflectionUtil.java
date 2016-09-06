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

package energy.usef.brp.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This utility class contains methods using reflection.
 */
public class ReflectionUtil {

    private ReflectionUtil() {
        // private constructor
    }

    /**
     * Modifies the value of a <code>static final</code> field.
     *
     * @param field    - {@link Field} to modify
     * @param newValue - new value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    public static void setFinalStatic(Field field, Object newValue) throws IllegalAccessException, NoSuchFieldException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
