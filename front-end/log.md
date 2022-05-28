# Dev log

issues:

- state that is modified from the ws-listener coroutine and different components need to subscribe to the changes of this state
- api is shared between different components and they make requests which mutate api's state.

I'll try to solve both of these using yewdux.

...

`yewdux` requires the state to be cloneable, I'll probably need the usual `Rc<Mutex<_>>`

**OR** I just save the api in the main component and utilize the inter-component message system.
The game state and subscription is still managed by yewdux.
