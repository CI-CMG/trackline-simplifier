# trackline-simplifier
The trackline-simplifier creates a simplified geometry from a source of longitude, latitude, and timestamped coordinates.

## Using Library In Your Project
```xml
<dependency>
    <groupId>io.github.ci-cmg</groupId>
    <artifactId>trackline-simplifier</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Runtime Requirements
- Java 8

## Building From Source
Maven 3.6.0+ is required.
```bash
mvn clean install
```

## Deployment via Github Actions
### Release from trunk (new branch)
1. Navigate to _"Actions"_ tab within trackline-simplifier repository
2. Select _"maven branch release"_ workflow
3. Select _"Run workflow"_ dropdown 
4. Ensure that this is set to _"Use workflow from: Branch: master"_
5. Select green _"Run workflow"_ button


### Release from branch (patch / RC release)
1. Navigate to _"Actions"_ tab within trackline-simplifier repository
2. Select _"maven tag release"_ workflow
3. Select _"Run workflow"_ dropdown
4. Ensure that this is set to use the workflow from the current release branch - Ex: _"Use workflow from: Branch: 2.2"_
5. Select green _"Run workflow"_ button


## More Info
https://maven.apache.org/maven-release/maven-release-plugin/index.html
https://docs.github.com/en/actions