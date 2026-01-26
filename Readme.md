# Distributed Wallet System
## Coursework 02 – Concurrent and Distributed Systems

---

## 1. Overview

This project implements a **Distributed Wallet System** using **Java** and **gRPC**.  
The system is designed to demonstrate key concepts in distributed systems, including:

- Client–server communication using gRPC
- Concurrency control and synchronization
- Consistency guarantees
- Fault tolerance through retries and idempotency
- High availability using Apache ZooKeeper for leader election

Multiple wallet server instances run concurrently, with **Apache ZooKeeper** coordinating
leader election to ensure that client requests are processed reliably even if a server fails.

---

## 2. System Requirements

Before running the system, ensure the following software is installed:

- **Java Development Kit (JDK):** Version 21 or later
- **Apache Maven:** Version 3.8 or later
- **Apache ZooKeeper:** Version 3.6.x
- **Operating System:** Windows, Linux, or macOS

---

## 3. Project Structure

```text
distributed-wallet/
├── src/main/java/
│   ├── ds.cw.client        # Wallet client implementation
│   ├── ds.cw.server        # Wallet server & gRPC services
│   ├── ds.cw.common        # Shared domain classes (Account, Store)
│   └── ds.cw.zookeeper     # ZooKeeper leader election logic
│
├── src/main/proto/
│   └── wallet.proto        # gRPC service definition
│
├── pom.xml                 # Maven build configuration
└── README.md               # Project documentation

```
### 4. Compilation and Execution Instructions

This section describes how to compile and run the system from scratch.

### 4.1 Compile the Project

Navigate to the project root directory and execute:
````
mvn clean package
````

This command compiles the source code, generates gRPC classes, and builds the project.

### 4.2 Start Apache ZooKeeper

ZooKeeper must be running before starting any wallet server instances.

Windows
````
zkServer.cmd
````

Linux / macOS
````
./zkServer.sh start
````

ZooKeeper runs on port 2181 by default and is used for leader election among wallet servers.

### 4.3 Start Wallet Servers (High Availability)

To demonstrate high availability, start three wallet server instances on different ports.
Each server connects to ZooKeeper and participates in leader election.

Server 1
````
mvn exec:java -Dexec.mainClass=ds.cw.server.WalletServer -Dexec.args="9000"
````

Server 2
````
mvn exec:java -Dexec.mainClass=ds.cw.server.WalletServer -Dexec.args="9001"
````

Server 3
``````
mvn exec:java -Dexec.mainClass=ds.cw.server.WalletServer -Dexec.args="9002"
``````
### 5.3 Start Wallet Client

Open a new terminal and run:
````
mvn exec:java -Dexec.mainClass=ds.cw.client.WalletClient
````

The client displays an interactive menu allowing users to:

* Create an account
* Check account balance
* Deposit funds
* Withdraw funds

### 6.  Known Limitations

The following limitations apply to the current implementation:

1. No Persistent Storage
All wallet data is stored in memory. If all servers are stopped or restarted simultaneously, account data is lost.

2. Single Leader Processing
Although multiple servers are deployed, only the elected leader handles client requests. Followers do not process requests directly.

3. No Automatic Client Failover
The client connects to a specific server port. If the leader changes, the client must be restarted or reconfigured manually.

4. Basic Fault Tolerance
Client-side retry logic handles transient failures, but advanced retry policies and timeouts are not implemented.

5. No Security Features
Authentication, authorization, and encrypted communication (TLS) are not implemented in this version.


### 7.  Future Improvements

Possible extensions to the system include:

* Persistent storage using a database or file system
* Automatic client-side leader discovery via ZooKeeper 
* Replicated state across servers 
* Secure gRPC communication using TLS 
* Monitoring and health checks

### 8. Conclusion

This Distributed Wallet System successfully demonstrates core distributed systems principles including gRPC communication, synchronization, consistency, fault tolerance, and high availability. The project aligns closely with the coursework objectives and provides a strong foundation for future enhancements.