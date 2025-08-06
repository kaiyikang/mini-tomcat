package com.kaiyikang.minitomcat.connector;

import java.net.URI;

public interface HttpExchangeRequest {
    String getRequestMethod();

    URI getRequestURI();
}
