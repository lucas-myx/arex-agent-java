[![Build Status](https://github.com/arextest/arex-agent-java/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/arextest/arex-agent-java/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/arextest/arex-agent-java/branch/main/graph/badge.svg)](https://app.codecov.io/gh/arextest/arex-agent-java)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arextest_arex-agent-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arextest_arex-agent-java)

# <img src="https://avatars.githubusercontent.com/u/103105168?s=200&v=4" alt="Arex Icon" width="27" height=""> AREX

#### An Open Source Testing Framework with Real World Data

- [Introduction](https://#introduction)
- [Installation](https://#installation)
- [Getting Started](https://#getting-started)
- [Contributing](https://#contributing)
- [License](https://#license)

## Introduction


As your application evolves more complex, the effort required to thoroughly test against it also becomes tremendous. Arex is a framework designed around a quite straightforward principle of leveraging your real world data(i.e. database record, service payload, cache items etc.) for regression testing. Simple is powerful. The idea behind makes it incredibly powerful.

AREX provides an out-of-box agent file that could be attached to any applications with Java 8+ and dynamically weaves solid  bytecode into your existing code to record the real data of live traffic, and further use and replay it for mocking, testing, and debugging purpose.

AREX is implemented with an unique mechanism for recording. Instead of being a proxy like other similar framework, `AREX` sits in the background without awareness of your application to record realistic data in live traffic which means that no intrusive code changes are required when integrating it to your existing application.

AREX utilizes the advanced Java technique, Instrument API, and is capable of instrumenting various libraries and frameworks which are widely used.

**Libraries and frameworks supported by Arex (to be added...)**


- Java Executors
- System time
- Apache HttpAsyncClient 4.x
- Apache HttpClient 4.x
- OkHttp 3+
- Hibernate 5.x
- MyBatis 3.x
- Redisson 3.x
- Lettuce 6+
- Jedis 2.10+, 4+
- Spring Boot 1.4+-2.x+, Servlet API 3+、5+
- Custom type
- Netty server 4.1+

## Installation


Simply download the latest binary from [github](https://github.com/arextest/releases) or compile it by yourself.

There are two agent files provided in the arex-agent-jar folder like below. They must be placed in the same directory.

```other
arex-agent-<version>.jar
arex-agent-bootstrap-<version>.jar

```


## Getting Started


***Enable the instrumentation agent by configuring a `javaagent` flag to the JVM to run arex：***

AREX agent works along with the [AREX storage service](https://github.com/arextest/arex-storage).

You could just configure the host and port of them respectively, like below

```other
java -javaagent:/path/to/arex-agent-<version>.jar
      -Darex.service.name=your-service-name
      -Darex.storage.service.host=[storage.service.host:port](storage.service.host:port) 
      -jar your-application.jar
```


Alternatively, you can put those configuration item in `arex.agent.conf` file, like below

```other
arex.service.name=your-service-name  
arex.storage.service.host=<storage.service.host:port> 
```


Then simply run:

```other
java -javaagent:/path/to/arex-agent-<version>.jar
      -Darex.config.path=/path/to/arex.agent.conf
      -jar your-application.jar
```


***Also, You can Run with CLI in local mode:***

Simply click the script in the `arex-agent-java/bin` directory to start the command line tool, or run it by following `java` command:

```other
java -cp "/path/to/arex-cli-parent/arex-cli/target/arex-cli.jar" io.arex.cli.ArexCli
```


The supported commands are as follows:


- **record**- record data or set record rate

  `[option: -r/--rate]` set record rate, default value 1, record once every 60 seconds

  `[option: -c/--close]` shut down record


- **replay**- replay recorded data and view differences

  `[option: -n/--num]` replay numbers, default the latest 10


- **watch**- view replay result and differences

  `[option: -r/--replayId]` replay id, multiple are separated by spaces


- **debug**- local debugging of specific cases

  `[option: -r/--recordId]` record id, required Option

In local mode, AREX uses [H2](https://www.h2database.com) as a local storage to save the recorded data for testing purpose,  Storage Service are no longer required, But you can't use the AREX-UI in this mode either.



## Contributing

1. Fork it
2. Create your feature branch
3. Commit your code changes and push to your feature branch
4. Create a new Pull Request


## License
- Code: [Apache-2.0](https://github.com/arextest/arex-agent-java/blob/main/LICENSE)
