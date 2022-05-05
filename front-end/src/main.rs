mod server_api;

use server_api::LoginResponse;
use yew::{
    function_component, html, html::Scope, use_state, Callback, Component, Html, Properties,
};

use log::*;
use serde_json::json;
use web_sys::FocusEvent;
use yew_hooks::{use_async, use_web_socket};

enum AppMsg {
    SuccessfulLogin { jwt: String },
}

struct App {
    jwt: Option<String>,
}

impl Component for App {
    type Message = AppMsg;

    type Properties = ();

    fn create(_ctx: &yew::Context<Self>) -> Self {
        App { jwt: None }
    }

    fn update(&mut self, _ctx: &yew::Context<Self>, msg: Self::Message) -> bool {
        match msg {
            AppMsg::SuccessfulLogin { jwt } => self.jwt = Some(jwt),
        }
        true
    }

    fn view(&self, ctx: &yew::Context<Self>) -> Html {
        let link = ctx.link().clone();
        let onsubmit = Callback::once(|focus_event: FocusEvent| {
            focus_event.prevent_default();

            wasm_bindgen_futures::spawn_local(login("daniil", "hello", link));
        });
        if let Some(jwt) = &self.jwt {
            html! {
                <Connection jwt={jwt.clone()} />
            }
        } else {
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
}

#[derive(Properties, PartialEq)]
struct ConnectionProps {
    jwt: String,
}

#[function_component(Connection)]
fn connection(props: &ConnectionProps) -> Html {
    let ws = use_web_socket("ws://localhost:8080/web-socket".to_string());
    ws.send(
        json!({
            "_type": "jwt",
            "jwt": &format!("Bearer {}", props.jwt)
        })
        .to_string(),
    );
    html!(
        <h2>{"connection"}</h2>
    )
}

async fn login(username: &str, password: &str, link: Scope<App>) {
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

    link.send_message(AppMsg::SuccessfulLogin { jwt: response.jwt });
}
fn main() {
    std::panic::set_hook(Box::new(console_error_panic_hook::hook));
    wasm_logger::init(wasm_logger::Config::default());
    yew::start_app::<App>();
}
