# Mini Tomcat

[Resource](https://liaoxuefeng.com/books/jerrymouse/introduction/index.html)

All diagram come from this tutorial resource.

## What is the main objective of this mini Tomcat?

The primary objective of this mini-Tomcat is to deconstruct how a popular web server operates, enabling me to apply this foundational knowledge to other web servers and similar projects. At the same time, I aim to gain a solid grasp of core concepts like Servlets or Filters. Furthermore, implementing the specific details provides and excellent opportunity to enhance my practical Java skills and learn advanced techniques or patterns that are often missed in typical self-directed or AI-driven coding.

## Targeted Features

- Support most features of Servlet 6
  - ✅ Servlet Components
  - ✅ Filter Components
  - ✅ Listener Components
  - ✅ Session (Cookie only)
  - ❌ JSP
  - ❌ Async and WebSocket.
- ✅ Deployable standard Web App
- ❌ Deploying multiple Web Apps at the same time
- ❌ Hot deployment

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

## FilterChain

```
  ┌─────────────────┐
  │ ServletContext  │
  │ ┌ ─ ─ ─ ─ ─ ─ ┐ │
  │   FilterChain   │
  │ │ ┌─────────┐ │ │
──┼──▶│ Filter  │   │
  │ │ └─────────┘ │ │
  │        │        │
  │ │      ▼      │ │
  │   ┌─────────┐   │
  │ │ │ Filter  │ │ │
  │   └─────────┘   │
  │ │      │      │ │
  │        ▼        │
  │ │ ┌─────────┐ │ │
  │   │ Filter  │   │
  │ │ └─────────┘ │ │
  │  ─ ─ ─ ┬ ─ ─ ─  │
  │        ▼        │
  │   ┌─────────┐   │
  │   │ Servlet │   │
  │   └─────────┘   │
  └─────────────────┘
```

The purpose of a `Filter` is to pre-process an HTTP request _before_ it reaches the final `servlet.service(request, response)`. This is useful for tasks like logging or performing authentication checks.

This system uses the **Chain of Responsibility** design pattern. It gives multiple Filters a chance to handle the request in sequence before it reaches its final destination.

Here's a simple analogy to understand how it works:

Imagine the `FilterChainImpl` is a **stairwell** in a building, and each `Filter` is a **floor**.

The stairwell always remembers which floor you are on (using its `index` variable). On each floor, the `Filter` does its job. When it's finished, it has two choices:

1.  **Proceed to the next floor** by going back into the stairwell (by calling `chain.doFilter(request, response)`).
2.  **Exit the building immediately** (e.g., by sending an error like `resp.sendError(403, "Forbidden")`).

When you choose to proceed, the stairwell (`FilterChainImpl`) automatically takes you to the next floor in the sequence (`filters[current].doFilter(...)`). This process repeats until you've passed all the floors and finally arrive at your destination on the ground floor: the `servlet.service(...)` method.

## HttpSession

Client is responsible for storing cookies, while the server is responsible for storing sessions. These two are matched via a `sessionId`.

When a request comes in, `HttpServletRequestImpl` needs to be initialized with `servletContext` and `response` as parameters.

Specifically, within `HttpServletRequestImpl`, it first checks the client's cookies for a value associated with the key `JSESSIONID`. If found, it retrieves the corresponding session via the `sessionManager` contained within the `servletContext`. Concurrently, the `sessionId` is placed in the `response` header, effectively instructing the client to store this `sessionId` in its cookies for future requests.

In the `sessionManager`, since all requests share a single `sessions` map, `ConcurrentHashMap` is used to prevent multithreading issues. Additionally, a new thread is started within this class to continuously monitor session validity. If a session is found to be invalid or expired, it is immediately cleaned up.

In `HttpServletResponseImpl`, additional logic needs to be implemented for handling cookies, redirects, and the `commit` process. It's crucial to understand that once the response is committed (via `commitHeaders(0);`), it's like a shipping label that has already been sent – the headers can no longer be modified. Following the sending process, only after the 'shipping label' (headers) has been sent can the actual content be transmitted, i.e., by writing data using `getOutputStream` or `getWriter`.

Finally, the specific business logic can be added within the servlets themselves. For instance, for an `index` servlet, if no session is found, the login page would be displayed. A `login` servlet, on the other hand, would check and compare credentials against a database; if successful, it would update the session, otherwise, it would return an error page indicating a failed login.

## Listener

Listeners are used to monitor events that occur within a web application. For instance, when the server initializes the `ServletContext`, an event is triggered. A listener that implements the corresponding interface can then handle this event. The handling logic is customizable and can be used for tasks like data encoding/decoding or loading configuration files.

This mechanism is based on the [Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern). When an event occurs, all registered listeners are notified and execute their corresponding operations.

The Servlet specification defines various Listener interfaces, each designed to monitor different types of events at different points in the application lifecycle.

The implementation process is as follows:

First, a Listener that implements an interface like `ServletContextAttributeListener` is created. It is then registered in `ServletContextImpl` via the `addListener()` method. Inside this method, a series of `if-else` statements determine which interface the listener implements and store it in the appropriate categorized list of listeners.

Next, a trigger function is implemented in `ServletContextImpl`. This function takes an event object as input, iterates over the relevant list of listeners, and invokes each one, thereby executing the trigger logic.

Finally, this trigger function is called from the methods that are meant to fire the event, such as `setAttribute()` or `removeAttribute()`.

In summary, specific server-side logic triggers a notification function. This function, in turn, invokes the pre-registered listeners, effectively broadcasting the event to all interested parties.

## Web App

According to the specifications, components such as Servlets, Filters, and Listeners should all be packaged into a WAR (Web Application Archive) file. The Web server then loads this file. A typical example of this specification is as follows:

```
hello-webapp
├── WEB-INF
│   ├── classes
│   │   └── com
│   │       └── example
│   │           ├── filter
│   │           │   └── LogFilter.class
│   │           ├── listener
│   │           │   ├── HelloHttpSessionListener.class
│   │           │   └── HelloServletContextAttributeListener.class
│   │           ├── servlet
│   │           │   ├── HelloServlet.class
│   │           │   └── IndexServlet.class
│   │           └── util
│   │               └── DateUtil.class
│   └── lib
│       ├── logback-classic-1.4.6.jar
│       ├── logback-core-1.4.6.jar
│       └── slf4j-api-2.0.4.jar
├── contact.html
└── favicon.ico
```

We need to configure essential information in a YAML file, such as the host and necessary web application details. During the `Start` phase, this YAML configuration is mapped into a `Config` POJO. Subsequently, the program scans the WAR file and loads all class files. Through annotations, it distinguishes whether a class is a Filter, a Listener, or a Servlet.

All loaded class files are then provided to `ServletContextImpl` for initialization. It's crucial to note that all class loading should be based on the WAR file, not through other means. Therefore, it's necessary to explicitly set `Thread.currentThread().setContextClassLoader(this.classLoader);` to avoid issues related to the [Parent Delegation Mechanism](https://stackoverflow.com/questions/2642606/java-classloader-delegation-model).

## Milestone

1. SimpleHttpServer done
2. ServletServer 2025.08.08 done
3. Servlet Module
   3.1. Servlet Context 2025.08.11 done
   3.2. FilterChain 2025.08.14 done
   3.3. HttpSession 2025.08.18 done
   3.4. Listener 2025.08.23 done
4. WebApp 2025.08.30 done
