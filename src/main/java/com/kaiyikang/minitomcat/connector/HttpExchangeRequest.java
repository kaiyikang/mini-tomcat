package com.kaiyikang.minitomcat.connector;

import java.net.URI;
import com.sun.net.httpserver.Headers;

public interface HttpExchangeRequest {
    String getRequestMethod();

    URI getRequestURI();

    Headers getRequestHeaders();

}
