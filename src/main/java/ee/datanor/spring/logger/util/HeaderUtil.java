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

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HeaderUtil {

    private HeaderUtil() {
    }

    public static String headersToString(HttpServletRequest request, Set<String> headersToInclude) {
        return headersToString(getHeadersAsMap(request), headersToInclude);
    }

    public static String headersToString(HttpServletResponse response, Set<String> headersToInclude) {
        return headersToString(getHeadersAsMap(response), headersToInclude);
    }

    public static String maskAuthorizationHeaderValue(String value) {
        if (value.trim().startsWith("Bearer")) {
            int index=value.lastIndexOf('.');
            if (index > 0 && value.chars().filter(ch -> ch == '.').count() > 1) {
                return value.substring(0, index) + ".***";
            } else {
                return value;
            }
        }

        return "***";
    }

    private static Map<String, String> getHeadersAsMap(HttpServletRequest request) {
        return getHeadersAsMap(
                request.getHeaderNames(),
                request::getHeaders
        ).entrySet().stream().peek((e) -> {
            if ("authorization".equalsIgnoreCase(e.getKey()) && StringUtils.hasLength(e.getValue())) {
                e.setValue(maskAuthorizationHeaderValue(e.getValue()));
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, String> getHeadersAsMap(HttpServletResponse response) {
        return getHeadersAsMap(
            Collections.enumeration(response.getHeaderNames()),
            headerName -> Collections.enumeration(response.getHeaders(headerName))
        );
    }

    private static Map<String, String> getHeadersAsMap(Enumeration<String> headerNames,
                                                       Function<String, Enumeration<String>> headerValuesFunction) {
        Map<String, String> result = new LinkedHashMap<>();
        while (headerNames.hasMoreElements()) {
            String h = headerNames.nextElement();
            StringBuilder hv = new StringBuilder();
            Enumeration<String> headerValues = headerValuesFunction.apply(h);
            while (headerValues.hasMoreElements()) {
                if (hv.length() > 0) {
                    hv.append(";");
                }
                hv.append(headerValues.nextElement());
            }
            result.put(h, hv.toString());
        }
        return result;
    }

    private static String headersToString(Map<String, String> headers, Set<String> headersToInclude) {
        TreeMap<String, String> caseInsensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveHeaders.putAll(headers);
        StringBuilder sb = new StringBuilder();
        if (headersToInclude != null) {
            headersToInclude.stream()
                    .filter(caseInsensitiveHeaders::containsKey)
                    .map(key -> findKeyIgnoreCase(caseInsensitiveHeaders, key))
                    .forEach(key -> appendHeader(sb, key, caseInsensitiveHeaders.get(key)));
        } else {
            headers.forEach((key, value) -> appendHeader(sb, key, value));
        }
        return sb.toString();
    }

    private static StringBuilder appendHeader(StringBuilder headerValuesStringBuilder, String key, String value) {
        return headerValuesStringBuilder.append(key).append(": ").append(value).append("\n");
    }

    private static String findKeyIgnoreCase(Map<String, ?> map, String key) {
        return map.keySet().stream().filter(key::equalsIgnoreCase).findAny().orElse(null);
    }

}
