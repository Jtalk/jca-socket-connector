# Overview #

This is a JCA 1.6 resource adapter for async networking IO. In opposite to JCA-Sockets project, SocketConnector is JCA-compliant. Current version allows asynchronous TCP operations via Netty IO engine. This project may also be treated as a JCA reference project utilizing all the major parts of the Java Connectors Architecture. This project also includes a sample application which uses this connector.

# Current status #

SocketConnector only supports TCP interactions for now. UDP support is planned but not yet implemented. 

# Application server #

This project is written and tested on WildFly 8 and 9. It includes a deployment descriptor for IronJacamar JCA container as WildFly seems not to support connectors configuration right. By default in IronJacamar-driven environments TCP connection factory will be bound to java:/socket/TCP JNDI path.

# Structure overview #

SocketConnector structure consists of several sub-projects:

* SocketConnectorAPI. This must be deployed separately to provide independent API interfaces and classes for everyone despite of their class loaders. This jar contains API interfaces used by both resource adapter and end-user beans.
* SocketConnectorRAR. This is a resource adapter archive wrapper over the SocketConnectorJAR. This RAR should be deployed to the application server for this connector to work.
* SocketConnectorTesterEAR. This is a sample project and connector tester. If your SocketConnectorRAR instance is deployed right, the Tester must be able to be deployed and must report it's successful connection interaction to the application log. The Tester project is useful when deploying this connector for the first time to make sure everything is all right, there's no need to keep it deployed any time later.

# Deployment #

Here's the JBoss/WildFly deployment process:

* Deploy SocketConnectorAPI to the application server
* Deploy SocketConnectorRAR to the application server.
* TCPConnectionFactory is not accessible through java:/socket/TCP name in default JNDI context.

# Usage #

The main idea behind this connector is to hide non-EJB-compliant Netty IO networking model. Netty is a great networking engine, but Netty 4.x does not support ExecutorService-like thread pools. Since EJBs are not allowed to interact with non-EJB threads and ManagedExecutorService is the only way of thread pooling in managed environment, I needed some kind of a proxy between EJB environment and Netty threading model. 

The answer was JCA. JCA message-driven inbound interaction model allows third-party threads to obtain EJB thread context for awhile (between beforeDelivery and afterDelivery calls on message endpoint) thus allowing us to use Netty-spawned threads to call message-driven beans delivery callbacks. 

There are several steps to make your network application run:

* Define a dependency to me.jtalk.socketconnector.api module in your application manifest. This is mandatory for JBoss/WildFly. Please, refer to your application server vendor's documentation regarding class loading process and cross-deployments interaction. In general, the me.jtalk.socketconnector.api classes need to be available for both RAR and your application's EAR deployments with the same class loader (this latter is mandatory since the equally-named classes loaded by different class loaders are not considered the same by the JVM).
* Create a @MessageDriven bean implementing TCPMessageListener interface from API. This bean should be attached to resource adapter in vendor-specific way (see jboss-ejb3.xml in EJB artifact for JBoss example). You MUST provide a clientId property for each TCPMessageListener. This ID is used for sockets lifetime control: all networking facility (including outbound connections) will be closed after TCPMessageListener is undeployed. 
* User should bind all the startup networking with this connector to TCPMessageListener.initialized callback. This callback is called once all Netty initialization for this clientId is done. Calling to TCPConnection methods before this callback is invoked will lead to ResourceException. TCPMessageListener supports several configuration options which are described in sections below.
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