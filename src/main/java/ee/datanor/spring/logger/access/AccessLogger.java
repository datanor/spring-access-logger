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

import ee.datanor.spring.logger.access.processor.RequestLogProcessor;
import ee.datanor.spring.logger.access.processor.ResponseLogProcessor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.List;

@Slf4j
public class AccessLogger {
    private final Logger requestLogger = LoggerFactory.getLogger("access-request-log");
    private final Logger responseLogger = LoggerFactory.getLogger("access-response-log");
    private final List<RequestLogProcessor> requestLogProcessors;
    private final List<ResponseLogProcessor> responseLogProcessors;

    public static AccessLoggerBuilder builder() {
        return new AccessLoggerBuilder();
    }

    public AccessLogger(List<RequestLogProcessor> requestLogProcessors, List<ResponseLogProcessor> responseLogProcessors) {
        this.requestLogProcessors = requestLogProcessors;
        this.responseLogProcessors = responseLogProcessors;
    }

    public void logRequest(HttpServletRequest httpRequest) {
        requestLogProcessors.forEach(p -> p.process(httpRequest));
        requestLogger.info("Incoming Request {}", MDC.get("AL_REQUEST_LINE"));
    }

    public void logResponse(HttpServletRequest httpRequest, ContentCachingResponseWrapper httpResponse, boolean isAsync) {
        responseLogProcessors.forEach(p -> p.process(httpRequest, httpResponse, isAsync));
        responseLogger.info("Outgoing response {}", MDC.get("AL_REQUEST_LINE"));
    }
}
