# SDN Northbound Application for Dynamic Network Routing

## Project Overview

This project implements an intelligent SDN northbound application that leverages **ONOS (Open Network Operating System)** to provide adaptive, real-time network routing optimization. The application continuously monitors network traffic patterns—including link utilization, bandwidth consumption, and congestion metrics—and dynamically reconfigures routing paths to ensure optimal performance and resource utilization.

By interfacing with ONOS through its RESTful Northbound APIs, the application demonstrates the core principles of **Software-Defined Networking (SDN)**: centralized control, programmability, and automation. The system detects network congestion in real-time and triggers intelligent rerouting decisions, effectively balancing traffic loads and preventing bottlenecks.

**Why it's useful:**
- Automates network management without manual intervention
- Reduces latency and improves throughput during peak traffic periods
- Provides a foundation for building intelligent, self-optimizing networks
- Demonstrates practical SDN concepts for research and educational purposes

---

## Architecture & Flow Diagram

The system architecture consists of three main layers:

```
┌─────────────────────────────────────────────────────────┐
│          Northbound Application (Java)                  │
│  - Traffic Monitor                                      │
│  - Congestion Detection Engine                          │
│  - Dynamic Routing Logic                                │
│  - REST API Client                                      │
└────────────────┬────────────────────────────────────────┘
                 │ REST API (HTTP/JSON)
                 ▼
┌─────────────────────────────────────────────────────────┐
│          ONOS SDN Controller                            │
│  - Topology Service                                     │
│  - Flow Rule Service                                    │
│  - Statistics Service                                   │
│  - Intent Framework                                     │
└────────────────┬────────────────────────────────────────┘
                 │ OpenFlow Protocol
                 ▼
┌─────────────────────────────────────────────────────────┐
│          Mininet Network Emulator                       │
│  - Virtual Switches (Open vSwitch)                      │
│  - Virtual Hosts                                        │
│  - Custom Topologies                                    │
└─────────────────────────────────────────────────────────┘
```

### Data Flow:

1. **Topology Discovery**: ONOS discovers the Mininet network topology via OpenFlow
2. **Traffic Monitoring**: The application polls ONOS for port statistics and flow metrics
3. **Analysis**: The application analyzes traffic patterns to detect congestion (threshold-based or ML algorithms)
4. **Decision Making**: When congestion is detected, the application computes alternative paths
5. **Route Installation**: New flow rules or intents are pushed to ONOS via REST API
6. **Flow Update**: ONOS translates high-level intents into OpenFlow rules and installs them on switches
7. **Traffic Rerouting**: Packets begin flowing through the optimized path

---

## Features

- **Real-Time Traffic Monitoring**: Continuously collects port statistics, byte/packet counts, and link utilization from ONOS
- **Intelligent Congestion Detection**: Identifies bottlenecks using configurable thresholds for bandwidth, packet loss, or latency
- **Dynamic Path Reconfiguration**: Automatically computes and installs alternative routes when congestion is detected
- **REST API Integration**: Seamless communication with ONOS via Northbound REST APIs
- **Event-Driven Architecture**: Responds to network changes in real-time
- **Topology-Aware Routing**: Leverages ONOS topology service for accurate path computation
- **Logging & Analytics**: Tracks rerouting events and performance metrics for analysis

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| **SDN Controller** | ONOS (Open Network Operating System) |
| **Northbound Application** | Java 11+ |
| **Network Emulator** | Mininet 2.3+ |
| **Southbound Protocol** | OpenFlow 1.3 |
| **Communication** | REST APIs (JSON over HTTP) |
| **Build Tool** | Maven / Gradle |
| **Virtual Switches** | Open vSwitch (OVS) |
| **Testing Tools** | iperf3, ping, tcpdump |

---

## Installation & Setup

### Prerequisites

- **Ubuntu 20.04+** or **WSL2** (Windows Subsystem for Linux)
- **Docker** and **Docker Compose**
- **Java Development Kit (JDK) 11+**
- **Maven** or **Gradle**
- **Python 2.7** (for Mininet)
- **Git**

### Step 1: Install ONOS (Docker Method)

```bash
# Pull the ONOS Docker image
docker pull onosproject/onos:2.7.0

# Run ONOS container
docker run -t -d \
  --name onos \
  -p 8181:8181 \
  -p 8101:8101 \
  -p 5005:5005 \
  -p 830:830 \
  onosproject/onos:2.7.0

# Verify ONOS is running
docker logs -f onos

# Access ONOS CLI
ssh -p 8101 onos@localhost
# Password: rocks
```

### Step 2: Install Mininet

```bash
# Install Mininet
sudo apt-get update
sudo apt-get install -y mininet

# Verify installation
sudo mn --version

# Install Open vSwitch
sudo apt-get install -y openvswitch-switch
```

### Step 3: Clone and Build the Application

```bash
# Clone the repository
git clone https://github.com/yourusername/sdn-dynamic-routing.git
cd sdn-dynamic-routing

# Build the Java application
mvn clean install
# OR with Gradle
gradle build

# The compiled JAR will be in target/ or build/libs/
```

### Step 4: Connect Mininet to ONOS

```bash
# Start a custom topology in Mininet connected to ONOS controller
sudo mn --custom topology.py \
        --topo mytopo \
        --controller remote,ip=127.0.0.1,port=6653 \
        --switch ovsk,protocols=OpenFlow13

# In Mininet CLI, verify connectivity
mininet> pingall
```

### Step 5: Activate Required ONOS Applications

