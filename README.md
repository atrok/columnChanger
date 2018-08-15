# columnChanger
small utility to change properties of agents and agent groups via Genesys configuration server

Steps to compile the project:

1. Download PSDK 853.2.8
2. import all library files from /lib folder into local maven repository
for instance:

mvn install:install-file -Dfile=commonsappblock.jar -DpomFile=pom/commonsappblock.pom

3. build the project (there are no tests yet to run so we omit it) 
mvn clean install -Dmaven.test.skip=true

4. Check /target directory for 

/lib folder

column-changer-0.0.1-SNAPSHOT.jar 

5. Open column-changer-0.0.1-SNAPSHOT.jar and make sure that it contains:

ag.txt

persons.txt

log4j2.xml

application.properties

6. Copy /lib folder and column-changer-0.0.1-SNAPSHOT.jar into destination directory.

7. start column-changer-0.0.1-SNAPSHOT.jar

java -jar column-changer-0.0.1-SNAPSHOT.jar 

8.Enjoy!


How does it work
=================

On startup utility checks next if 2 files are in its classpath, and it fails if it can't find it.
Files are located in the root of .jar file by default, and thus are within the scope of classpath visibility.

application.properties file contains configuration server connection options, which needs to be prepopulated before utility starts.
2 additional options control execution flow:

execution.timeout=<int> in msec (10msec default)
execution.tasks=<comma separated list of <person|ag>>
  
In order to execute tasks for person objects only specify 
execution.tasks=person

In order to execute tasks for agent groups objects only specify
execution.tasks=ag

Both tasks
execution.tasks=person,ag


log4j2.xml file is log configuration file, and doesn't require any adjustment.
ag.txt and persons.txt files contains line break separated list of dbids of agent group objects and person objects correspondingly.
Utility once it's started and is succesfully connected to configuration server does the following in next order:
- reads persons.txt file
- pulls person object from configserver
- looks for workbin related options in its interaction-workspace section
- if options are found then it removes it from Person object and puts Person object into the internal list for following processing
- once all the Person objects are modified it takes the list of modified Person objects and submits Person objects one by one to Configuration Server

Once it's done with persons it continues with Agent Groups
- reads ag.txt file
- pulls AG from configserver
- look for workbin related options in its interaction-workspace section
- if option is found then utility adjusts corresponding property by assigning a new value
- puts AG into the list for following processing
- once all AG objects are modified it takes the list of modified AG objects and submits AG objects one by one to Configuration Server 
