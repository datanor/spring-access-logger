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

package ee.datanor.spring.logger.access.processor;

import ee.datanor.spring.logger.util.BodyMasker;
import ee.datanor.spring.logger.util.EscapeUtil;
import ee.datanor.spring.logger.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.util.PathMatcher;

import java.util.Map;

public interface LogProcessor {
    String EMPTY_REPLACEMENT = "-";

    default void setMDCValue(String attribute, Object value) {
        MDC.put(attribute, EscapeUtil.escape(replaceEmpty(value)));
    }

    default String replaceEmpty(Object value) {
        String parsedValue = "null".equalsIgnoreCase("" + value) ? EMPTY_REPLACEMENT : "" + value;
        return StringUtils.firstNonBlank(parsedValue, EMPTY_REPLACEMENT);
    }

    default String maskSensitiveBody(HttpServletRequest httpRequest, PathMatcher pathMatcher, Map<String, BodyMasker> maskers, String content) {
        if (content == null) {
            return null;
        }
        String response = content;
        for (Map.Entry<String, BodyMasker> path : maskers.entrySet()) {
            if (pathMatcher.match(path.getKey(), RequestUtil.getRequestPath(httpRequest))) {
                response = path.getValue().mask(content);
            }
        }

        return response;
    }
}
