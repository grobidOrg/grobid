# Install a GROBID development environment

## Requirements

### Java Development Kit (JDK)

- **For building**: OpenJDK 21 or higher

!!! important "JDK Requirements"
    GROBID is developed using OpenJDK 21. 

#### Installing OpenJDK

**Linux (Ubuntu/Debian):**
```bash
# Ubuntu 22.04+ or Debian 12+
sudo apt update
sudo apt install openjdk-21-jdk
```

**Linux (CentOS/RHEL/Fedora):**
```bash
# For JDK 21
sudo dnf install java-21-openjdk-devel
```

**macOS (using Homebrew):**
```bash
# Install JDK 21
brew install openjdk@21

# Set JAVA_HOME (add to ~/.zshrc or ~/.bash_profile)
export JAVA_HOME=$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

#### Verifying Java Installation

```bash
# Check Java version
java -version

# Check JDK version (should show 21+ for building)
javac -version
```

## Getting the GROBID project source

### Latest stable release

The [latest stable release](https://github.com/grobidOrg/grobid#latest-version) of GROBID is version ```0.9.0``` which can be downloaded as follow: 
```bash
> wget https://github.com/grobidOrg/grobid/archive/0.9.0.zip
> unzip 0.9.0.zip
```

or using the [docker](Grobid-docker.md) container. 

### Current development version

The current development version is ```0.9.1-SNAPSHOT```, which can be downloaded from GitHub and built as follow:

Clone source code from github:
```bash
> git clone https://github.com/grobidOrg/grobid.git
```

Or download directly the zip file:
```bash
> wget https://github.com/grobidOrg/grobid/zipball/master
> unzip master
```

## Build GROBID from the source

!!! tip
    Please make sure that Grobid is installed in a path with no parent directories containing spaces.

### Build GROBID with Gradle 

The standard method for building GROBID is to use gradle. Under the main directory `grobid/`:
```bash
> ./gradlew clean build
```

### Building through a proxy

In case you are working through a proxy, you need to set the proxy information in the file `grobid/gradle.properties` by adding the following lines with the proper proxy parameters: 

```
systemProp.http.proxyHost=host
systemProp.http.proxyPort=port
systemProp.http.proxyUser=username
systemProp.http.proxyPassword=password
systemProp.https.proxyHost=host
systemProp.https.proxyPort=port
systemProp.https.proxyUser=username
systemProp.https.proxyPassword=password
```

## Use a built GROBID project

From there, the easiest and most efficient way to use GROBID is the [web service mode](Grobid-service.md).
You can also use the tool in [batch mode](Grobid-batch.md) or integrate it in your Java project via the [Java API](Grobid-java-library.md).

## Need Help?

If you encounter any issues during installation, check our [Troubleshooting and FAQ](Frequently-asked-questions.md) which covers:
- Platform-specific issues (macOS Apple Silicon, Windows, etc.)
- Native library problems
- Java version conflicts
- Memory and build issues

For Docker-based installation alternatives, see the [Docker documentation](Grobid-docker.md). 

