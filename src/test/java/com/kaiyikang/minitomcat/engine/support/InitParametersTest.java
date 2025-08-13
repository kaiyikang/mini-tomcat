package com.kaiyikang.minitomcat.engine.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

public class InitParametersTest {
    private InitParameters params;

    @BeforeEach
    void setUp() {
        this.params = new InitParameters();
    }

    @Nested
    @DisplayName("setInitParameter(name, value)")
    class setInitParameterTests {

    }
}
