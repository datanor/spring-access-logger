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

package ee.datanor.spring.logger.access;

import ee.datanor.spring.logger.access.processor.LogProcessor;
import ee.datanor.spring.logger.access.processor.request.CorrelationIdLogProcessor;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLoggerTest {

    @Mock
    private MultipartResolver multipartResolver;

    @Mock
    private UnrestrictedContentCachingRequestWrapper httpServletRequest;

    @Mock
    private ContentCachingResponseWrapper httpServletResponse;

    private AccessLogger accessLogger;

    @BeforeEach
    void setUp() {
        accessLogger = AccessLogger.builder()
                .logRequestBody(multipartResolver)
                .logResponseBody()
                .sensitiveUriParameter("/uri", "sensitive")
                .loggedRequestHeaders("h1")
                .loggedResponseHeaders("h1")
                .sensitiveBodyPattern("/uri", "\"key\"\\s*:\\s*\"([^\"]+)")
                .loggedResponseBodyMediaTypes(Set.of("json", "xml"))
                .addRequestProcessor(new CorrelationIdLogProcessor())
                .build();
        MDC.clear();
    }

    @Test
    void shouldGenerateHashes() {
        // given
        mockRequestHeaders();

        // when
        accessLogger.logRequest(httpServletRequest);

        // then
        assertNotEmpty("AL_CORRELATION_ID_HASH");
        assertNotEmpty("AL_REQUEST_HASH");
    }

    @Test
    void shouldAddRequestHeaders() {
        // given
        mockRequestHeaders();

        // when
        accessLogger.logRequest(httpServletRequest);

        // then
        assertNotEmpty("AL_REQUEST_HEADERS");
    }

    @Test
    void shouldAddRequestLine() {
        // given
        mockRequestHeaders();
        doReturn("GET").when(httpServletRequest).getMethod();
        doReturn("/uri").when(httpServletRequest).getRequestURI();
        doReturn("HTTP/1.1").when(httpServletRequest).getProtocol();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_LINE");

        // then
        assertEquals("GET /uri HTTP/1.1", result);
    }

    @Test
    void shouldAddRequestLineWithQueryString() {
        // given
        mockRequestHeaders();
        doReturn("GET").when(httpServletRequest).getMethod();
        doReturn("/uri").when(httpServletRequest).getRequestURI();
        doReturn("HTTP/1.1").when(httpServletRequest).getProtocol();
        doReturn("a=b&c=d").when(httpServletRequest).getQueryString();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_LINE");

        // then
        assertEquals("GET /uri?a=b&c=d HTTP/1.1", result);
    }

    @Test
    void shouldMaskSensitiveParameterOnRequestLine() {
        // given
        mockRequestHeaders();
        doReturn("GET").when(httpServletRequest).getMethod();
        doReturn("/uri").when(httpServletRequest).getRequestURI();
        doReturn("/uri").when(httpServletRequest).getServletPath();
        doReturn("HTTP/1.1").when(httpServletRequest).getProtocol();
        doReturn("a=b&sensitive=17&c=d").when(httpServletRequest).getQueryString();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_LINE");

        // then
        assertEquals("GET /uri?a=b&sensitive=***&c=d HTTP/1.1", result);
    }

    @Test
    void shouldAddRequestTime() {
        // given
        mockRequestHeaders();

        // when
        accessLogger.logRequest(httpServletRequest);

        // then
        assertNotEmpty("AL_REQUEST_TIME");
    }

    @Test
    void shouldAddServerName() {
        // given
        mockRequestHeaders();
        doReturn("localhost").when(httpServletRequest).getServerName();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_SERVER_NAME");

        // then
        assertEquals("localhost", result);
    }

    @Test
    void shouldAddServerPort() {
        // given
        mockRequestHeaders();
        doReturn(88).when(httpServletRequest).getServerPort();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_SERVER_PORT");

        // then
        assertEquals("88", result);
    }

    @Test
    void shouldAddClientIp() {
        // given
        mockRequestHeaders();
        doReturn("1.2.3.4").when(httpServletRequest).getRemoteAddr();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_CLIENT_IP");

        // then
        assertEquals("1.2.3.4", result);
    }

    @Test
    void shouldAddRequestBodyWhenDebugIsEnabled() throws IOException {
        // given
        mockRequestHeaders();
        doReturn("test").when(httpServletRequest).getContentAsString();

        // when
        accessLogger.logRequest(httpServletRequest);

        // then
        assertNotEmpty("AL_REQUEST_BODY");
    }

    @Test
    @Disabled
    void shouldLimitRequestBodyLength() throws IOException {
        // given
        mockRequestHeaders();
        doReturn("test").when(httpServletRequest).getContentAsString();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_BODY");

        // then
        assertEquals("te", result);
    }

    @Test
    void shouldMaskSensitiveRequestBody() throws IOException {
        // given
        mockRequestHeaders();
        doReturn("/uri").when(httpServletRequest).getServletPath();
        doReturn("{\"key\":\"value\"}").when(httpServletRequest).getContentAsString();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_BODY");

        // then
        assertEquals("{\"key\":\"***\"}", result);
    }

    @Test
    void shouldNotAddRequestBodyWhenDisabledInOptions() {
        // given
        mockRequestHeaders();

        // when
        accessLogger.logRequest(httpServletRequest);
        String result = MDC.get("AL_REQUEST_BODY");

        // then
        assertEquals(LogProcessor.EMPTY_REPLACEMENT, result);
    }

    @Test
    void shouldAddMultipartRequestParameters() {
        // given
        MultipartHttpServletRequest multipartHttpServletRequest = mock(MultipartHttpServletRequest.class);
        Map<String, String[]> parameterMap = new HashMap<>() {
            {
                put("param", new String[] {"value"});
            }
        };
        doReturn(parameterMap).when(multipartHttpServletRequest).getParameterMap();
        mockRequestHeaders(multipartHttpServletRequest);
        doReturn(true).when(multipartResolver).isMultipart(any());
        UnrestrictedContentCachingRequestWrapper httpServletRequestWrapper = new UnrestrictedContentCachingRequestWrapper(multipartHttpServletRequest);

        // when
        accessLogger.logRequest(httpServletRequestWrapper);
        String result = MDC.get("AL_REQUEST_BODY");

        // then
        assertEquals("param=value", result);
    }

    @Test
    void shouldAddResponseHeaders() {
        // given
        mockResponseHeaders();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);

        // then
        assertNotEmpty("AL_RESPONSE_HEADERS");
    }

    @Test
    void shouldAddResponseStatus() {
        // given
        mockResponseHeaders();
        doReturn(200).when(httpServletResponse).getStatus();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);

        // then
        assertNotEmpty("AL_RESPONSE_HEADERS");
    }

    @Test
    void shouldAddResponseBody() {
        // given
        mockResponseHeaders();
        doReturn("test".getBytes(StandardCharsets.UTF_8)).when(httpServletResponse).getContentAsByteArray();
        doReturn("UTF-8").when(httpServletResponse).getCharacterEncoding();
        doReturn("application/json").when(httpServletResponse).getContentType();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);

        // then
        assertNotEmpty("AL_RESPONSE_BODY");
    }

    @Test
    void shouldNotAddResponseBodyForAsyncDispatch() {
        // given
        mockResponseHeaders();
        doReturn("application/json").when(httpServletResponse).getContentType();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, true);

        // then
        verify(httpServletResponse, never()).getContentAsByteArray();
    }

    @Test
    void shouldNotAddResponseBodyForContentType() {
        // given
        mockResponseHeaders();
        doReturn("test".getBytes(StandardCharsets.UTF_8)).when(httpServletResponse).getContentAsByteArray();
        doReturn("UTF-8").when(httpServletResponse).getCharacterEncoding();
        doReturn("text/plain").when(httpServletResponse).getContentType();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);
        String result = MDC.get("AL_RESPONSE_BODY");

        // then
        assertEquals(LogProcessor.EMPTY_REPLACEMENT, result);
    }

    @Test
    @Disabled
    void shouldLimitMaxResponseBodyLength() {
        // given
        mockResponseHeaders();
        doReturn("test".getBytes(StandardCharsets.UTF_8)).when(httpServletResponse).getContentAsByteArray();
        doReturn("UTF-8").when(httpServletResponse).getCharacterEncoding();
        doReturn("application/json").when(httpServletResponse).getContentType();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);
        String result = MDC.get("AL_RESPONSE_BODY");

        // then
        assertEquals("te", result);
    }

    @Test
    void shouldNotAddResponseBodyWhenDisabledInOptions() {
        // given
        mockResponseHeaders();
        doReturn("test".getBytes(StandardCharsets.UTF_8)).when(httpServletResponse).getContentAsByteArray();
        doReturn("UTF-8").when(httpServletResponse).getCharacterEncoding();

        // when
        accessLogger.logResponse(httpServletRequest, httpServletResponse, false);
        String result = MDC.get("AL_RESPONSE_BODY");

        // then
        assertEquals(LogProcessor.EMPTY_REPLACEMENT, result);
    }

    private void mockRequestHeaders() {
        mockRequestHeaders(httpServletRequest);
    }

    private void mockRequestHeaders(HttpServletRequest httpServletRequest) {
        List<String> headerNames = Collections.singletonList("H1");
        Enumeration<String> enumeratedHeaderNames = Collections.enumeration(headerNames);
        Enumeration<String> headerValues = Collections.enumeration(Collections.singletonList("val"));
        doReturn(enumeratedHeaderNames).when(httpServletRequest).getHeaderNames();
        doReturn(headerValues).when(httpServletRequest).getHeaders(eq("H1"));
    }

    private void mockResponseHeaders() {
        List<String> headerNames = Arrays.asList("H1", "H2");
        List<String> headerValues = Collections.singletonList("val");
        doReturn(headerNames).when(httpServletResponse).getHeaderNames();
        doReturn(headerValues).when(httpServletResponse).getHeaders(eq("H1"));
        doReturn(headerValues).when(httpServletResponse).getHeaders(eq("H2"));
    }

    private void assertNotEmpty(String attr) {
        String value = MDC.get(attr);
        assertNotNull(value);
        assertNotEquals("-", value);
    }

    private void setLoggingLevel(String logger, Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();

    }
}