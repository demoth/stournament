mod server_api;
mod utils;

use gloo::net::websocket::Message;
use server_api::ServerApi;
use utils::value_by_id;
use yew::{html, Callback, Component, FocusEvent, Html, MouseEvent, Properties};

use log::*;

enum AppMsg {
    LogInStarted,
    LoggedIn(ServerApi),
    Msg(String),
    WsMessage(Message),
}

struct App {
    api: Option<ServerApi>,
    logging_in: bool,
}

impl Component for App {
    type Message = AppMsg;

    type Properties = ();

    fn create(_ctx: &yew::Context<Self>) -> Self {
        App {
            api: None,
            logging_in: false,
        }
    }

    fn update(&mut self, ctx: &yew::Context<Self>, msg: Self::Message) -> bool {
        match msg {
            AppMsg::LoggedIn(mut api) => {
                api.list_games();
                self.api = Some(api);
                true
            }

            AppMsg::Msg(m) => {
                info!("{m}");
                false
            }
            AppMsg::WsMessage(m) => {
                info!("Server says {m:?}");
                true
            }
            AppMsg::LogInStarted => {
                self.logging_in = true;
                true
            }
        }
    }

    fn view(&self, ctx: &yew::Context<Self>) -> Html {
        if let Some(api) = &self.api {
            html!(
                <h1>{"Playing!"}</h1>
            )
        } else {
            if self.logging_in {
                return html!(
                    <h1>{"Logging in..."}</h1>
                );
            }
            let onmessage = ctx.link().callback(AppMsg::WsMessage);
            let onclick = ctx.link().callback_future_once(|_e| async {
                let login = value_by_id("username").unwrap();
                let password = value_by_id("pwd").unwrap();
                let api = ServerApi::login(&login, &password, onmessage).await;
                AppMsg::LoggedIn(api.unwrap())
            });
            html! {
                <>
                <label for="username">{ "Username:" }</label><br />
                <input type="text" id="username" name="username" /><br />
                <label for="pwd">{ "Password:" }</label><br />
                <input type="password" id="pwd" name="pwd" /><br />
                <input type="submit" value="Login" onclick={onclick}/>
                </>
            }
        }
    }
}

#[derive(Properties, PartialEq)]
struct ConnectionProps {
    jwt: String,
}

fn main() {
    std::panic::set_hook(Box::new(console_error_panic_hook::hook));
    wasm_logger::init(wasm_logger::Config::default());
    yew::start_app::<App>();
}