```bash
# Access ONOS CLI
ssh -p 8101 onos@localhost

# Activate required apps
onos> app activate org.onosproject.openflow
onos> app activate org.onosproject.fwd
onos> app activate org.onosproject.proxyarp
onos> app activate org.onosproject.hostprovider

# Verify topology is detected
onos> hosts
onos> devices
onos> links
```

---

## Usage

### Running the Northbound Application

```bash
# Run the Java application
java -jar target/sdn-routing-app-1.0.jar \
  --onos-ip=localhost \
  --onos-port=8181 \
  --username=onos \
  --password=rocks \
  --polling-interval=5

# Expected output:
# [INFO] Connecting to ONOS at http://localhost:8181
# [INFO] Topology discovered: 6 devices, 8 links
# [INFO] Starting traffic monitor (interval: 5s)
# [INFO] Monitoring traffic on all links...
```

### Generating Traffic in Mininet

```bash
# In Mininet CLI
mininet> h1 ping h4
mininet> iperf h1 h2

# Generate congestion on a specific link
mininet> h1 iperf -c h2 -t 60 -b 100M
```

### Testing Dynamic Rerouting

1. **Baseline Test**: Establish normal traffic flow
   ```bash
   mininet> pingall
   mininet> h1 ping -c 10 h4
   ```

2. **Induce Congestion**: Generate heavy traffic on a link
   ```bash
   mininet> iperf h1 h2 -t 60 -b 100M
   ```

3. **Observe Rerouting**: Check application logs
   ```bash
   [WARN] Congestion detected on link s1->s2 (utilization: 95%)
   [INFO] Computing alternative path...
   [INFO] New path found: h1 -> s1 -> s3 -> s4 -> h2
   [INFO] Installing flow rules via ONOS API...
   [INFO] Rerouting complete. Latency improved by 40ms
   ```

4. **Verify New Path**: Check ONOS flow rules
   ```bash
   onos> flows -s
   onos> intents
   ```

### Accessing ONOS Web UI

Navigate to `http://localhost:8181/onos/ui` in your browser
- **Username**: `onos`
- **Password**: `rocks`

View topology, flows, and statistics in real-time.

---

## Results & Output

### Expected Behavior

When the application detects congestion (e.g., link utilization exceeds 80%):

1. **Detection Phase**:
   ```
   [2025-01-15 10:23:45] Traffic Monitor: Link s1->s2 at 87% capacity
   [2025-01-15 10:23:45] Threshold exceeded (max: 80%)
   ```

2. **Path Computation**:
   ```
   [2025-01-15 10:23:46] Analyzing topology for alternative routes
   [2025-01-15 10:23:46] Found 2 candidate paths
   [2025-01-15 10:23:46] Selected optimal path: s1->s3->s4->s2 (cost: 15)
   ```

3. **Route Installation**:
   ```
   [2025-01-15 10:23:47] Pushing intent to ONOS: h1/None <-> h2/None
   [2025-01-15 10:23:47] Flow rules installed successfully
   [2025-01-15 10:23:48] Traffic rerouted via s3
   ```

4. **Performance Improvement**:
   ```
   Before: RTT avg = 125ms, Throughput = 45 Mbps
   After:  RTT avg = 82ms,  Throughput = 95 Mbps
   ```

### Visualization

The ONOS GUI will display:
- Link color changes (green → yellow → red) based on utilization
- Updated flow paths highlighted in the topology view
- Real-time traffic statistics on each port

---

## Future Enhancements

- **Machine Learning Integration**: Use ML models (Random Forest, Neural Networks) to predict congestion before it occurs
- **Python Analytics Engine**: Build a Python-based analytics dashboard using Flask/Django for visualization
- **Multi-Objective Optimization**: Balance multiple metrics (latency, bandwidth, packet loss, energy consumption)
- **QoS-Aware Routing**: Implement priority-based routing for different traffic classes
- **Load Balancing**: Distribute traffic across multiple paths using ECMP (Equal-Cost Multi-Path)
- **Automated Testing Suite**: Develop comprehensive unit and integration tests
- **Kubernetes Deployment**: Containerize all components for cloud-native deployment
- **Intent-Based Networking**: Transition from flow rules to high-level network intents
- **Integration with P4**: Explore programmable data planes using P4 language
- **Distributed ONOS Cluster**: Deploy ONOS in a clustered setup for high availability

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows Java coding standards and includes appropriate documentation.

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Contributors & Acknowledgments

**Project Team:**
- [Your Name] - Initial development and architecture design
- [Contributor Names] - Feature implementations and testing

**Acknowledgments:**
- [ONOS Project](https://onosproject.org/) - For providing a robust SDN controller platform
- [Mininet Team](http://mininet.org/) - For the excellent network emulation framework
- [Open Networking Foundation](https://opennetworking.org/) - For OpenFlow specifications
- Academic advisors and mentors who guided this research

**References:**
- ONOS Documentation: https://wiki.onosproject.org/
- Mininet Walkthrough: http://mininet.org/walkthrough/
- OpenFlow Specification: https://opennetworking.org/software-defined-standards/specifications/
- SDN Architecture: https://www.opennetworking.org/wp-content/uploads/2013/02/TR_SDN_ARCH_1.0_06062014.pdf

---

## Contact

For questions, suggestions, or collaboration opportunities:

- **Email**: your.email@example.com
- **GitHub Issues**: [Create an issue](https://github.com/yourusername/sdn-dynamic-routing/issues)
- **LinkedIn**: [Your LinkedIn Profile](https://linkedin.com/in/yourprofile)

---

**Star this repository if you found it helpful!**
