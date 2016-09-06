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
 * Throwable class for the Business Exception.
 */
public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;

    private final BusinessError businessError;
    private String message;

    /**
     * Constructs a BusinessException with the specified businessError and detail message.
     *
     * @param businessError a specific {@link BusinessException}.
     * @param message       a specific detail message}.
     */
    public BusinessException(BusinessError businessError, String message) {
        super(message);
        this.businessError = businessError;
    }

    /**
     * Constructs a BusinessException with the specified businessError.
     *
     * @param businessError a specific type of {@link BusinessException}.
     * @param errorValues
     */
    public BusinessException(BusinessError businessError, Object... errorValues) {
        this.businessError = businessError;
        parse(errorValues);
    }

    /**
     * Parses everything between {} in the error messages.
     *
     * @param errorValues a list of objects which represents the parameters to put in the error message.
     */
    private void parse(Object... errorValues) {
        if (businessError != null) {
            message = businessError.getError();
            if (message != null) {
                for (Object errorValue : errorValues) {
                    message = message.replaceFirst("\\{\\}", errorValue.toString());
                }
            }
        }
    }

    public BusinessError getBusinessError() {
        return businessError;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
