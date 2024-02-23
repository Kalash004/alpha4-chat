# Peer-To-Peer chat service

#### build Java
```bsh
mvn clean install
```

#### build RPM
```bsh
buildrpm.bash
```

#### configuration
Please configure next properties custom.properties file:

```bsh
peer-id: YOUR_PEER_ID
broadcast-address-subnet: YOUR_PEER_TO_PEER_NETWORK_BROADCAST_ADDRESS
```

#### systemd service operation:
- service status: `systemctl status alpha4.service`
- start service: `systemctl start alpha4.service`
- stop service: `systemctl stop alpha4.service`
- service output: `journalctl --follow -u alpha4.service -b`
