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

import ee.datanor.spring.logger.access.UnrestrictedContentCachingRequestWrapper;
import ee.datanor.spring.logger.util.BodyMasker;
import ee.datanor.spring.logger.util.ParameterMasker;
import ee.datanor.spring.logger.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class RequestBodyLogProcessor extends ParameterMaskingRequestLogProcessor {
    public static final String MDC_KEY = "AL_REQUEST_BODY";

    private final int maxLoggedRequestLength;
    private final MultipartResolver multipartResolver;
    private final Map<String, BodyMasker> sensitiveBodyMaskers;

    public RequestBodyLogProcessor(MultipartResolver multipartResolver) {
        this(Map.of(), 2048, multipartResolver, Map.of());
    }

    public RequestBodyLogProcessor(Map<String, ParameterMasker> maskers, int maxLoggedRequestLength,
                                   MultipartResolver multipartResolver, Map<String, BodyMasker> sensitiveBodyMaskers) {
        super(maskers);
        this.maxLoggedRequestLength = maxLoggedRequestLength;
        this.multipartResolver = multipartResolver;
        this.sensitiveBodyMaskers = sensitiveBodyMaskers;
    }

    @Override
    public void process(HttpServletRequest httpRequest) {
        String requestBody = replaceEmpty(getRequestBody(httpRequest));
        if (requestBody.length() > maxLoggedRequestLength) {
            setMDCValue(MDC_KEY, requestBody.substring(0, maxLoggedRequestLength));
        } else {
            setMDCValue(MDC_KEY, requestBody);
        }
    }

    private String getRequestBody(HttpServletRequest httpRequest) {
        try {
            String content;
            if (multipartResolver.isMultipart(httpRequest)) {
                content = parseMultipartRequest((HttpServletRequestWrapper) httpRequest);
            } else {
                content = parseRequest(httpRequest);
            }
            String maskedContent = maskSensitiveParameters(httpRequest, content);
            return maskSensitiveBody(httpRequest, pathMatcher, sensitiveBodyMaskers, maskedContent);
        } catch (MultipartException | IOException e) {
            log.error("Failed to read request body", e);
        }
        return null;
    }

    private String parseMultipartRequest(HttpServletRequestWrapper requestWrapper) {
        MultipartHttpServletRequest multipartHttpServletRequest;
        if (WebUtils.getNativeRequest(requestWrapper.getRequest(), MultipartHttpServletRequest.class) != null) {
            log.debug("Request is already a MultipartHttpServletRequest");
            multipartHttpServletRequest = (MultipartHttpServletRequest) requestWrapper.getRequest();
        } else {
            multipartHttpServletRequest = multipartResolver.resolveMultipart(requestWrapper);
        }
        return RequestUtil.requestParametersToString(multipartHttpServletRequest);
    }

    private String parseRequest(HttpServletRequest request) throws IOException {
        if (request instanceof UnrestrictedContentCachingRequestWrapper) {
            UnrestrictedContentCachingRequestWrapper requestWrapper = (UnrestrictedContentCachingRequestWrapper) request;
            return requestWrapper.getContentAsString();
        } else {
            ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper) request;
            return new String(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
        }
    }
}
