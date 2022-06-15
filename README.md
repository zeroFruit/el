# el 

[![ci](https://github.com/zeroFruit/el/actions/workflows/ci.yaml/badge.svg)](https://github.com/zeroFruit/el/actions/workflows/ci.yaml)

**el** is a minimal reactive network application framework without any dependencies.

## Documentations

You can read about the detail documentations on the [`docs/`](./docs)

## Examples

**el** is tested on **Java8, Java11**.

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


## Milestone

- [x] Implement EventLoop and Promise concept
- [ ] [Enabling local transport](https://github.com/zeroFruit/el/milestone/1)
- [ ] [Support read/write data via Channel](https://github.com/zeroFruit/el/milestone/2)
- [ ] [Implement NIO based Channel](https://github.com/zeroFruit/el/milestone/3)

