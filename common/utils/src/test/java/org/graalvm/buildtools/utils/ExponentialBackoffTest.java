/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graalvm.buildtools.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExponentialBackoffTest {
    @Test
    @DisplayName("executed a passing operation")
    void simpleExecution() {
        AtomicBoolean success = new AtomicBoolean();
        ExponentialBackoff.get().execute(() -> success.set(true));
        assertTrue(success.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    @DisplayName("retries expected amount of times")
    void countRetries(int retries) {
        AtomicInteger count = new AtomicInteger();
        assertThrows(ExponentialBackoff.RetriableOperationFailedException.class, () -> ExponentialBackoff.get().withMaxRetries(retries)
            .execute(() -> {
                count.incrementAndGet();
                throw new RuntimeException();
            }));
        assertEquals(retries + 1, count.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    @DisplayName("passes after one retry")
    void passAfterRetry(int retries) {
        AtomicInteger count = new AtomicInteger();
        int result = ExponentialBackoff.get().withMaxRetries(retries)
            .supply(() -> {
                if (count.getAndIncrement() == 0) {
                    throw new RuntimeException();
                }
                return 200;
            });
        assertEquals(2, count.get());
        assertEquals(200, result);
    }

    @Test
    @DisplayName("can configure initial backoff time")
    void canConfigureInitialBackoffTime() {
        double sd = System.currentTimeMillis();
        assertThrows(ExponentialBackoff.RetriableOperationFailedException.class, () -> ExponentialBackoff.get()
            .withMaxRetries(4)
            .withInitialWaitPeriod(Duration.of(1, ChronoUnit.MILLIS))
            .execute(() -> {
                throw new RuntimeException();
            }));
        double duration = System.currentTimeMillis() - sd;
        assertTrue(duration < 100);
    }

}
