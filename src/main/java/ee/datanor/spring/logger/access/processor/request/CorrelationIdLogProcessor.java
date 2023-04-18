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

package ee.datanor.spring.logger.access.processor.request;

import ee.datanor.spring.logger.access.processor.RequestLogProcessor;
import ee.datanor.spring.logger.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public class CorrelationIdLogProcessor implements RequestLogProcessor {
    public static final String MDC_KEY = "AL_CORRELATION_ID_HASH";
    public static final int DEFAULT_HASH_LENGTH = 8;
    public static final String DEFAULT_CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

    private int hashLength;
    private String correlationIdHeaderName;

    public CorrelationIdLogProcessor() {
        this(DEFAULT_HASH_LENGTH, DEFAULT_CORRELATION_ID_HEADER_NAME);
    }

    public CorrelationIdLogProcessor(String correlationIdHeaderName) {
        this(DEFAULT_HASH_LENGTH, correlationIdHeaderName);
    }

    public CorrelationIdLogProcessor(int hashLength, String correlationIdHeaderName) {
        this.hashLength = hashLength;
        this.correlationIdHeaderName = correlationIdHeaderName;
    }

    @Override
    public void process(HttpServletRequest httpRequest) {
        String correlationId = httpRequest.getHeader(correlationIdHeaderName);
        if (StringUtils.isEmpty(correlationId)) {
            correlationId = HashUtil.generateHash(hashLength);
        }
        setMDCValue(MDC_KEY, correlationId);
    }
}
