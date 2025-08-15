package com.kaiyikang.minitomcat.engine.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kaiyikang.minitomcat.connector.HttpExchangeRequest;
import com.kaiyikang.minitomcat.utils.HttpUtils;

public class Parameters {
    Map<String, String[]> parameters;
    final HttpExchangeRequest exchangeRequest;
    Charset charset;

    public Parameters(HttpExchangeRequest exchangeRequest, String charSet) {
        this.exchangeRequest = exchangeRequest;
        this.charset = Charset.forName(charSet);
    }

    public void setCharSet(String charSet) {
        this.charset = Charset.forName(charSet);
    }

    public String getParameter(String name) {
        String[] values = getParameterValues(name);
        if (values == null) {
            return null;
        }
        return values[0];
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    public Map<String, String[]> getParameterMap() {
        if (this.parameters == null) {
            this.parameters = initParameters();
        }
        return this.parameters;
    }

    Map<String, String[]> initParameters() {
        Map<String, List<String>> params = new HashMap<>();
        String query = this.exchangeRequest.getRequestURI().getRawQuery();
        if (query != null) {
            params = HttpUtils.parseQuery(query, charset);
        }
        if ("POST".equals(this.exchangeRequest.getRequestMethod())) {
            String value = HttpUtils.getHeader(this.exchangeRequest.getRequestHeaders(), "Content-Type");
            if (value != null && value.startsWith("application/x-www-form-urlencoded")) {
                // Load request body
                String requestBody;
                try {
                    requestBody = new String(this.exchangeRequest.getRequestBody(), charset);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                Map<String, List<String>> postParams = HttpUtils.parseQuery(requestBody, charset);
                for (String key : postParams.keySet()) {
                    List<String> queryVal = params.get(key);
                    List<String> postVal = postParams.get(key);
                    // If no query value, use post value
                    if (queryVal == null) {
                        params.put(key, postVal);
                    } else {
                        // otherwise add post value into query value
                        queryVal.addAll(postVal);
                    }
                }
            }
        }

        Map<String, String[]> paramsMap = new HashMap<>();
        for (String key : params.keySet()) {
            List<String> values = params.get(key);
            paramsMap.put(key, values.toArray(String[]::new));
        }
        return paramsMap;
    }

}
