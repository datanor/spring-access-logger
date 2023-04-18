/*
 * Copyright 2023 Datanor OÜ.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.datanor.spring.logger.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeUtilTest {

    private static Stream<Arguments> testArgments() {
        return Stream.of(
                Arguments.of("test\t", "test\\t"),
                Arguments.of("te\tst", "te\\tst"),
                Arguments.of("test\b", "test\\b"),
                Arguments.of("te\bst", "te\\bst"),
                Arguments.of("test\n", "test\\n"),
                Arguments.of("te\nst", "te\\nst"),
                Arguments.of("nothing to escape", "nothing to escape")
        );
    }

    @ParameterizedTest
    @MethodSource("testArgments")
    void shouldEscapeSpecialCharacters(String input, String expected) {
        // when
        String result = EscapeUtil.escape(input);

        // then
        assertEquals(expected, result);
    }
}