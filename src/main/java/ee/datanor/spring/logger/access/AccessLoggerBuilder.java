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
import ee.datanor.spring.logger.access.processor.request.ClientIpLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestBodyLengthLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestBodyLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestHashLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestHeadersLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestLineLogProcessor;
import ee.datanor.spring.logger.access.processor.request.RequestTimeLogProcessor;
import ee.datanor.spring.logger.access.processor.request.ServerInfoLogProcessor;
import ee.datanor.spring.logger.access.processor.response.ResponseBodyLogProcessor;
import ee.datanor.spring.logger.access.processor.response.ResponseHeadersLogProcessor;
import ee.datanor.spring.logger.access.processor.response.ResponseStatusLogProcessor;
import ee.datanor.spring.logger.util.BodyMasker;
import ee.datanor.spring.logger.util.ParameterMasker;
import org.springframework.web.multipart.MultipartResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessLoggerBuilder {
    private final Map<String, ParameterMasker> parameterMaskers = new HashMap<>();
    private final Map<String, BodyMasker> bodyMaskers = new HashMap<>();
    private final List<RequestLogProcessor> requestLogProcessors;
    private final List<ResponseLogProcessor> responseLogProcessors;
    private int maxLoggedRequestBodyLength = 1024;
    private int maxLoggedResponseBodyLength = 1024;
    private boolean logRequestBody = false;
    private boolean logResponseBody = false;
    private List<RequestLogProcessor> additionalRequestLogProcessors = new ArrayList<>();
    private List<ResponseLogProcessor> additionalResponseLogProcessors = new ArrayList<>();
    private Set<String> includedRequestHeaders = new HashSet<>();
    private Set<String> includedResponseHeaders = new HashSet<>();
    private MultipartResolver multipartResolver;
    private Set<String> loggedResponseBodyMediaTypes = new HashSet<>(Set.of("json", "xml"));

    AccessLoggerBuilder() {
        this.requestLogProcessors = new ArrayList<>(List.of(
                new RequestTimeLogProcessor(),
                new ServerInfoLogProcessor(),
                new ClientIpLogProcessor(),
                new RequestHashLogProcessor()
        ));
        this.responseLogProcessors = new ArrayList<>(List.of(
                new ResponseStatusLogProcessor()
        ));
    }

    public AccessLoggerBuilder sensitiveUriParameter(String uriPattern, String parameterName) {
        this.parameterMaskers.put(uriPattern, new ParameterMasker(parameterName));
        return this;
    }

    public AccessLoggerBuilder sensitiveBodyPattern(String uriPattern, String regexp) {
        this.bodyMaskers.put(uriPattern, new BodyMasker(regexp));
        return this;
    }

    public AccessLoggerBuilder maxRequestBodyLength(int maxLoggedRequestBodyLength) {
        this.maxLoggedRequestBodyLength = maxLoggedRequestBodyLength;
        return this;
    }

    public AccessLoggerBuilder maxResponseBodyLength(int maxLoggedResponseBodyLength) {
        this.maxLoggedResponseBodyLength = maxLoggedResponseBodyLength;
        return this;
    }

    public AccessLoggerBuilder logRequestBody(MultipartResolver multipartResolver) {
        if (multipartResolver != null) {
            this.multipartResolver = multipartResolver;
            this.logRequestBody = true;
        }
        return this;
    }

    public AccessLoggerBuilder logResponseBody() {
        this.logResponseBody = true;
        return this;
    }

    public AccessLoggerBuilder addRequestProcessor(RequestLogProcessor requestLogProcessor) {
        this.additionalRequestLogProcessors.add(requestLogProcessor);
        return this;
    }

    public AccessLoggerBuilder addResponseProcessor(ResponseLogProcessor responseLogProcessor) {
        this.additionalResponseLogProcessors.add(responseLogProcessor);
        return this;
    }

    public AccessLoggerBuilder loggedRequestHeaders(String... headers) {
        this.includedRequestHeaders.addAll(List.of(headers));
        return this;
    }

    public AccessLoggerBuilder loggedResponseHeaders(String... headers) {
        this.includedResponseHeaders.addAll(List.of(headers));
        return this;
    }

    public AccessLoggerBuilder loggedResponseBodyMediaTypes(Set<String> loggedResponseBodyMediaTypes) {
        this.loggedResponseBodyMediaTypes = loggedResponseBodyMediaTypes;
        return this;
    }

    public AccessLogger build() {
        requestLogProcessors.add(new RequestLineLogProcessor(parameterMaskers));
        requestLogProcessors.add(new RequestHeadersLogProcessor(includedRequestHeaders));
        if (logRequestBody) {
            requestLogProcessors.add(new RequestBodyLengthLogProcessor());
            requestLogProcessors.add(
                    new RequestBodyLogProcessor(
                            parameterMaskers,
                            maxLoggedRequestBodyLength,
                            multipartResolver,
                            bodyMaskers
                    )
            );
        }

        responseLogProcessors.add(new ResponseHeadersLogProcessor(includedResponseHeaders));
        if (logResponseBody) {
            responseLogProcessors.add(new ResponseBodyLogProcessor(maxLoggedResponseBodyLength, loggedResponseBodyMediaTypes, bodyMaskers));
        }

        requestLogProcessors.addAll(additionalRequestLogProcessors);
        responseLogProcessors.addAll(additionalResponseLogProcessors);
        return new AccessLogger(requestLogProcessors, responseLogProcessors);
    }
}
