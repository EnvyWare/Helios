# Helios [![Join our Discord](https://img.shields.io/discord/831966641586831431.svg?logo=discord&label=)](https://discord.envyware.co.uk) [![Developer Releases](https://maven.envyware.co.uk/api/badge/latest/releases/uk/co/envyware/helios?color=40c14a&prefix=v&name=API)]([https://jitpack.io/#Pixelmon-Development/API](https://maven.envyware.co.uk/#/releases/uk/co/envyware/helios)) [![GitHub](https://img.shields.io/github/license/EnvyWare/Helios)](https://opensource.org/license/mit)

Helios is a developer tool designed to improve the usability of the Builder pattern in Java.

## Installing
You can use the features listed below by installing the plugin from the IntelliJ marketplace which you can find [here](https://plugins.jetbrains.com/plugin/25124-helios).

## Features

### RequiredMethod annotation
The `@RequiredMethod` annotation should be added to the `build()` (or equivalent) method in your builder pattern.
This annotation will ensure that the methods specified in the value are called before the build method allowing you to 
provide a compile-time error if a required method is not called.

This improves the usability of the builder pattern as you no longer have to compile, and then run, your code
to find out that you have missed a required method call.

#### Example
```java
public class ExampleBuilder {
    
    private String name = null;
    private int age = -1;
    
    public ExampleBuilder() {}
    
    @RequiredMethod({"name", "age"})
    public ExampleBuilder build() {
        assert name != null;
        assert age != -1;
        
        return new ExampleBuilder();
    }
    
    public ExampleBuilder name(@NotNull String name) {
        assert name != null;
        
        this.name = name;
        return this;
    }
    
    public ExampleBuilder someUnimportantMethod() {
        return this;
    }
    
    public ExampleBuilder age(int age) {
        assert age > 0;
        
        this.age = age;
        return this;
    }
}

public class MainClass {
    
    public static void main(String[] args) {
        // This would be highlighted as an error as the `name()` method has not been called
        new ExampleBuilder()
                .age(10)
                .build();
        
        // This would not be highlighted as all required methods have been called
        new ExampleBuilder()
                .age(10)
                .name("Test")
                .build();
    }
    
}
```

## Getting Started

Note: if you want the syntax highlighting to work you need to install the IntelliJ plugin.


You can use the Helios library by adding the following to your build.gradle file repositories block:

```groovy
repositories {
    maven {
        url 'https://maven.envyware.co.uk'
    }
}
```

And then adding the following to your dependencies block:

```groovy
dependencies {
    implementation 'uk.co.envyware:helios:1.1-SNAPSHOT'
}
```
