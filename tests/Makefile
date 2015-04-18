LIBS=-cp /home/matthew/workspace/cs288_assign1/assign1.jar:/home/matthew/workspace/cs288_assign1/assign1-submit.jar

CLSS= $($(wildcard *.java):.java=.class)

all: 
	javac -g $(LIBS) src/**/*.java
	cd src && jar cvf ../out.jar **/*.class

run:
	java $(LIBS):out.jar simple.ScratchMain
