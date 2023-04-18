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

package ee.datanor.spring.logger.access.processor.response;

import ee.datanor.spring.logger.access.processor.ResponseLogProcessor;
import ee.datanor.spring.logger.util.BodyMasker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.Map;
import java.util.Set;

@Slf4j
public class ResponseBodyLogProcessor implements ResponseLogProcessor {
    private static final String LENGTH_MDC_KEY = "AL_RESPONSE_BODY_LENGTH";
    private static final String BODY_MDC_KEY = "AL_RESPONSE_BODY";
    private static final int DEFAULT_MAX_LOGGED_CONTENT_LENGTH = 2048;

    private final int maxLoggedResponseLength;
    private final Set<String> includedResponseBodyMediaSubtypes;
    private final Map<String, BodyMasker> sensitiveBodyMaskers;
    protected final PathMatcher pathMatcher = new AntPathMatcher();

    public ResponseBodyLogProcessor() {
        this(DEFAULT_MAX_LOGGED_CONTENT_LENGTH, Set.of("json", "xml"), Map.of());
    }


    public ResponseBodyLogProcessor(int maxLoggedResponseLength, Set<String> includedResponseBodyMediaSubtypes, Map<String, BodyMasker> sensitiveBodyMaskers) {
        this.maxLoggedResponseLength = maxLoggedResponseLength;
        this.includedResponseBodyMediaSubtypes = includedResponseBodyMediaSubtypes;
        this.sensitiveBodyMaskers = sensitiveBodyMaskers;
    }

    @Override
    public void process(HttpServletRequest httpRequest, ContentCachingResponseWrapper responseWrapper, boolean isASync) {
        String responseBody;
        if (!isASync) {
            responseBody = getResponseBody(responseWrapper);
            setMDCValue(LENGTH_MDC_KEY, replaceEmpty(responseBody.length()));
        } else {
            responseBody = EMPTY_REPLACEMENT;
            setMDCValue(LENGTH_MDC_KEY, EMPTY_REPLACEMENT);
        }
        if (responseBodyMediaSubtypeMatches(responseWrapper)) {
            String maskedResponseBody = maskSensitiveBody(httpRequest, pathMatcher, sensitiveBodyMaskers, responseBody);
            if (maskedResponseBody.length() > maxLoggedResponseLength) {
                setMDCValue(BODY_MDC_KEY, maskedResponseBody.substring(0, maxLoggedResponseLength));
            } else {
                setMDCValue(BODY_MDC_KEY, replaceEmpty(maskedResponseBody));
            }
        } else {
            setMDCValue(BODY_MDC_KEY, EMPTY_REPLACEMENT);
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper httpResponse) {
        try {
            byte[] content = httpResponse.getContentAsByteArray();
            return new String(content, httpResponse.getCharacterEncoding());
        } catch (Exception e) {
            log.error("Failed to read response attributes", e);
            return "";
        }
    }

    private boolean responseBodyMediaSubtypeMatches(HttpServletResponse httpServletResponse) {
        String contentType = httpServletResponse.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            return false;
        }
        String subtype = MediaType.parseMediaType(contentType).getSubtype();
        return includedResponseBodyMediaSubtypes.stream()
                .map(String::toLowerCase)
                .anyMatch(st -> st.equalsIgnoreCase(subtype));
    }
}
