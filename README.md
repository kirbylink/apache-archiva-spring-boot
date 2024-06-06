# Apache Archiva Spring Boot application

This is a Spring Boot application that runs an Apache Archiva War file.

## Project Status: Archived

**Important Notice:** This project has been archived and is no longer maintained. Due to the following reasons, development has been discontinued:
- Apache Archiva has been archived and is no longer actively developed.
- Newer versions of Spring Boot require Java 17 or higher, while Apache Archiva only supports Java 8.
- Significant changes in the Java ecosystem, such as the transition from `javax` to `jakarta` packages, would require extensive modifications to update Apache Archiva, which is not feasible given its complex structure.

As a result, this repository will no longer receive updates or new releases.

Table of contents:
- [General](#general)
- [Requirements](#requirements)
- [How to build](#how-to-build)
- [How to start](#how-to-start)
  - [Optional parameters](#optional-parameters)

## General
Apache Archiva™ is an extensible repository management software that helps take care of your own personal or enterprise-wide build artifact repository. It is the perfect companion for build tools such as Maven, Continuum, and ANT.<br /> 
Apache [offers](https://archiva.apache.org/download.cgi) standalone binaries for several operating systems, but not for ARM architecture. Besides binaries, Apache provides also Web Application Archive (WAR) files that can be used in Java-Servlet applications.<br />
Since Spring Boot brings its own Tomcat server, it is easier to run and to configure. Unfortunately, there are only a few entries when you try to search how to get a WAR file running with Spring Boot. Most tutorials show how to convert a Spring Boot application to WAR.<br />
This repository contains a minimal setup with Spring Boot that needs at least one argument that points to the locally stored Apache Archiva WAR file, and Archiva needs also two environment arguments that point to a folder for Tomcat CATALINA.<br />
<br />
Sources that helped to create a running Spring Boot application with an external WAR file:
- [How to deploy WAR files to Spring Boot Embedded Tomcat](https://www.vojtechruzicka.com/spring-boot-add-war-to-embedded-tomcat/)
- [The Build Artifact Repository Manager](https://archiva.apache.org/index.html)

## Requirements
- Java Runtime Environment (JRE) 8
- Apache Archiva WAR file (tested with version [2.2.10](http://www.apache.org/dist/archiva/2.2.10/binaries/apache-archiva-2.2.10.war)).
- Maven
- Git (optional)

## How to build
### Docker image
See [Dockerfile](./Dockerfile) or [Dockerfile-alpine](./Dockerfile-alpine)
Note: The Apache Archiva WAR file must be in the root folder and called `webapp.war`.

### Local installation
- Download or build the newest Apache Archiva WAR file (tested with version [2.2.10](http://www.apache.org/dist/archiva/2.2.10/binaries/apache-archiva-2.2.10.war))
- Run `mvn verify -Dapache-archiva.configuration.path=/path/to/apache-archiva.war/file` or `mvn package` (skips test)
- The Spring Boot JAR file can be found under `target/apache-archiva-spring-boot-<version>.jar`
- Run the application (See next section)

## How to start
The application must be started with the following parameters:

```
spring.profiles.active
apache-archiva.configuration.path
```

and the following environment variables

```
appserver.base
appserver.home
```

Example:

```
java -Dappserver.base=<path/to/base|home> -Dappserver.home=<path/to/base|home> -jar apache-archiva-spring-boot-<version>.jar --spring.profiles.active=<local|prod> --apache-archiva.configuration.path=<path/to/archiva/webapp>
```

Additional example can be found in the [run.sh](./run.sh) script

### Optional parameters

```
apache-archiva.configuration.db.url
```

Examples:

Storing database to disk
```
apache-archiva.configuration.db.url=jdbc:derby:directory:<path/to/database/for/users>;create=true
```

In memory database
```
apache-archiva.configuration.db.url=jdbc:derby:memory:users;create=true
```