# An Improved RMI Tutorial with Eclipse

## Introduction
 There are/have-been heaps of remoting frameworks in java, but RMI being part of the JRE/JDK and therefore having no external dependencies is my personal preference for remoting with java. Its main drawback: the wire protocol is not web-friendly and therefore difficult to go through firewalls (although it is possible). But if used behind the firewall, it makes for an excellent way to do distributed computing using only the JDK (OMG! only the JDK?! no Spring? or JMS?!). It does have several killer features that are found in very few (if any) remoting frameworks: callbacks and remote classloading. In this tutorial, you will see remote classloading. Ever since JRE 5.0, you don’t need to compile stubs (meaning you don’t need an extra compile-time step to get RMI working). I am guessing someone decided to do away with those and use the jdk dynamic proxies.

I wrote this tutorial after going through the most excellent [RMI Tutorial by SUN/Oracle](https://docs.oracle.com/javase/tutorial/rmi/index.html). This tutorial has some aspects that complicate things:

* (a soft) requirement of a webserver to download some code (wtf?! I just want to get RMI working. why do i need a webserver?)
* Defining and using security policy files that point to explicit paths (I just want to get RMI working)
* Compiling stuff into 3 jars (with no build files provided this makes stuff very tedious) and having a big-ass command to run on the command line.

I will still recommend that you read the concepts presented in RMI tutorial before attempting this one since I will only present the same code, with some instructions on how to get it running within eclipse.

And finally, to simplify things, we will use only eclipse to get this tutorial working. All eclipse projects (including sources) are attached at the end of this tutorial. I will also say that not all the code is written by me. Some is taken directly from Sun/Oracle’s RMI tutorial. Lets get started!

## Eclipse Project Setups

The example application has 3 components:

1. Some interfaces
2. A Server component that implements these interfaces
3. A Client components that uses these interfaces

![eclipse projects](/screenshot_002.png)

And so we will create 3 eclipse projects: rmi-base, rmi-server and rmi-client. In the rmi-base we will put all the interfaces and any classes that both the client and server might need. We will then set the project dependencies such that rmi-server and rmi-client projects depend on rmi-base. Be sure that the project dependencies are setup correctly. In eclipse you can do this via right-click on a project -> Properties -> Java Build Path -> Projects tab. For the rmi-server and rmi-client projects, in this Projects tab, we have to Add the rmi-base project.

![eclipse projects build path 1](/screenshot_007.png)

![eclipse projects build path 1](/screenshot_008.png)

### The `rmi-base` Project
This project will contain our 2 interfaces for this RMI app: Compute and Task. The sample application defines a Compute interface that takes Task types, computes them and returns a result. A server component will implement the Compute interface and a client component will implement and send to the server a Task implementation.

![rmi base](/screenshot_003.png)

### The remote classloading

Here is where the remote classloading comes in. The server and client components will be running in different jvms. So even thought the server only knows the compute interface, it will acquire the implementation (defined in the client) from the client, run it and return the result. This can be very useful because most remoting frameworks transport data. RMI can transport data and logic.

The code for these 2 interfaces is noting spectacular and is shown below: 

```java
package saqib.rasul;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Compute extends Remote {

    public static final String SERVICE_NAME = "ComputeEngine";

     T executeTask(Task t) throws RemoteException;
}
```

```java
package saqib.rasul;

public interface Task {
    T execute();
}
```

Apart from these 2 interfaces we have 2 classes used by both client and server. One is the PolicyFileLocator whose task it is to give the path to the policy file. Our policy file, which by the way, is not secure at all in that it allows everything. The code of the PolicyFileLocator just finds this policy file (whether on the file system or in a jar), copies it to a temp-file and returns the path to this temp file. Unlike the last 2 interfaces, this class is much more spectacular. 

```java

package saqib.rasul;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * class to locate our most "secure" policy file
 *
 * @author srasul
 *
 */
public class PolicyFileLocator {

    public static final String POLICY_FILE_NAME = "/allow_all.policy";

    public static String getLocationOfPolicyFile() {
        try {
            File tempFile = File.createTempFile("rmi-base", ".policy");
            InputStream is = PolicyFileLocator.class.getResourceAsStream(POLICY_FILE_NAME);
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            int read = 0;
            while((read = is.read()) != -1) {
                writer.write(read);
            }
            writer.close();
            tempFile.deleteOnExit();
            return tempFile.getAbsolutePath();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

To go with this we have our policy file which allows all.

```
grant {
    permission java.security.AllPermission;
};
```

To be able to do RMI we need to do 3 things beforhand:

* set the java.rmi.server.codebase system property (more about this later)
* set the java.security.policy system property to the path to a policy file
* set a new SecurityManager

Since we need to do these three things for the server and client component, I decided to make a common class that does this: RmiStarter.

#### The java.rmi.server.codebase property
 For the server component, this defines the location for classes originating from this server can be found. For the client, this defines the location where the client can find classes that originating from the client can be found. Since the server needs to expose these interfaces to the RMI registry, for the server we define the location of the 2 interfaces. For the client, it needs to expose/send-over an implementation of Task, so we need to define for it the location of the PI class. Using the class object we can find the location of where a class is living. And this location is the value for the java.rmi.server.codebase property.

If this property is not defined, or defined wrong then you get nasty ClassNotFound exceptions when trying to use RMI:

```
java.rmi.ServerException: RemoteException occurred in server thread; nested exception is:
	java.rmi.UnmarshalException: error unmarshalling arguments; nested exception is:
	java.lang.ClassNotFoundException: saqib.rasul.Compute
	at sun.rmi.server.UnicastServerRef.oldDispatch(UnicastServerRef.java:396)
	at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:250)
	at sun.rmi.transport.Transport$1.run(Transport.java:159)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.Transport.serviceCall(Transport.java:155)
	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:535)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:790)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:649)
	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)
	at java.lang.Thread.run(Thread.java:619)
	at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:255)
	at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:233)
	at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:359)
	at sun.rmi.registry.RegistryImpl_Stub.rebind(Unknown Source)
	at saqib.rasul.server.TheRmiServer.doCustomRmiHandling(TheRmiServer.java:23)
	at saqib.rasul.RmiStarter.(RmiStarter.java:27)
	at saqib.rasul.server.TheRmiServer.(TheRmiServer.java:13)
	at saqib.rasul.server.TheRmiServer.main(TheRmiServer.java:32)
