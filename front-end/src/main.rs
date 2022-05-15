mod server_api;
mod utils;

use gloo::net::websocket::Message;
use server_api::{RigelServerMessage, ServerApi};
use utils::value_by_id;
use yew::{html, Callback, Component, FocusEvent, Html, MouseEvent, Properties};

use log::*;

enum AppMsg {
    LoggedIn(ServerApi),
    Msg(String),
    WsMessage(RigelServerMessage),
    CreateNewGame,
}

struct App {
    api: Option<ServerApi>,
    current_screen: Screen,
    games: Vec<String>,
}

// FIXME: disable login button after the first press
// FIXME: add yew router
enum Screen {
    Login,
    ListGames,
}

impl Component for App {
    type Message = AppMsg;

    type Properties = ();

    fn create(_ctx: &yew::Context<Self>) -> Self {
        App {
            api: None,
            current_screen: Screen::Login,
            games: vec![],
        }
    }

    fn update(&mut self, ctx: &yew::Context<Self>, msg: Self::Message) -> bool {
        match msg {
            AppMsg::LoggedIn(mut api) => {
                api.list_games();
                self.api = Some(api);
                self.current_screen = Screen::ListGames;
                true
            }

            AppMsg::Msg(m) => {
                info!("{m}");
                false
            }
            AppMsg::WsMessage(m) => {
                match m {
                    RigelServerMessage::GamesListResponse { games } => self.games = games,
                    RigelServerMessage::NewGameCreated { gameId } => self.games.push(gameId),
                    _ => info!("Server says {m:?}"),
                }
                true
            }
            AppMsg::CreateNewGame => {
                self.api.as_mut().unwrap().new_game();
                false
            }
        }
    }

    fn view(&self, ctx: &yew::Context<Self>) -> Html {
        match self.current_screen {
            Screen::Login => {
                let onmessage = ctx.link().callback(AppMsg::WsMessage);
                let onclick = ctx.link().callback_future_once(move |_e| {
                    // FIXME: use node refs
                    let login = value_by_id("username").unwrap();
                    let password = value_by_id("pwd").unwrap();
                    async move {
                        let api = ServerApi::login(&login, &password, onmessage).await;
                        AppMsg::LoggedIn(api.unwrap())
                    }
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
            Screen::ListGames => {
                // let games  = self.api.unwrap().list_games()
                // FIXME: new component will have non-optional api
                let newgame = ctx.link().callback(|_e| AppMsg::CreateNewGame);
                html! {
                    <>
                    <ul>
                        if self.games.is_empty() {
                            <h2>{ "No games"}</h2>
                        } else {
                            <ul>
                            { for self.games.iter().map(|g| html!{<li>{g}</li>}) }
                            </ul>
                        }
                    </ul>
                    <button onclick={newgame}>{ "New game" }</button>
                    </>
                }
            }
        }
        // if let Some(api) = &self.api {
        //     html!(
        //         <h1>{"Playing!"}</h1>
        //     )
        // } else {

        // }
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
