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

package energy.usef.agr.workflow.nonudi.service;

/**
 * Wrapper class which contains the return value for PowerMatcher requests and the status code of the request.
 * Public for unit tests.
 *
 * @param <T>
 */
public class HttpReturnValue<T> {
    private int status;
    private Object returnValue;

    /**
     * Constructs a HttpReturnValue with the specified status and returnValue.
     *
     * @param status        a specific status.
     * @param returnValue   a specific returnValue.
     */
    public HttpReturnValue(int status, Object returnValue) {
        this.status = status;
        this.returnValue = returnValue;
    }

    public int getStatus() {
        return status;
    }

    public <T> T getReturnValue() {
        return (T) returnValue;
    }
}