Caused by: java.rmi.UnmarshalException: error unmarshalling arguments; nested exception is:
	java.lang.ClassNotFoundException: saqib.rasul.Compute
	at sun.rmi.registry.RegistryImpl_Skel.dispatch(Unknown Source)
	at sun.rmi.server.UnicastServerRef.oldDispatch(UnicastServerRef.java:386)
	at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:250)
	at sun.rmi.transport.Transport$1.run(Transport.java:159)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.Transport.serviceCall(Transport.java:155)
	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:535)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:790)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:649)
	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)
	at java.lang.Thread.run(Thread.java:619)
Caused by: java.lang.ClassNotFoundException: saqib.rasul.Compute
	at java.net.URLClassLoader$1.run(URLClassLoader.java:202)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:190)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:307)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:248)
	at java.lang.Class.forName0(Native Method)
	at java.lang.Class.forName(Class.java:247)
	at sun.rmi.server.LoaderHandler.loadProxyInterfaces(LoaderHandler.java:711)
	at sun.rmi.server.LoaderHandler.loadProxyClass(LoaderHandler.java:655)
	at sun.rmi.server.LoaderHandler.loadProxyClass(LoaderHandler.java:592)
	at java.rmi.server.RMIClassLoader$2.loadProxyClass(RMIClassLoader.java:628)
	at java.rmi.server.RMIClassLoader.loadProxyClass(RMIClassLoader.java:294)
	at sun.rmi.server.MarshalInputStream.resolveProxyClass(MarshalInputStream.java:238)
	at java.io.ObjectInputStream.readProxyDesc(ObjectInputStream.java:1531)
	at java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1493)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1732)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1329)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:351)
	... 12 more
```
 A clue here is the final exception. The Server is looking for the Compute class and can’t find it. So therefore the server’s java.rmi.server.codebase property should point to the location where it can find this class. For the rmi-server project, eclipse takes care of the classpath and so using the `Compute.class.getProtectionDomain().getCodeSource().getLocation()` we can find out where this class lives. We can then feed the location of this class (could be directory or a jar file) as the value for the `java.rmi.server.codebase` property.

Here is the code for the RmiStarter class: 

```java
package saqib.rasul;

/**
 * class to do some common things for client & server to get RMI working
 *
 * @author srasul
 *
 */
public abstract class RmiStarter {

