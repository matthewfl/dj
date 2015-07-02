TARGET= target/scala-2.11/dj-assembly-0.0.1.jar

.PHONY: all download build

all: build


download:
	@if [ ! -f .sbt-launch.jar ]; then \
		curl -o .sbt-launch.jar http://static.matthewfl.com/downloads/sbt-launch.jar; \
	fi

build: download $(TARGET)

clean: download
	java -Xmx1536M -Xms512M -XX:+CMSClassUnloadingEnabled -jar .sbt-launch.jar clean

run: build
	

$(TARGET): $(wildcard src/**/*)
	java -Xmx1536M -Xms512M -XX:+CMSClassUnloadingEnabled -jar .sbt-launch.jar assembly
