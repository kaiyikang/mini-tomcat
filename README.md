# Mini Tomcat

[Resource](https://liaoxuefeng.com/books/jerrymouse/introduction/index.html)

All diagram come from this tutorial resource.

## Targeted Features

- Support most features of Servlet 6
  - Servlet Components
  - Filter Components
  - Listener Components
  - Session (Cookie only)
  - Does not support JSP
  - Does not support Async and WebSocket.
- Deployable standard Web App
- Does not support deploying multiple Web Apps at the same time
- Does not support hot deployment

## To Be Supplemented

WebSocket: [Resource](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)

## Architecture

```
  ┌─────────────────────────────────────────────────────┐
  │                    Tomcat Server                    │
  │ ┌─────────────────────────────────────────────────┐ │
  │ │                     Service                     │ │
  │ │                 ┌─────────────────────────────┐ │ │
  │ │                 │           Engine            │ │ │
  │ │                 │ ┌─────────────────────┐     │ │ │
  │ │                 │ │        Host         │     │ │ │
  │ │ ┌───────────┐   │ │ ┌───────────────────┴─┐   │ │ │
  │ │ │Connectors │   │ │ │        Host         │   │ │ │
  │ │ │┌─────────┐│   │ │ │ ┌───────────────────┴─┐ │ │ │
  │ │ ││Connector││   │ │ │ │        Host         │ │ │ │
  │ │ │└─────────┘│   │ │ │ │ ┌─────────────┐     │ │ │ │
  │ │ │┌─────────┐│   │ │ │ │ │   Context   │     │ │ │ │
◀─┼─┼▶││Connector││◀─▶│ │ │ │ │ ┌───────────┴─┐   │ │ │ │
  │ │ │└─────────┘│   │ │ │ │ │ │   Context   │   │ │ │ │
  │ │ │┌─────────┐│   │ │ │ │ │ │ ┌───────────┴─┐ │ │ │ │
  │ │ ││Connector││   │ │ │ │ └─┤ │   Context   │ │ │ │ │
  │ │ │└─────────┘│   │ └─┤ │   │ │ ┌─────────┐ │ │ │ │ │
  │ │ └───────────┘   │   │ │   └─┤ │ Web App │ │ │ │ │ │
  │ │                 │   └─┤     │ └─────────┘ │ │ │ │ │
  │ │                 │     │     └─────────────┘ │ │ │ │
  │ │                 │     └─────────────────────┘ │ │ │
  │ │                 └─────────────────────────────┘ │ │
  │ └─────────────────────────────────────────────────┘ │
  └─────────────────────────────────────────────────────┘

```

It contains one or multiple services, and it normally has one. The service has two components:

- Connectors: single or multiple connectors are defined, e.g., two connectors for HTTP and HTTPS.
- Engine: all requests are passed to Engine after Connector.

One Engine contains multiple Host, and in the Host, there are few Contexts which is corresponding one Web App.

An HTTP request to the host: `www.example.com/abc`:

- `www.example.com`: one host
- `\`: the root of the web app
- `\abc`: The prefix is located in one Context

But for Mini-Tomcat, we simplify it to:

- Only one HTTP Connector
- Does not support HTTPS
- Only supports mounting to a single Context
- Does not support multiple Hosts and multiple Contexts

```
  ┌───────────────────────────────┐
  │       minitomcat Server       │
  │                 ┌───────────┐ │
  │  ┌─────────┐    │  Context  │ │
  │  │  HTTP   │    │┌─────────┐│ │
◀─┼─▶│Connector│◀──▶││ Web App ││ │
  │  └─────────┘    │└─────────┘│ │
  │                 └───────────┘ │
  └───────────────────────────────┘

```

We can run multiple Servers instead of multiple Apps. Meanwhile, Nginx can replace the HTTPS feature of the Connector, so that we can focus on the essential features of Mini-Tomcat:

```
               ┌───────────────────────────────┐
               │       mini-tomcat Server      │
               │ ┌─────────────────────────────┴─┐
               │ │       mini-tomcat Server      │
    ┌───────┐  │ │ ┌─────────────────────────────┴─┐
    │       │◀─┼─│ │       mini-tomcat Server      │
    │       │  │ │ │                 ┌───────────┐ │
◀──▶│ Nginx │◀─┼─┼─│  ┌─────────┐    │  Context  │ │
    │       │  └─┤ │  │  HTTP   │    │┌─────────┐│ │
    │       │◀───┼─┼─▶│Connector│◀──▶││ Web App ││ │
    └───────┘    └─┤  └─────────┘    │└─────────┘│ │
                   │                 └───────────┘ │
                   └───────────────────────────────┘

```

## Servlet

Java Web applications are always packaged into `.war` files, which can be loaded by Tomcat, Jetty, and other Web Servers, and are compiled according to the Java EE (Jakarta EE) specification.

```
  ┌─────────────────┐
  │     Web App     │
  └─────────────────┘
           ▲
           │
           ▼
  ┌─────────────────┐
┌─┤Servlet Interface├─┐
│ └─────────────────┘ │
│          ▲          │
│          │          │
│          ▼          │
│ ┌─────────────────┐ │
│ │     Servlet     │ │
│ │ Implementation  │ │
│ └─────────────────┘ │
│       Server        │
└─────────────────────┘
```

### Servlet Server

The Servlet Server is based on the `HttpServletRequest` and `HttpServletResponse` interface to handle HTTP. We can use the [Adapter Pattern](https://www.baeldung.com/java-adapter-pattern) to adapt the HttpExchange for a new one.

Now that we have our `HttpExchangeAdapter`, we can build `HttpServletRequestImpl` and `HttpServletResponseImpl` to handle requests and responses.

The `HttpServletRequestImpl` and `HttpServletResponseImpl` will both use the `HttpExchange` instance which was adapted by `HttpExchangeAdapter`. This lets them easily access and work with the request and response data.

```
                      ┌───────────────┐
         ┌────────────┼ HttpConnector ┼─────────┐
         │            └──────┬────────┘         │
         │                   │                  │
┌────────▼───────────┐       │        ┌─────────▼───────────┐
│ HttpServletRequest │       │        │ HttpServletResponse │
└────────┬───────────┘       │        └─────────┬───────────┘
         │                   │                  │
┌────────▼────────────┐      │       ┌──────────▼───────────┐
│ HttpExchangeRequest │      │       │ HttpExchangeResponse │
└────────┬────────────┘      │       └──────────┬───────────┘
         │                   │                  │
         │        ┌──────────▼──────────┐       │
         └───────►│ HttpExchangeAdapter │◄──────┘
                  └─────────▲───────────┘
                            │
                     ┌──────┼───────┐
                     │ HttpExchange │
                     └──────────────┘
```

Following the conversion of our `HttpExchange` to standard `ServletRequest` and `ServletResponse` objects, we now invoke the `process(req, resp)` method to handle the incoming request.

This core `process(req, resp)` method is ideally situated within the `ServletContext`. As defined by the Servlet specification, the `ServletContext` provides a central point for managing key components of a web application, including `Servlets`, `Filters`, and `Listeners`, etc.

### Servlet Context

最初的请求，首先由`HttpConnector`接收，在 `constructor` 中，我们加载了已经提前定义好的`servletClass`。随后，我们初始化核心的`ServletContextImpl`并且传递`servletClass`。

随后我们启动服务器，通过提前定义好的`handle`函数，我们将拆除经过封装的请求和返回`HttpExchange`，拆除后的请求是满足`HttpServletRequest`接口的`HttpServletRequestImpl`，以及满足`HttpServletResponse`接口的`HttpServletResponseImpl`。转换的过程是通过`HttpExchangeAdapter` 完成的。

完成转换后，我们将标准的请求和返回给到`servletContext.process`方法。

初始化context的时候，即调用`ServletContextImpl.initialize`时，我们应加载并初始化定义好的servletClass。当加载的时候，额外的步骤是引入`ServletRegistration.Dynamic`，一个`registeration` 对应着一个`servlet`。原因是，它可以摆脱web.xml的配置，实现了运行时动态修改Servlet配置的能力。

初始化已经加载的servletClass，也需要`registeration`的参与，并且我们还需要额外维护一个`servletMappings`的字典，保存好`urlPattern` 和对应的`servlet`,前者应该在每一个自定义的servlet前定义，类似于`@WebServlet(urlPatterns = "/hello")`,这样当用户输入网址的时候，minitomcat可以根据pattern选择特定的规则进行匹配（这里使用的是哪个长度短，选择哪个）。

`Context`中的执行步骤逻辑较为简单，使用`servletMappings`获得匹配的servlet，然后调用`servlet.service(request, response)`以处理请求和返回。

## Milestone

1. SimpleHttpServer done
2. ServletServer 2025.08.08 done
3. Servlet Module
   1. Servlet Context 2025.08.11 done