    /**
     *
     * @param clazzToAddToServerCodebase a class that should be in the java.rmi.server.codebase property.
     */
    public RmiStarter(Class clazzToAddToServerCodebase) {

        System.setProperty("java.rmi.server.codebase", clazzToAddToServerCodebase
            .getProtectionDomain().getCodeSource().getLocation().toString());

        System.setProperty("java.security.policy", PolicyFileLocator.getLocationOfPolicyFile());

        if(System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        doCustomRmiHandling();
    }

    /**
     * extend this class and do RMI handling here
     */
    public abstract void doCustomRmiHandling();

}
```

And with that we are over the hill. Its all downhill from here! 

## Writing the Server Component in the rmi-server Project

![server project](/screenshot_004.png)

The server component consists of the implementation of the Compute interface. The code for this is:

```java
package saqib.rasul.server;

import java.rmi.RemoteException;

import saqib.rasul.Compute;
import saqib.rasul.Task;

public class ComputeEngine
    implements Compute {

    @Override
    public  T executeTask(Task t)
        throws RemoteException {
        System.out.println("got compute task: " + t);
        return t.execute();
    }
}

```

And the code to start up the server component:

```java
package saqib.rasul.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import saqib.rasul.Compute;
import saqib.rasul.RmiStarter;

/**
 * start the server component. this exposes the an implementation of the Compute interface as a service over RMI
 *
 * @author srasul
 *
 */
public class ComputeEngineStarter
    extends RmiStarter {

    public ComputeEngineStarter() {
        super(Compute.class);
    }

    @Override
    public void doCustomRmiHandling() {
        try {
            Compute engine = new ComputeEngine();
            Compute engineStub = (Compute)UnicastRemoteObject.exportObject(engine, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(Compute.SERVICE_NAME, engineStub);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new ComputeEngineStarter();
    }
}
```

And thats it for the server component. If we were to run the ComptueEngineStarter class by its main method it would expose an RMI service with the name “ComputeEngine”. I find that running 1 class is much simpler than writing out a big-ass command on the command line to run a class, define classpaths and define system properties.

## Writing the Client Component in the rmi-client Project

The client contains an implementation of Task called PI. A snippet of code of this class is given below. This class calculates PI to a given number of decimal places.

![client project](/screenshot_005.png)

```java
package saqib.rasul.client;

import java.io.Serializable;
import java.math.BigDecimal;

import saqib.rasul.Task;

public class PI
    implements Task, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 3942967283733335029L;

    /** constants used in pi computation */
    private static final BigDecimal FOUR = BigDecimal.valueOf(4);

    /** rounding mode to use during pi computation */
    private static final int roundingMode = BigDecimal.ROUND_HALF_EVEN;

    /** digits of precision after the decimal point */
    private final int digits;

    /**
     * Construct a task to calculate pi to the specified precision.
     */
    public PI(int digits) {
        this.digits = digits;
    }

    /**
     * Calculate pi.
     */
    public BigDecimal execute() {
        return computePi(digits);
    }

    // .... the rest is omitted for saving pixels
    // .... 

```

 And a starter class that sends this to the PI task to the ComputeEngine service. 

 ```java
 package saqib.rasul.client;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import saqib.rasul.Compute;
import saqib.rasul.RmiStarter;

/**
 * get the RMI Compute service and send a task to compute PI to N digits
 *
 * @author srasul
 *
 */
public class StartComputeTaskPI
    extends RmiStarter {

    public StartComputeTaskPI() {
        super(PI.class);
    }

    @Override
    public void doCustomRmiHandling() {
        try {
            Registry registry = LocateRegistry.getRegistry();
            Compute compute = (Compute)registry.lookup(Compute.SERVICE_NAME);
            PI task = new PI(50);
            BigDecimal pi = compute.executeTask(task);
            System.out.println("computed pi: " + pi);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new StartComputeTaskPI();
    }
}
 ```

 ## Running it

First make sure you have the rmiregistry running by executing `rmiregistry` on the command line. See the original RMI tutorial for this

Then in eclipse, first run the ComputeEngineStarter class and then the `StartComputeTaskPI` class. This will run those 2 classes in separate jvms and they will communicator over RMI.

![outout eclipse](/screenshot_006.png)

If you wanted to make jars out of the projects, that would work as well. Then you can run the classes on the command line as done in the original RMI tutorial. Except you dont need to define any system properties. You just run the 2 classes in different jvms: 

![outout console](/Screenshot.png)

## The End & Downloads
Thats it for now. I hope you found the tutorial usefull and helpful. You can use the code in your projects. I have placed it under an Apache 2 License. 
