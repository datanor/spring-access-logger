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
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RequestTimeLogProcessor implements RequestLogProcessor {
    public static final String MDC_KEY = "AL_REQUEST_TIME";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withLocale(Locale.getDefault());

    private DateTimeFormatter dateTimeFormatter;

    public RequestTimeLogProcessor() {
        this(DATE_FORMAT);
    }

    public RequestTimeLogProcessor(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    public void process(HttpServletRequest httpRequest) {
        setMDCValue(MDC_KEY, dateTimeFormatter.format(OffsetDateTime.now()));
    }
}
