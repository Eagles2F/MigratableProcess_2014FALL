JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
CLASSPATH=.;src/manager
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	ProcessManager.java \
	manager/ManagerServer.java\
	manager/ConnectionServer.java\
	processes/GrepProcess.java\
	processes/MigratableProcess.java\
	processes/ReverseWordProcess.java\
	utility/CommandType.java\
	utility/Message.java\
	utility/ProcessInfo.java\
	utility/ResponseType.java\
	utility/TransactionalFileInputStream.java\
	utility/TransactionalFileOutputStream.java\
	worker/WorkerNode.java

default: classes

classes: 
	$(JC) ./manager/ManagerServer.java -d ../bin/
	$(JC) ./manager/ProcessManager.java -d ../bin/
	$(JC) ./manager/ConnectionServer.java -d ../bin/
	$(JC) ./processes/MigratableProcess.java -d ../bin/
	$(JC) ./processes/ReverseWordProcess.java -d ../bin/
	$(JC) ./processes/GrepProcess.java -d ../bin/
	$(JC) ./utility/CommandType.java -d ../bin/
	$(JC) ./utility/Message.java -d ../bin/
	$(JC) ./utility/ProcessInfo.java -d ../bin/
	$(JC) ./utility/ResponseType.java -d ../bin/
	$(JC) ./utility/TransactionalFileOutputStream.java -d ../bin/
	$(JC) ./worker/WorkerNode.java -d ../bin/
	

clean:
	$(RM) -rf ../bin/*
