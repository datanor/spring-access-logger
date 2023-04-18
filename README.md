# Spring access logger

Access logger for Spring Boot.

## Getting started

**Add dependency**

```
dependencies {
    implementation 'ee.datanor.spring.logger:spring-access-logger:1.0.0'
}
```

**Configure logger**

```
@Bean
    public AccessLogger accessLogger(MultipartResolver multipartResolver) {
        return AccessLogger.builder()
                .logRequestBody(multipartResolver)
                .logResponseBody()
                .loggedRequestHeaders("referer", "user-agent", "content-type", "accept", "accept-language", "content-length", "transfer-encoding")
                .loggedResponseHeaders("content-type", "content-length")
                .maxRequestBodyLength(2048)
                .maxResponseBodyLength(2048)
                .loggedResponseBodyMediaTypes(Set.of("json", "xml"))
                .build();
    }

    @Bean
    public FilterRegistrationBean<AccessLoggingFilter> loggingFilter(AccessLogger accessLogger) {
        FilterRegistrationBean<AccessLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        AccessLoggingFilter accessLoggingFilter = new AccessLoggingFilter(accessLogger);
        registrationBean.setFilter(accessLoggingFilter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
```

**Log4j2 xml configuration example**
```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">

    <Properties>
        <Property name="access-request-log-pattern">
            REQ\t%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}\t%X{AL_REQUEST_HASH}\t%X{AL_SERVER_NAME}:%X{AL_SERVER_PORT}\t%X{AL_CLIENT_IP}\t-\t%X{AL_REQUEST_LINE}\t%X{AL_REQUEST_HEADERS}\t%X{AL_REQUEST_BODY_LENGTH}\t%replace{%X{AL_REQUEST_BODY}}{^$}{-}%n
        </Property>
        <Property name="access-response-log-pattern">
            RES\t%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}\t%X{AL_REQUEST_LINE}\t%X{AL_PROCESSING_TIME}ms\t%X{AL_RESPONSE_STATUS}\t%X{AL_RESPONSE_HEADERS}\t%X{AL_RESPONSE_BODY_LENGTH}\t%X{AL_RESPONSE_BODY}%n
        </Property>

        ...

    </Properties>

    <Appenders>
        <Console name="access-request-logger-console" target="SYSTEM_OUT">
            <PatternLayout pattern="${access-request-log-pattern}"/>
        </Console>

        <Console name="access-response-logger-console" target="SYSTEM_OUT">
            <PatternLayout pattern="${access-response-log-pattern}"/>
        </Console>

        ...

    </Appenders>

    <Loggers>

        <Logger name="access-request-log" additivity="false" level="DEBUG">
            <AppenderRef ref="access-request-logger-console"/>
        </Logger>

        <Logger name="access-response-log" additivity="false" level="DEBUG">
            <AppenderRef ref="access-response-logger-console"/>
        </Logger>

        ...

    </Loggers>

</Configuration>


```
