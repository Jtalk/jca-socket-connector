# Overview #

This is a JCA 1.6 resource adapter for async networking IO. In opposite to JCA-Sockets project, SocketConnector is JCA-compliant. Current version allows asynchronous TCP operations via Netty IO engine. This project may also be treated as a JCA reference project utilizing all the major parts of the Java Connectors Architecture. [SocketConnectorTester](https://bitbucket.org/__jtalk/socketconnectortester) is a sample application using this connector.

# Current status #

SocketConnector now only supports TCP interactions. UDP support is planned but not yet implemented. 

# Application server #

This project is written and tested for WildFly 8.2. It includes a deployment descriptor for IronJacamar JCA container as WildFly seems not to support connectors configuration right. By default in IronJacamar-driven environments TCP connection factory will be bound to java:/socket/TCP JNDI path.

# Structure overview #

SocketConnector structure consists of several sub-projects:

* SocketConnectorAPI. This must be laid to the application server classpath or, in case of JBoss, be installed as module. This jar contains API interfaces used by both resource adapter and end-user EJB's. One should declare this module as a dependency in his project's manifest. API jar also contains @NetAddress validation annotation for activation spec validation.
* SocketConnectorJAR. This is an actual resource adapter implementation. It should be included into RAR and deployed to the application server.
* SocketConnectorRAR. This is a resource adapter archive wrapper upon the SocketConnectorJAR. SocketConnectorRAR.rar should be deployed to the application server for this connector to work.

# Deployment #

Here's the JBoss/WildFly deployment process:

* Create a me.jtalk.socketconnector.api module containing SocketConnectorAPI jar (it's suggested to use bin/jboss-cli interface).
* Deploy SocketConnectorRAR to the application server.
* TCPConnectionFactory is not accessible through java:/socket/TCP name in default JNDI context.

# Usage #

The main idea behind this connector is to hide non-EJB-compliant Netty IO networking model. Netty is a great networking engine, but Netty 4.x does not support ExecutorService-like thread pools. Since EJBs are not allowed to interact with non-EJB threads and ManagedExecutorService is the only way of thread pooling in managed environment, I needed some kind of a proxy between EJB environment and Netty threading model. 

The answer was JCA. JCA message-driven inbound interaction model allows third-party threads to obtain EJB thread context for awhile (between beforeDelivery and afterDelivery calls on message endpoint) thus allowing us to use Netty-spawned threads to call message-driven beans delivery callbacks. 

There are several steps to make your network application run:

* Define a dependency to me.jtalk.socketconnector.api module in your application manifest. This is mandatory for JBoss/WildFly.
* Create a @MessageDriven bean implementing TCPMessageListener interface from API. This bean should be attached to resource adapter in vendor-specific way (org.jboss.ejb3.annotation.ResourceAdapter for JBoss/WildFly). You MUST provide a clientId property for each TCPMessageListener. This ID is used for sockets lifetime control: all networking facility (including outbound connections) will be closed after TCPMessageListener is undeployed. 
* User should bind all the startup networking with this connector to TCPMessageListener.initialized callback. This callback is called once all Netty initialization for this clientId is done. Calling to TCPConnection methods before this callback is invoked will lead to ResourceException. TCPMessageListener supports several configuration options which are described in below sections.
* Obtain TCPConnectionFactory either through JNDI context or through @Resource annotation. This factory can either create a new connection or obtain an existing one. Every network connection is represented by its identifier -- identifier is generated for each createConnection call and can be retreived by using TCPConnection.getId method. Calling to TCPConnectionFactory.getConnection with this connection ID will return TCPConnection pointing to the same underlying socket. Still, TCPConnection instances returned for same ID are not guaranteed to be same (or even equal). You must also specify clientId as described in TCPMessageListener configuration guide above.
* TCPConnection allows you to send byte sequences through the underlying TCP socket. Replies will be delivered through TCPMessageListener. Connection ID can be used to correlate TCPConnection instance with a message delivered through the message-driven bean. 
* Calling TCPConnection.disconnect shuts the underlying socket down. All connection object pointing to that particular socket will be invalidated and will throw ConnectionClosedException on every operation attempt.
* Calling TCPConnection.close will release this connection object for reuse, still, underlying connection is NOT closed. This connector is created for persistent connections handling and is not supposed to be used as single-send object (open/send/disconect sequence).

# TCPMessageListener configuration #

TCPMessageListener can be configured with several options (via @Activation annotation or a deployment descriptor):

* clientId: application server-unique connection pool id. GUID is suggested. You're advised to generate it once and store somewhere in your application code. This is a mandatory parameter.
* keepalive: TCP keepalive flag. Enabled by default.
* localAddress: local IP address to bind to. 0.0.0.0 by default.
* localPort: port to bind to. 0 by default (i.e. random port).
* listnerThreadsCount: number of threads Netty is allowed to use for TCP connections listening. 2 by default.
* receiverThreadsCount: number of threads Netty is allowed to use for regular TCP IO. 4 by default.
* backlog: TCP backlog size. 50 by default.