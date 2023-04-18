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
import ee.datanor.spring.logger.util.ParameterMasker;
import ee.datanor.spring.logger.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Map;

@RequiredArgsConstructor
public abstract class ParameterMaskingRequestLogProcessor implements RequestLogProcessor {
    private final Map<String, ParameterMasker> maskers;
    protected final PathMatcher pathMatcher = new AntPathMatcher();

    protected String maskSensitiveParameters(HttpServletRequest httpRequest, String content) {
        if (content == null) {
            return null;
        }
        String response = content;
        for (Map.Entry<String, ParameterMasker> path : maskers.entrySet()) {
            if (pathMatcher.match(path.getKey(), RequestUtil.getRequestPath(httpRequest))) {
                response = path.getValue().mask(content);
            }
        }

        return response;
    }
}
