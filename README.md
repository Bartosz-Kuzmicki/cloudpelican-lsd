# cloudpelican-lsd
Log stream dump tool (uses Rsyslog, Apache Kafka and Apache Storm)

### Introduction ###
CloudPelican LSD (log stream dump) is designed for analyzing realtime log streams. This enables you to interact with log streams in realtime, where as tools like `grep` would only run on a single machine. By forwarding the syslog of your servers to Kafka (using rsyslogd and omkafka as transport layer), you have all your logs in one stream. CloudPelican LSD sits directly on that stream and filters data based on your desires.

### Examples ###
Tail all your files with a regex:
```
$ cloudpelican>
$ cloudpelican> tail stream:default where 'kernel';
$ your messages matching keyword 'kernel' (which is a regex)
```

Tail all log files non-interactively:
`cloudpelican -e "tail stream:default"`

Console charts
![alt tag](https://raw.github.com/RobinUS2/cloudpelican-lsd/master/docs/console_chart.png)

# Building #
In order to use Cloudpelican LSD you need clone this repository, install a few basic tools and follow the instructions below. All Java source code has to be compiled with JDK 1.8
```
git clone git@github.com:RobinUS2/cloudpelican-lsd.git
brew install go
brew install maven
```

### Starting storm ###
```
cd cloudpelican-lsd/storm
mvn clean install
java -jar java -jar ~/.m2/repository/nl/us2/cloudpelican/storm-processor/1.0-SNAPSHOT/storm-proceor-1.0-SNAPSHOT-jar-with-dependencies.jar -zookeeper=zookeeper1.domain.com:2181,zookeeper2.domain.com:2181,zookeeper3.domain.com:2181 -topic=my_logging -submit
```

### Starting supervisor ###
```
cd cloudpelican-lsd/supervisor
./build.sh
./supervisor -auth-user="<your username>" -auth-password="<your password>"
```

### Starting the CLI ###
```
cd cloudpelican-lsd/cli
./build.sh # Uses go build to compile the binary
./link_binary.sh # This will add a symlink to /usr/bin/cloudpelican for easy use
cloudpelican
```

### Running storm in local mode ###
```
cd cloudpelican-lsd/storm
mvn clean install
java -jar java -jar ~/.m2/repository/nl/us2/cloudpelican/storm-processor/1.0-SNAPSHOT/storm-proceor-1.0-SNAPSHOT-jar-with-dependencies.jar -zookeeper=zookeeper1.domain.com:2181,zookeeper2.domain.com:2181,zookeeper3.domain.com:2181 -topic=my_logging -grep=this
```

# Authentication #
The CLI talks with the Supervisor in order to communicate safely with the Storm topology.

```
$ cloudpelican> auth <username> <password>
$ cloudpelican> connect http://<supervisor_hostname>:<port>/
$ cloudpelican> ping
Pong
$ cloudpelican> save
Saved session
```

# Data Flow #
[application] => [rsyslog on host] => [kafka] => [storm] => [cloudpelican supervisor] => [cloudpelican CLI]

![alt tag](https://raw.github.com/RobinUS2/cloudpelican-lsd/master/docs/infra.png)
