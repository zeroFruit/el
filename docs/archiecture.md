# Architecture

## EventLoop

A EventLoop represents the runner for running asynchronous computation. One of implementations is `SingleThreadEventLoop`. In the case of `SingleThreadEventLoop`, it manages its tasks using queues.

The client can use `SingleThreadEventLoop` APIs (`submit()`, `schedule()`) to request to run asynchronous task. Internally `SingleThreadEventLoop` manages several queues. One is `ScheduldTaskQueue`, when the client schedule the task to be run after some amount of time, `SingleThreadEventLoop` uses `ScheduldTaskQueue` to manage scheduled tasks. When the scheduled task is the time to be run, `SingleThreadEventLoop` place the task to the `RunnableQueue`, the second queue it manages. The tasks on the `RunnableQueue` now can be dequeue using `takeTask()`. The responsibility to run those retrieved tasks is on the client side.

![single-thread-eventloop-diagram](../images/docs-arch-single-thread-eventloop-diagram.png)

## Promise

A Promise is a placeholder for the result of asynchronous computation. One that runs asynchronous computation can return the Promise to the client for the result of request. On the client side, it can wait until Promise to be set result or the error.

A PromiseListener can be registered to the Promise, once it register, it can receive the Promise when it update its state.

![promise-diagram](../images/docs-arch-promise-diagram.png)

# Component Relation

![component-diagram](../images/docs-arch-component-diagram.png)

