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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class ResponseStatusLogProcessor implements ResponseLogProcessor {
    public static final String MDC_KEY = "AL_RESPONSE_STATUS";

    @Override
    public void process(HttpServletRequest httpRequest, ContentCachingResponseWrapper responseWrapper, boolean isASync) {
        setMDCValue(MDC_KEY, responseWrapper.getStatus());
    }
}
