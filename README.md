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
broadcast-subnet: YOUR_PEER_TO_PEER_NETWORK_BROADCAST_SUBNET
```

#### systemd service operation:
- service status: `systemctl status alpha4.service`
- start service: `systemctl start alpha4.service`
- stop service: `systemctl stop alpha4.service`
- service output: `journalctl --follow -u alpha4.service -b`

## Dependencies
Use maven to build dependencies

```xml
<dependencies>
		<!-- logger implementation dependency -->
		<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-classic</artifactId>
			<version>1.4.14</version>
		</dependency>
		<!-- logger API dependency -->
		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>slf4j-api</artifactId>
			<version>2.0.12</version>
		</dependency>
		<!-- JSON to Object and back mapper dependency -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
			<version>2.16.1</version>
		</dependency>
		<!-- used to unescape HTML entities, received via API -->
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-text</artifactId>
			<version>1.11.0</version>
		</dependency>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
			<version>4.13.2</version>
			<scope>test</scope>
		</dependency>
	</dependencies>
```

## Requirements from the program

This app was required to:
* Scan subnet for possible peers to join via UDP.
* Save messages (up to 100) in local memmory
* Share messages from local memmory between the peers
* Have http server as an api for the program

## API

### Read
```
curl http://localhost:8000/messages
```

### Write
```
curl http://localhost:8000/send?message=text
```
