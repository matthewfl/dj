TARGET= target/scala-2.11/dj-assembly-0.0.1.jar

# find or download the sbt command
SBT := $(shell which sbt 2>/dev/null > /dev/null && echo sbt || { if [ ! -f .sbt-launch.jar ]; then curl -o .sbt-launch.jar http://static.matthewfl.com/downloads/sbt-launch.jar; fi; 	echo 'java $$SBT_OPTS -jar .sbt-launch.jar' ; })

SBT_OPTS ?= -Xmx1536M -Xms512M -XX:+CMSClassUnloadingEnabled
export SBT_OPTS

# find an open port that we can use for the debugger interface
PORT := $(shell python -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()')

src_files= $(shell find src/ -type f)

.PHONY: all build clean run console

all: build

build: $(TARGET)

clean:
	$(SBT) clean

run: build
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$(PORT) -Ddj.jdwpport=$(PORT) -jar $(TARGET)

reload-debug: $(TARGET)
reload-debug:
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$(PORT) -Ddj.jdwpport=$(PORT) -jar $(TARGET) -fjar $(TARGET) -maincls testcase.SimpleMapthing -debug_clazz_bytecode /tmp/djrl/

console: SBT_OPTS += -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$(PORT) -Ddj.jdwpport=$(PORT)
console:
	SBT_OPTS="$(SBT_OPTS)" $(SBT) console

$(TARGET): $(src_files)
	$(SBT) assembly
