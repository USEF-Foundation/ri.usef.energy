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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

/**
 * This utility class provides additional methods for the java.util.concurrent classes.
 */
@ApplicationScoped
public class ConcurrentUtil {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);



    /**
     * Fail a {@link CompletableFuture} after the specified {@link Duration}.
     *
     * @param duration the {@link Duration}to wait before failing.
     */
    public <T> CompletableFuture<T> failAfter(Duration duration, String timeoutMessage) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException(timeoutMessage + " " + duration);
            return promise.completeExceptionally(ex);
        }, duration.toMillis(), MILLISECONDS);
        return promise;
    }

    /**
     * Helper method to link A {@link CompletableFuture} failing after the specified {@link Duration}.
     *
     * @param future the {@link CompletableFuture} that is supposed to complete
     * @param duration the {@link Duration}to wait before failing.
     *
     */
    public <T> CompletableFuture<T> within(CompletableFuture<T> future, Duration duration, String timeoutMessage) {
        final CompletableFuture<T> timeout = failAfter(duration, timeoutMessage);
        return future.applyToEither(timeout, Function.identity());
    }

}
