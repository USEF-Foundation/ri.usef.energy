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

package energy.usef.core.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BusinessExceptionTest implements BusinessError {

    @Override
    public String getError() {
        return "differentmessage";
    }

    @Test
    public void testBusinessException() {
        BusinessException e1 = new BusinessException(null);
        BusinessException e2 = new BusinessException(this);
        assertNull(e1.getBusinessError());
        assertEquals("differentmessage", e2.getMessage());
    }

    @Test
    public void testDefaultMessage() {
        BusinessException e1 = new BusinessException(ExampleBusinessError.EMPTY_MESSAGE);
        assertEquals("Congestionpoint does not exist.", e1.getMessage());
    }

    @Test
    public void testDefaultMessageWithParameters() {
        BusinessException e1 = new BusinessException(ExampleBusinessError.EXAMPLE_ERROR, "1", "2", "3");
        assertEquals("1 2 3.", e1.getMessage());
    }

    @Test
    public void testDefaultMessageWithOtherParameters() {
        BusinessException e1 = new BusinessException(ExampleBusinessError.EXAMPLE_ERROR, "1", 2, 3.2);
        assertEquals("1 2 3.2.", e1.getMessage());
    }

    @Test
    public void testDefaultMessageWithNoParameters() {
        BusinessException e1 = new BusinessException(ExampleBusinessError.EXAMPLE_ERROR);
        assertEquals("{} {} {}.", e1.getMessage());
    }

    public enum ExampleBusinessError implements BusinessError {
        EMPTY_MESSAGE("Congestionpoint does not exist."),
        EXAMPLE_ERROR("{} {} {}.");

        private final String errorMessage;

        ExampleBusinessError(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getError() {
            return errorMessage;
        }
    }

}
