/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.jooq;

import java.util.function.Consumer;

/**
 * A <code>RecordHandler</code> is a handler that can receive {@link Record}
 * objects, when fetching data from the database.
 *
 * @author Lukas Eder
 * @deprecated - 3.15.0 - [#11902] - Use {@link Iterable#forEach(Consumer)}
 *             based methods, instead.
 */
@Deprecated(forRemoval = true, since = "3.15")
@FunctionalInterface
public interface RecordHandler<R extends Record> extends Consumer<R> {

    /**
     * A callback method indicating that the next record has been fetched.
     */
    void next(R record);

    @Override
    default void accept(R record) {
        next(record);
    }
}
