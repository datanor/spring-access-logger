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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.util.ContentCachingRequestWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnrestrictedContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    private ByteArrayOutputStream cachedBytes;

    public UnrestrictedContentCachingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null) cacheInputStream();
        return new CachedServletInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String charset = getRequest().getCharacterEncoding();
        if (charset == null) {
            charset = "UTF-8";
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharset(getRequest())));
    }

    private void cacheInputStream() throws IOException {
        InputStream is = super.getInputStream();
        byte[] bytes = is.readAllBytes();
        cachedBytes = new ByteArrayOutputStream();
        cachedBytes.writeBytes(bytes);
    }

    public String getContentAsString() throws IOException {
        if (cachedBytes == null) cacheInputStream();
        return new String(cachedBytes.toByteArray(), getCharset(getRequest()));
    }

    public class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream inputStream;

        public CachedServletInputStream() {
            inputStream = new ByteArrayInputStream(cachedBytes.toByteArray());
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }

    private String getCharset(ServletRequest servletRequest) {
        String charset = servletRequest.getCharacterEncoding();
        return charset != null ? charset : StandardCharsets.UTF_8.name();
    }
}
