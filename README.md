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

#### Request Handling and Server Initialization

The initial request is first received by the `HttpConnector`. Within its constructor, we load a predefined list of servlet classes (`servletClass`). Subsequently, we initialize the core `ServletContextImpl` and pass this list of servlet classes to it.

Next, the server is started. Incoming requests are processed by a predefined `handle` method, which unwraps the `HttpExchange` object provided by the underlying HTTP server. This unwrapping process yields a request object, `HttpServletRequestImpl`, which implements the standard `HttpServletRequest` interface, and a response object, `HttpServletResponseImpl`, which implements the `HttpServletResponse` interface. This conversion is managed by the `HttpExchangeAdapter`.

Once the conversion is complete, the standard request and response objects are passed to the `servletContext.process()` method for further handling.

```
                     ┌────────────────────┐
                     │   ServletContext   │
                     ├────────────────────┤
                     │     ┌────────────┐ │
    ┌─────────────┐  │ ┌──▶│IndexServlet│ │
───▶│HttpConnector│──┼─┤   ├────────────┤ │
    └─────────────┘  │ └──▶│HelloServlet│ │
                     │     └────────────┘ │
                     └────────────────────┘
```

#### Servlet Initialization and Dynamic Registration

During the context's initialization phase, specifically when `ServletContextImpl.initialize()` is called, we load and instantiate the predefined servlet classes. A critical step in this process is the use of `ServletRegistration.Dynamic`. Each servlet is associated with a corresponding `ServletRegistration` instance. The primary advantage of this approach is that it eliminates the need for a `web.xml` deployment descriptor, enabling the ability to dynamically configure servlets at runtime.

The `ServletRegistration` is also involved in the initialization of each loaded servlet class. Additionally, we maintain a `servletMappings` map, which stores the relationship between a `urlPattern` and its corresponding servlet instance. The URL pattern for each servlet should be defined using an annotation, such as `@WebServlet(urlPatterns = "/hello")`, on the custom servlet class. This allows the Mini-Tomcat server to select the appropriate servlet by matching the request's URL against these patterns. The matching logic here prioritizes shorter patterns (e.g., the servlet with the shorter matching URL pattern is selected).

#### Request Processing within the Context

The execution logic within the `Context` is straightforward. It uses the `servletMappings` to find the servlet that matches the incoming request's URL. Once the appropriate servlet is identified, its `service(request, response)` method is invoked to process the request and generate the response.

## Milestone

1. SimpleHttpServer done
2. ServletServer 2025.08.08 done
3. Servlet Module
   1. Servlet Context 2025.08.11 done
