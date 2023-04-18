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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
public class AccessLoggingFilter extends OncePerRequestFilter {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String AL_PROCESSING_TIME_ATTR = "AL_PROCESSING_TIME";

    private final AccessLogger accessLogger;
    private final ThreadLocal<Long> requestStartTime = new ThreadLocal<>();

    public AccessLoggingFilter(AccessLogger accessLogger) {
        this.accessLogger = accessLogger;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        requestStartTime.set(System.currentTimeMillis());

        boolean isFirstRequest = isFirstRequest(request);
        HttpServletRequest httpRequest = getHttpRequest(request, isFirstRequest);
        AsyncAwareContentCachingResponseWrapper httpResponse = new AsyncAwareContentCachingResponseWrapper(response, httpRequest);
        try {
            if (isFirstRequest) {
                logRequest(httpRequest);
            }
            chain.doFilter(httpRequest, httpResponse);
        } finally {
            logResponseAndCleanup(httpRequest, httpResponse);
        }
    }

    protected HttpServletRequest getHttpRequest(HttpServletRequest request, boolean isFirstRequest) {
        boolean shouldLog = isFirstRequest && !(request.getClass().isAssignableFrom(ContentCachingRequestWrapper.class));
        if (shouldLog) {
            String contentType = request.getContentType();
            boolean isFormPost = contentType != null && contentType.contains(FORM_CONTENT_TYPE) && HttpMethod.POST.matches(request.getMethod());
            if (isFormPost) {
                ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
                //Trigger writeRequestParametersToCachedContent();
                wrapper.getParameterNames();
                return wrapper;
            } else {
                return new UnrestrictedContentCachingRequestWrapper(request);
            }
        }
        else {
            return request;
        }
    }

    protected void logRequest(HttpServletRequest httpRequest) {
        try {
            accessLogger.logRequest(httpRequest);
        } catch (Exception e) {
            log.error("Failed to log HTTP request", e);
        }
    }

    protected void logResponseAndCleanup(HttpServletRequest httpRequest, ContentCachingResponseWrapper responseWrapper) {
        try {
            addRequestProcessingTime();
            boolean isAsync = isAsyncDispatch(httpRequest);
            accessLogger.logResponse(httpRequest, responseWrapper, isAsync);
        } catch (Exception e) {
            log.error("Failed to log HTTP response", e);
        } finally {
            MDC.clear();
            requestStartTime.remove();
        }
        unwrapResponse(responseWrapper);
    }

    protected void addRequestProcessingTime() {
        MDC.put(AL_PROCESSING_TIME_ATTR, "" + (System.currentTimeMillis() - requestStartTime.get()));
    }

    protected boolean isFirstRequest(HttpServletRequest request) {
        return !isAsyncDispatch(request);
    }

    private void unwrapResponse(ContentCachingResponseWrapper response) {
        try {
            response.copyBodyToResponse();
        } catch (IOException e) {
            log.error("Failed to copy the cached body content to the response.", e);
        }
    }
}
