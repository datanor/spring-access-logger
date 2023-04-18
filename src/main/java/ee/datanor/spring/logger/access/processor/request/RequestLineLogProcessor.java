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

import ee.datanor.spring.logger.util.ParameterMasker;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public class RequestLineLogProcessor extends ParameterMaskingRequestLogProcessor {
    public static final String MDC_KEY = "AL_REQUEST_LINE";

    public RequestLineLogProcessor() {
        this(new HashMap<>());
    }

    public RequestLineLogProcessor(Map<String, ParameterMasker> maskers) {
        super(maskers);
    }

    @Override
    public void process(HttpServletRequest httpRequest) {
        setMDCValue(MDC_KEY, replaceEmpty(getRequestLine(httpRequest)));
    }

    private String getRequestLine(HttpServletRequest httpRequest) {
        return String.format("%s %s %s", httpRequest.getMethod(), getRequestUrl(httpRequest), httpRequest.getProtocol());
    }

    private String getRequestUrl(HttpServletRequest httpRequest) {
        if (httpRequest.getQueryString() != null) {
            return httpRequest.getRequestURI() + "?" + maskSensitiveParameters(httpRequest, httpRequest.getQueryString());
        }
        return httpRequest.getRequestURI();
    }
}
