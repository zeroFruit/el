# el 

[![ci](https://github.com/zeroFruit/el/actions/workflows/ci.yaml/badge.svg)](https://github.com/zeroFruit/el/actions/workflows/ci.yaml)

**el** is a minimal reactive network application framework. This is WIP project. You can see the progress one the Milestone section.



## Run example code on your machine

One of the way to see how el EventLoop work, you can run example codes on the `example` package. First you need to clone this project.

```bash
git clone https://github.com/zerofruit/el
cd el
```

There're two example code now: `io.el.example.executor.TaskExecutor`, `io.el.example.scheduler.TaskExecuteScheduler`.  You need to update `example/build.gradle` file to run example code.

```groovy
// example/build.gradle

...
ext {
    javaMainClass = "io.el.example.executor.TaskExecutor" // or "io.el.example.scheduler.TaskExecuteScheduler"
}
...
```

Then run following command on the root path of this project.

```bash
./gradlew :example:run
```

**el** is tested on **Java8, Java11**.



## Milestone

- ~~Implement EventLoop and Promise concept~~
- Add NIO network stack.
- Add Reactor component on it.



## Documentations

- [Architecture and Concept](./docs/archiecture.md) : You can find the concept of the **el**'s main component, and how it is defined.

