package com.kaiyikang.minitomcat;

import java.util.Map;

public record Config(Server server) {
    public record Server(
            String host,
            int port,
            int backlog,
            String requestEncoding,
            String responseEncoding,
            String name,
            String mimeDefault,
            int threadPoolSize,
            boolean enableVirtualThread,
            Map<String, String> mimeTypes,
            WebApp webApp,
            ForwardedHeaders forwardedHeaders) {

        public String getMimeType(String url) {
            if (url == null) {
                return this.mimeDefault;
            }
            int n = url.lastIndexOf('.');
            if (n < 0) {
                return this.mimeDefault;
            }
            String ext = url.substring(n).toLowerCase();
            return Map.copyOf(this.mimeTypes).getOrDefault(ext, this.mimeDefault);
        }
    }

    public record WebApp(
            String name,
            boolean fileListings,
            String virtualServerName,
            String sessionCookieName,
            Integer sessionTimeout) {
    }

    public record ForwardedHeaders(
            String forwardedProto,
            String forwardedHost,
            String forwardedFor) {
    }

}