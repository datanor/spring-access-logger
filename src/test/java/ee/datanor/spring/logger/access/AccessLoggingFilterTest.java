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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLoggingFilterTest {

    @Mock
    private AccessLogger accessLogger;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    @Spy
    private AccessLoggingFilter accessLoggingFilter;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void shouldLogRequest() throws IOException, ServletException {
        // when
        accessLoggingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(accessLogger, times(1)).logRequest(any(HttpServletRequest.class));
    }

    @Test
    void shouldNotLogRequestForAsyncDispatch() throws IOException, ServletException {
        // given
        doReturn(false).when(accessLoggingFilter).isFirstRequest(any(HttpServletRequest.class));

        // when
        accessLoggingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(accessLogger, never()).logRequest(any(HttpServletRequest.class));
    }

    @Test
    void shouldSuppressLogRequestException() throws IOException, ServletException {
        // given
        doThrow(new RuntimeException()).when(accessLogger).logRequest(any(HttpServletRequest.class));

        // when
        accessLoggingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }


    @Test
    void shouldLogResponse() throws IOException, ServletException {
        // when
        accessLoggingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(accessLogger, times(1)).logResponse(any(HttpServletRequest.class), any(ContentCachingResponseWrapper.class), anyBoolean());
    }

    @Test
    void shouldSuppressLogResponseException() throws IOException, ServletException {
        // given
        doThrow(new RuntimeException()).when(accessLogger).logResponse(any(HttpServletRequest.class), any(ContentCachingResponseWrapper.class), anyBoolean());

        // when
        accessLoggingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
