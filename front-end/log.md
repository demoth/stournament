# Dev log

issues:

- state that is modified from the ws-listener coroutine and different components need to subscribe to the changes of this state
- api is shared between different components and they make requests which mutate api's state.

I'll try to solve both of these using yewdux.

...

`yewdux` requires the state to be cloneable, I'll probably need the usual `Rc<Mutex<_>>`

**OR** I just save the api in the main component and utilize the inter-component message system.
The game state and subscription is still managed by yewdux.

The issue currently is with the "login" scenario, where the code needs to first make some asyncronous requests, and after that write back the websocket.

I'll revert to "login" being an async function that returns the fully-connected ServerApi object. Later the "ServerApi" will become "ServerEventApi", since most of the logic will switch to a request-response model, which is easy to implement with gloo requests.

Now the problem is calling api methods from other components.
This requires providing necessary callbacks from the root component, which bloats the view function of the root component too much.
I'll try storing `Scope<App>` in yewdux state, so the children can easily schedule callbacks to the root element.

Fail: `Scope<App>` does not implement PartialEq, which is required for yewdux State.
What if instead I use yewdux state to pass messages to the root component?

I failed to realize that I just need a single callback, which takes an `ClientRequest` and produces `AppMsg::ClientRequest(ClientRequest)`. No yewdux required (for now).
I can pass copies of this callback to all the sub-components and this will be a good step towards the MVP.
