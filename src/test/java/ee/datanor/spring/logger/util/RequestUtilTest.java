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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RequestUtilTest {

    @Mock
    private HttpServletRequest httpServletRequest;

    @Test
    void shouldConvertRequestParameterMapToString() {
        // given
        Map<String, String[]> parameterMap = new HashMap<>() {
            {
                put("param", new String[] {"value"});
            }
        };
        doReturn(parameterMap).when(httpServletRequest).getParameterMap();

        // when
        String result = RequestUtil.requestParametersToString(httpServletRequest);

        // then
        assertEquals("param=value", result);
    }

    @Test
    void shouldConvertMultipleRequestParameterMapToString() {
        // given
        Map<String, String[]> parameterMap = new HashMap<>() {
            {
                put("param", new String[] {"value"});
                put("param2", new String[] {"value2"});
            }
        };
        doReturn(parameterMap).when(httpServletRequest).getParameterMap();

        // when
        String result = RequestUtil.requestParametersToString(httpServletRequest);

        // then
        assertEquals("param=value&param2=value2", result);
    }
}