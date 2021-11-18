# trackline-simplifier
The trackline-simplifier creates a simplified geometry from a source of longitude, latitude, and timestamped coordinates.

## Using Library In Your Project
TODO

## Runtime Requirements
1. Java 8

## Building From Source
Maven 3.6.0+ is required.
```bash
mvn clean install
```

## Release from trunk (new branch)
###Create branch
```bash
mvn release:branch -DbranchName=2.1 -DdevelopmentVersion=2.2.0-SNAPSHOT
```
OR
```bash
mvn release:branch -DbranchName=2.1 -DdevelopmentVersion=2.2.0-SNAPSHOT -DdryRun=true
mvn release:clean
```
### Release
See Release from branch to do a non snapshot release

## Release from branch (patch / RC release)
### Prepare release
```bash
mvn release:prepare -Dtag=v2.1.0 -DreleaseVersion=2.1.0 -DdevelopmentVersion=2.1.1-SNAPSHOT
```
OR
```bash
mvn release:prepare -Dtag=v2.1.0 -DreleaseVersion=2.1.0 -DdevelopmentVersion=2.1.1-SNAPSHOT -DdryRun=true
mvn release:clean
```

If you mess up, run:
```bash
mvn release:clean
```
and try again

### Release
```bash
mvn release:perform
```
OR
```bash
mvn release:perform -DdryRun=true
mvn release:clean
```

If you mess up, run:
```bash
mvn release:rollback
```

## More Info
https://maven.apache.org/maven-release/maven-release-plugin/index.html