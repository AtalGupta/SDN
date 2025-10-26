#!/usr/bin/env python3
"""
Mininet Topology for SDN Dynamic Routing Project

Creates a network topology with:
- 5 OpenFlow switches
- 5 hosts
- Multiple redundant paths for testing congestion and rerouting

Topology:
         h1----s1----s2----h2
                |\ /|
                | X |
                |/ \|
         h3----s3----s4----h4
                |
                s5
                |
                h5
"""

import os
from mininet.net import Mininet
from mininet.node import RemoteController, OVSKernelSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import TCLink

def createTopology():
    """Create the network topology"""

    # Get ONOS controller IP from environment (default to localhost)
    onos_ip = os.environ.get('ONOS_IP', '127.0.0.1')

    info('*** Creating network with ONOS controller at %s\n' % onos_ip)

    # Create Mininet network
    net = Mininet(
        controller=None,
        switch=OVSKernelSwitch,
        link=TCLink,
        autoSetMacs=True
    )

    info('*** Adding ONOS controller\n')
    # Add remote ONOS controller
    net.addController(
        'c0',
        controller=RemoteController,
        ip=onos_ip,
        port=6653
    )

    info('*** Adding switches\n')
    # Add 5 switches with OpenFlow 1.3
    s1 = net.addSwitch('s1', cls=OVSKernelSwitch, protocols='OpenFlow13')
    s2 = net.addSwitch('s2', cls=OVSKernelSwitch, protocols='OpenFlow13')
    s3 = net.addSwitch('s3', cls=OVSKernelSwitch, protocols='OpenFlow13')
    s4 = net.addSwitch('s4', cls=OVSKernelSwitch, protocols='OpenFlow13')
    s5 = net.addSwitch('s5', cls=OVSKernelSwitch, protocols='OpenFlow13')

    info('*** Adding hosts\n')
    # Add 5 hosts with specific IPs
    h1 = net.addHost('h1', ip='10.0.0.1/24', mac='00:00:00:00:00:01')
    h2 = net.addHost('h2', ip='10.0.0.2/24', mac='00:00:00:00:00:02')
    h3 = net.addHost('h3', ip='10.0.0.3/24', mac='00:00:00:00:00:03')
    h4 = net.addHost('h4', ip='10.0.0.4/24', mac='00:00:00:00:00:04')
    h5 = net.addHost('h5', ip='10.0.0.5/24', mac='00:00:00:00:00:05')

    info('*** Creating links\n')
    # Host-to-switch links (100 Mbps)
    net.addLink(h1, s1, cls=TCLink, bw=100, delay='5ms')
    net.addLink(h2, s2, cls=TCLink, bw=100, delay='5ms')
    net.addLink(h3, s3, cls=TCLink, bw=100, delay='5ms')
    net.addLink(h4, s4, cls=TCLink, bw=100, delay='5ms')
    net.addLink(h5, s5, cls=TCLink, bw=100, delay='5ms')

    # Switch-to-switch links (100 Mbps) - Creates mesh topology
    # Primary path: s1 <-> s2
    net.addLink(s1, s2, cls=TCLink, bw=100, delay='10ms')

    # Alternative paths (X topology)
    net.addLink(s1, s3, cls=TCLink, bw=100, delay='10ms')
    net.addLink(s1, s4, cls=TCLink, bw=100, delay='10ms')
    net.addLink(s2, s3, cls=TCLink, bw=100, delay='10ms')
    net.addLink(s2, s4, cls=TCLink, bw=100, delay='10ms')

    # Lower tier connections
    net.addLink(s3, s4, cls=TCLink, bw=100, delay='10ms')
    net.addLink(s3, s5, cls=TCLink, bw=100, delay='10ms')

    info('*** Starting network\n')
    net.start()

    info('*** Network topology created successfully!\n')
    info('*** Topology summary:\n')
    info('    - 5 switches (s1-s5)\n')
    info('    - 5 hosts (h1-h5) with IPs 10.0.0.1-5\n')
    info('    - Multiple redundant paths for testing\n')
    info('    - All links: 100 Mbps bandwidth\n')
    info('\n')
    info('*** Testing connectivity...\n')

    # Wait for controller connection
    info('*** Waiting for switches to connect to ONOS...\n')
    net.waitConnected()

    info('*** Running pingall to discover hosts...\n')
    net.pingAll()

    info('\n*** Network is ready!\n')
    info('*** You can now:\n')
    info('    1. Test connectivity: mininet> pingall\n')
    info('    2. Generate traffic: mininet> iperf h1 h2\n')
    info('    3. Create congestion: mininet> h1 iperf -c 10.0.0.2 -b 85M -t 60\n')
    info('    4. Monitor links in ONOS GUI: http://localhost:8181/onos/ui\n')
    info('\n')

    # Start CLI
    CLI(net)

    info('*** Stopping network\n')
    net.stop()

if __name__ == '__main__':
    # Set log level
    setLogLevel('info')

    # Create and run topology
    createTopology()
