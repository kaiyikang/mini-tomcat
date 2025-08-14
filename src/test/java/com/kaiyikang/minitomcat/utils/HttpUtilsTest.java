package com.kaiyikang.minitomcat.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.Headers;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class HttpUtilsTest {

    @Mock
    private Headers mockHeaders;

    @Nested
    @DisplayName("")
    class Header {

        @Test
        @DisplayName("When the header exists and has only one value, that value should be returned.")
        void getHeader_WhenHeaderExistsWithSingleValue_ShouldReturnTheValue() {
            // Given
            String headerName = "Content-Type";
            String expectedValue = "application/json";
            when(mockHeaders.get(headerName)).thenReturn(List.of(expectedValue));

            // When
            String actualValue = HttpUtils.getHeader(mockHeaders, headerName);

            // Then
            assertEquals(actualValue, expectedValue);
        }

        @Test
        @DisplayName("When the header exists and has multiple values, only the first value should be returned.")
        void getHeader_WhenHeaderExistsWithMultipleValues_ShouldReturnFirstValue() {
            // Given
            String headerName = "Accept-Encoding";
            when(mockHeaders.get(headerName)).thenReturn(List.of("gzip", "deflate", "br"));

            // When
            String actualValue = HttpUtils.getHeader(mockHeaders, headerName);

            // Then
            assertEquals(actualValue, "gzip");

        }

        @Test
        @DisplayName("When the header does not exist, null should be returned.")
        void getHeader_WhenHeaderDoesNotExist_ShouldReturnNull() {
            // Given
            String headerName = "X-Non-Existent-Header";
            when(mockHeaders.get(headerName)).thenReturn(null);

            // When
            String actualValue = HttpUtils.getHeader(mockHeaders, headerName);

            // Then
            assertThat(actualValue).isNull();
        }

        @Test
        @DisplayName("When the header exists but its value list is empty, null should be returned.")
        void getHeader_WhenHeaderExistsButValueListIsEmpty_ShouldReturnNull() {
            // Given
            String headerName = "X-Empty-Header";
            when(mockHeaders.get(headerName)).thenReturn(Collections.emptyList());

            // When
            String actualValue = HttpUtils.getHeader(mockHeaders, headerName);

            // Then
            assertThat(actualValue).isNull();
        }
    }

    @Nested
    @DisplayName("parseQuery Core Functionalities")
    class ParseQueryCoreFunctionality {

        private static final Charset CHARSET = StandardCharsets.UTF_8;

        @Test
        @DisplayName("to parse standard query strings and handle multiple values and URL decoding")
        void shouldParseStandardQueryWithMultipleValuesAndUrlEncoding() {
            String query = "name=John%20Doe&topic=java&topic=spring&city=NewYork";
            Map<String, List<String>> result = HttpUtils.parseQuery(query, CHARSET);

            assertThat(result)
                    .hasSize(3)
                    .containsEntry("name", List.of("John Doe"))
                    .containsEntry("topic", List.of("java", "spring"))
                    .containsEntry("city", List.of("NewYork"));
        }

        @Test
        @DisplayName("When the query string is null or empty, an empty Map should be returned.")
        void shouldReturnEmptyMapForNullOrEmptyQuery() {
            Map<String, List<String>> resultForNull = HttpUtils.parseQuery(null, CHARSET);
            assertThat(resultForNull).isNotNull().isEmpty();

            Map<String, List<String>> resultForEmpty = HttpUtils.parseQuery("", CHARSET);
            assertThat(resultForEmpty).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Ignore parameters with incorrect formats, such as those with only keys or only values.")
        void shouldIgnoreMalformedParameters() {
            // Given
            String query = "valid=true&malformedKey&_another=value&=malformedValue";

            // When
            Map<String, List<String>> result = HttpUtils.parseQuery(query, CHARSET);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .containsEntry("valid", List.of("true"))
                    .containsEntry("_another", List.of("value"))
                    .doesNotContainKey("malformedKey")
                    .doesNotContainKey("");
        }

        @Test
        @DisplayName("Overloaded methods without a character set should default to UTF-8.")
        void convenienceMethodShouldDefaultToUtf8() {
            // %E4%BD%A0%E5%A5%BD is "你好" in UTF-8
            String query = "greeting=%E4%BD%A0%E5%A5%BD";
            Map<String, List<String>> result = HttpUtils.parseQuery(query);

            assertThat(result)
                    .hasSize(1)
                    .containsEntry("greeting", List.of("你好"));
        }
    }
}
