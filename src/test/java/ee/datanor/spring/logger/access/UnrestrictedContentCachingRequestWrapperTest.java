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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.DelegatingServletInputStream;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UnrestrictedContentCachingRequestWrapperTest {

    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private UnrestrictedContentCachingRequestWrapper unrestrictedContentCachingRequestWrapper;

    @BeforeEach
    void setUp() throws IOException {
        InputStream source = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        doReturn(new DelegatingServletInputStream(source)).when(request).getInputStream();
    }

    @Test
    void shouldAllowToReadInputStreamMultipleTimes() throws IOException {
        // when
        String body1 = IOUtils.toString(unrestrictedContentCachingRequestWrapper.getInputStream(), StandardCharsets.UTF_8.name());
        String body2 = IOUtils.toString(unrestrictedContentCachingRequestWrapper.getInputStream(), StandardCharsets.UTF_8.name());

        // then
        assertNotNull(body1);
        assertNotNull(body2);
    }

    @Test
    void shouldReturnContentAsStringWhenInputStreamIsAlreadyRead() throws IOException {
        // given
        IOUtils.toString(unrestrictedContentCachingRequestWrapper.getInputStream(), StandardCharsets.UTF_8.name());

        // when
        String content = unrestrictedContentCachingRequestWrapper.getContentAsString();

        // then
        assertEquals("test", content);
    }

    @Test
    void shouldReturnContentAsStringWhenInputStreamIsNotYetRead() throws IOException {
        // when
        String content = unrestrictedContentCachingRequestWrapper.getContentAsString();

        // then
        assertEquals("test", content);
    }

    @Test
    void shouldAllowToAccessReaderMultipleTimes() throws IOException {
        // when
        String body1 = IOUtils.toString(unrestrictedContentCachingRequestWrapper.getReader());
        String body2 = IOUtils.toString(unrestrictedContentCachingRequestWrapper.getReader());

        // then
        assertNotNull(body1);
        assertNotNull(body2);
    }

    @Test
    void inputStreamShouldNotBeFinished() throws IOException {
        // given
        UnrestrictedContentCachingRequestWrapper.CachedServletInputStream isToRead =
                (UnrestrictedContentCachingRequestWrapper.CachedServletInputStream) unrestrictedContentCachingRequestWrapper.getInputStream();
        isToRead.readNBytes(1);

        // when
        boolean result = isToRead.isFinished();

        // then
        assertFalse(result);
    }

    @Test
    void inputStreamShouldBeFinished() throws IOException {
        // given
        UnrestrictedContentCachingRequestWrapper.CachedServletInputStream isToRead =
                (UnrestrictedContentCachingRequestWrapper.CachedServletInputStream) unrestrictedContentCachingRequestWrapper.getInputStream();
        IOUtils.toString(isToRead, StandardCharsets.UTF_8.name());

        // when
        boolean result = isToRead.isFinished();

        // then
        assertTrue(result);
    }

    @Test
    void shouldBeReady() throws IOException {
        // given
        UnrestrictedContentCachingRequestWrapper.CachedServletInputStream isToRead =
                (UnrestrictedContentCachingRequestWrapper.CachedServletInputStream) unrestrictedContentCachingRequestWrapper.getInputStream();

        // when
        boolean result = isToRead.isReady();

        // then
        assertTrue(result);
    }

    @Test
    void shouldNotAllowToSetReadListener() throws IOException {
        // given
        UnrestrictedContentCachingRequestWrapper.CachedServletInputStream isToRead =
                (UnrestrictedContentCachingRequestWrapper.CachedServletInputStream) unrestrictedContentCachingRequestWrapper.getInputStream();

        // when
        Executable executable = () -> isToRead.setReadListener(null);

        // then
        assertThrows(IllegalStateException.class, executable);
    }
}