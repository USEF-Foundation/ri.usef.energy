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

/**
 * Wrapper class for runtime (unchecked) exceptions.
 */
public class TechnicalException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String technicalMessage;

    @SuppressWarnings("unused")
    private TechnicalException() {
        // private constructor.
        technicalMessage = null;
    }

    /**
     * Constructs a {@link TechnicalException} from any {@link Throwable}.
     *
     * @param e a {@link Throwable}.
     */
    public TechnicalException(Throwable e) {
        super(e);
        technicalMessage = null;
    }

    /**
     * Constructs a {@link TechnicalException} with a simple message.
     *
     * @param message {@link String} a human-readable message.
     */
    public TechnicalException(String message) {
        super(message);
        technicalMessage = message;
    }

    /**
     * Constructs a {@link TechnicalException} from a simple message and any {@link Throwable}.
     *
     * @param message {@link String} a human-readable message.
     * @param e a {@link Throwable}.
     */
    public TechnicalException(String message, Throwable e) {
        super(e);
        this.technicalMessage = message;
    }

    /**
     * Gets the technical message of the exception.
     *
     * @return {@link String} the technical message of the exception.
     */
    public String getTechnicalMessage() {
        return technicalMessage;
    }
}
