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
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class HeaderUtilTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;

    @Test
    void shouldConvertRequestHeadersToString() {
        // given
        List<String> headerNames = Arrays.asList("H1", "H2");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues1 = Collections.enumeration(Collections.singletonList("val"));
        Enumeration<String> headerValues2 = Collections.enumeration(Collections.singletonList("val"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues1).when(httpServletRequest).getHeaders(eq("H1"));
        doReturn(headerValues2).when(httpServletRequest).getHeaders(eq("H2"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("H1: val\nH2: val\n", headersAsString);
    }

    @Test
    void shouldExcludeNotIncludedRequestHeaders() {
        // given
        List<String> headerNames = Arrays.asList("H1", "H2");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues1 = Collections.enumeration(Collections.singletonList("val"));
        Enumeration<String> headerValues2 = Collections.enumeration(Collections.singletonList("val"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues1).when(httpServletRequest).getHeaders(eq("H1"));
        doReturn(headerValues2).when(httpServletRequest).getHeaders(eq("H2"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(Collections.singletonList("H2")));

        // then
        assertEquals("H2: val\n", headersAsString);
    }

    @Test
    void shouldReturnAllRequestHeadersWhenHeadersToIncludeIsNull() {
        // given
        List<String> headerNames = Arrays.asList("H1", "H2");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues1 = Collections.enumeration(Collections.singletonList("val"));
        Enumeration<String> headerValues2 = Collections.enumeration(Collections.singletonList("val"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues1).when(httpServletRequest).getHeaders(eq("H1"));
        doReturn(headerValues2).when(httpServletRequest).getHeaders(eq("H2"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, null);

        // then
        assertEquals("H1: val\nH2: val\n", headersAsString);
    }

    @Test
    void shouldHandleRequestHeadersWithMultipleValues() {
        // given
        List<String> headerNames = Arrays.asList("H1");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Arrays.asList("val1", "val2"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("H1"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("H1: val1;val2\n", headersAsString);
    }

    @Test
    void shouldConvertResponseHeadersToString() {
        // given
        List<String> headerNames = Arrays.asList("H1", "H2");
        List<String> headerValues = Collections.singletonList("val");
        doReturn(headerNames).when(httpServletResponse).getHeaderNames();
        doReturn(headerValues).when(httpServletResponse).getHeaders(eq("H1"));
        doReturn(headerValues).when(httpServletResponse).getHeaders(eq("H2"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletResponse, new HashSet<>(headerNames));

        // then
        assertEquals("H1: val\nH2: val\n", headersAsString);
    }

    @Test
    void shouldStripSignatureFromBearerToken() {
        // given
        List<String> headerNames = List.of("Authorization");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Collections.singletonList("Bearer ABC.CDE.FGH"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("Authorization"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("Authorization: Bearer ABC.CDE.***\n", headersAsString);
    }


    @Test
    void shouldMaskNonBearerAuthorizationHeader() {
        // given
        List<String> headerNames = List.of("Authorization");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Collections.singletonList("Basic ABCDEF"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("Authorization"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("Authorization: ***\n", headersAsString);
    }

    @Test
    void shouldHandleEmptyAuthorizationHeader() {
        // given
        List<String> headerNames = List.of("Authorization");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Collections.singletonList(""));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("Authorization"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("Authorization: \n", headersAsString);
    }

    @Test
    void shouldHandleMalformedBearerAuthorizationHeader() {
        // given
        List<String> headerNames = List.of("Authorization");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Collections.singletonList("Bearer none"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("Authorization"));

        // when
        String headersAsString = HeaderUtil.headersToString(httpServletRequest, new HashSet<>(headerNames));

        // then
        assertEquals("Authorization: Bearer none\n", headersAsString);
    }
}