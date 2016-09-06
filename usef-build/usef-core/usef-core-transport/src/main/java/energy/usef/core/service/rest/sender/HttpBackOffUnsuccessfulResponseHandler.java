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

package energy.usef.core.service.rest.sender;

import java.io.IOException;
import java.util.List;

import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.BackOffUtils;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.Sleeper;

/**
 * Back-off handler which handles an abnormal HTTP response with {@link BackOff}.
 * <p>
 * <p>
 * It is designed to work with only one {@link HttpRequest} at a time. As a result you MUST create a new instance of
 * {@link HttpBackOffIOExceptionHandler} with a new instance of {@link BackOff} for each instance of {@link HttpRequest}.
 * </p>
 * <p>
 * <p>
 * Sample usage:
 * </p>
 * <p>
 * <pre>
 * request.setUnsuccessfulResponseHandler(
 *         new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
 * </pre>
 * <p>
 * <p>
 * Note: Implementation doesn't call {@link BackOff#reset} at all, since it expects a new {@link BackOff} instance.
 * </p>
 * <p>
 * <p>
 * Implementation is not thread-safe
 * </p>
 */
public class HttpBackOffUnsuccessfulResponseHandler implements HttpUnsuccessfulResponseHandler {
    private static final int STATUS_CODE_MOD = 5;
    private static final int DIVIDE_BY_100 = 100;

    /**
     * Back-off policy.
     */
    private final BackOff backOff;
    private final List<Integer> retryHttpErrorCodes;

    /**
     * Defines if back-off is required based on an abnormal HTTP response.
     */
    private BackOffRequired backOffRequired = ON_SERVER_ERROR;

    /**
     * Sleeper.
     */
    private Sleeper sleeper = Sleeper.DEFAULT;

    /**
     * Back-off required implementation which returns {@code true} to every {@link #isRequired(HttpResponse)} call.
     */
    public static final BackOffRequired ALWAYS = (response, retryHttpErrorCodes1) -> true;

    /**
     * Back-off required implementation which its {@link #isRequired(HttpResponse)} returns {@code true} if a server error occurred
     * (5xx).
     */
    public static final BackOffRequired ON_SERVER_ERROR = (response, retryHttpErrorCodes1) ->
            retryHttpErrorCodes1.contains(response.getStatusCode())
                    || response.getStatusCode() / DIVIDE_BY_100 == STATUS_CODE_MOD;

    /**
     * Constructs a new instance from a {@link BackOff}.
     *
     * @param backOff back-off policy
     * @param retryHttpErrorCodes http error codes that must be retried
     */
    public HttpBackOffUnsuccessfulResponseHandler(BackOff backOff, List<Integer> retryHttpErrorCodes) {
        this.backOff = Preconditions.checkNotNull(backOff);
        this.retryHttpErrorCodes = Preconditions.checkNotNull(retryHttpErrorCodes);
    }

    /**
     * Returns the back-off.
     */
    public final BackOff getBackOff() {
        return backOff;
    }

    /**
     * Returns the {@link BackOffRequired} instance which determines if back-off is required based on an abnormal HTTP response.
     */
    public final BackOffRequired getBackOffRequired() {
        return backOffRequired;
    }

    /**
     * Sets the {@link BackOffRequired} instance which determines if back-off is required based on an abnormal HTTP response.
     * <p>
     * <p>
     * The default value is {@link BackOffRequired#ON_SERVER_ERROR}.
     * </p>
     * <p>
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing the return type, but nothing
     * else.
     * </p>
     */
    public HttpBackOffUnsuccessfulResponseHandler setBackOffRequired(
            BackOffRequired backOffRequired) {
        this.backOffRequired = Preconditions.checkNotNull(backOffRequired);
        return this;
    }

    /**
     * Returns the sleeper.
     */
    public final Sleeper getSleeper() {
        return sleeper;
    }

    /**
     * Sets the sleeper.
     * <p>
     * <p>
     * The default value is {@link Sleeper#DEFAULT}.
     * </p>
     * <p>
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing the return type, but nothing
     * else.
     * </p>
     */
    public HttpBackOffUnsuccessfulResponseHandler setSleeper(Sleeper sleeper) {
        this.sleeper = Preconditions.checkNotNull(sleeper);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleResponse(
            HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
        if (!supportsRetry) {
            return false;
        }
        // check if back-off is required for this response
        if (backOffRequired.isRequired(response, retryHttpErrorCodes)) {
            try {
                return BackOffUtils.next(sleeper, backOff);
            } catch (InterruptedException exception) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Interface which defines if back-off is required based on an abnormal {@link HttpResponse}.
     */
    public interface BackOffRequired {

        /**
         * Invoked when an abnormal response is received and determines if back-off is required.
         *
         * @param response the {@link HttpResponse} received
         * @param retryHttpErrorCodes the {@link List} of http error codes that must be retried
         * @return boolean
         */
        boolean isRequired(HttpResponse response, List<Integer> retryHttpErrorCodes);
    }
}
