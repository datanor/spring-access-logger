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

package ee.datanor.spring.logger.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RequestUtil {
    private static final String PARAMETER_SEPARATOR = "&";

    private RequestUtil() {
    }

    public static String requestParametersToString(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        return parameterMap.keySet().stream().map(it -> expandParameterValues(it, parameterMap.get(it))).collect(Collectors.joining(PARAMETER_SEPARATOR));
    }

    private static String expandParameterValues(String key, String[] values) {
        return Arrays.stream(values).map(it -> key + "=" + urlEncodeValue(it)).collect(Collectors.joining(PARAMETER_SEPARATOR));
    }

    private static String urlEncodeValue(String value) {
        return URLEncoder.encode(Objects.requireNonNullElse(value, "null"), StandardCharsets.UTF_8);
    }

    public static String getRequestPath(HttpServletRequest request) {
        String url = request.getServletPath();
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            url = StringUtils.hasLength(url) ? url + pathInfo : pathInfo;
        }
        return url;
    }
}
