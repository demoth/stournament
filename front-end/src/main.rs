mod server_api;

use server_api::LoginResponse;
use yew::{function_component, html, use_state, Html, Callback, Component};

use log::*;
use serde_json::json;
use web_sys::FocusEvent;
use yew_hooks::use_async;

enum Status {
    NotLoggedIn,
    LoggedIn { jwt: String },
}

struct App;

impl Component for App {
    type Message = ();

    type Properties = ();

    fn create(ctx: &yew::Context<Self>) -> Self {
        App
    }

    fn view(&self, ctx: &yew::Context<Self>) -> Html {
        let onsubmit = Callback::from(|focus_event: FocusEvent| {
            focus_event.prevent_default();
            
            wasm_bindgen_futures::spawn_local(login("daniil", "hello"));
            
        });
        html! {
            <form onsubmit={onsubmit}>
            <label for="username">{ "Username:" }</label><br />
            <input type="text" id="username" name="username" /><br />
            <label for="pwd">{ "Password:" }</label><br />
            <input type="password" id="pwd" name="pwd" /><br />
            <input type="submit"/>
          </form>
        }
    }
}


async fn login(username: &str, password: &str) {
    let response: LoginResponse = reqwest::Client::new()
        .post("http://localhost:8080/login")
        .json(&json!({
            "name": "daniil",
            "password": "world"
        }))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();

    info!("{response:?}");
}
fn main() {
    std::panic::set_hook(Box::new(console_error_panic_hook::hook));
    wasm_logger::init(wasm_logger::Config::default());
    yew::start_app::<App>();
}
