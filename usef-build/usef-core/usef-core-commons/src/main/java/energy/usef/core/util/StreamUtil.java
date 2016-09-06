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

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Utility class for the Stream API.
 */
public class StreamUtil {

    private StreamUtil() {

    }

    /**
     * Collector allowing to flatMap.
     *
     * @param mapper
     * @param downstream
     * @param <T> Orginial type used in the mapper.
     * @param <U> Generic type of the collected stream after the mapping.
     * @param <A> Type of the supplier.
     * @param <R> Type of the result.
     * @return a {@link Collector}.
     */
    public static <T, U, A, R> Collector<T, A, R> flatMapping(Function<T, Stream<U>> mapper,
            Collector<U, A, R> downstream) {
        BiConsumer<A, U> downstreamAccumulator = downstream.accumulator();
        return Collector.of(downstream.supplier(),
                (r, t) -> {
                    try (Stream<U> result = mapper.apply(t)) {
                        if (result != null) {
                            result.sequential().forEach(u -> downstreamAccumulator.accept(r, u));
                        }
                    }
                },
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics().toArray(new Collector.Characteristics[0]));
    }
}
