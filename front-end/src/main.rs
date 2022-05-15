mod game_list;
mod login;
mod server_api;
mod utils;

use std::{cell::RefCell, rc::Rc};

use crate::game_list::GameList;
use crate::login::Login;
use log::*;
use server_api::{RigelServerMessage, ServerApi};
use utils::value_by_id;
use yew::prelude::*;
use yew_router::prelude::*;

enum AppMsg {
    LoggedIn(ServerApi),
    WsMessage(RigelServerMessage),
}

struct App {
    api: Option<Rc<RefCell<ServerApi>>>,
    current_screen: Screen,
    games: Vec<String>,
}

// FIXME: disable login button after the first press
// FIXME: add yew router
enum Screen {
    Login,
    ListGames,
}

#[derive(Clone, Routable, PartialEq)]
enum Route {
    #[at("/")]
    Home,
    #[at("/login")]
    Login,
    #[at("/games")]
    GamesList,
    #[not_found]
    #[at("/404")]
    NotFound,
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
                self.api = Some(Rc::new(RefCell::new(api)));
                self.current_screen = Screen::ListGames;
                true
            }

            AppMsg::WsMessage(m) => {
                match m {
                    RigelServerMessage::GamesListResponse { games } => self.games = games,
                    RigelServerMessage::NewGameCreated { gameId } => self.games.push(gameId),
                    _ => info!("Server says {m:?}"),
                }
                true
            } // AppMsg::CreateNewGame => {
              //     self.api.as_mut().unwrap().new_game();
              //     false
              // }
        }
    }

    fn view(&self, ctx: &yew::Context<Self>) -> Html {
        // match self.current_screen {
        //     Screen::Login => {

        //     }
        //     Screen::ListGames => {
        //         // let games  = self.api.unwrap().list_games()
        //         // FIXME: new component will have non-optional api
        //         let newgame = ctx.link().callback(|_e| AppMsg::CreateNewGame);
        //         html! {
        //             <>
        //             <ul>
        //                 if self.games.is_empty() {
        //                     <h2>{ "No games"}</h2>
        //                 } else {
        //                     <ul>
        //                     { for self.games.iter().map(|g| html!{<li>{g}</li>}) }
        //                     </ul>
        //                 }
        //             </ul>
        //             <button onclick={newgame}>{ "New game" }</button>
        //             </>
        //         }
        //     }
        // };
        // if let Some(api) = &self.api {
        //     html!(
        //         <h1>{"Playing!"}</h1>
        //     )
        // } else {

        // }

        let link = ctx.link().clone();
        let link2 = link.clone();
        let api = self.api.as_ref().map(Rc::clone);
        let gotologin = Callback::from(move |_| link2.history().unwrap().push(Route::Login));

        let switch = move |route: &Route| -> Html {
            let onmessage = link.callback(AppMsg::WsMessage);
            let onlogin = link.callback_future(move |_e| {
                let onmessage = onmessage.clone();
                // FIXME: use node refs
                let login = value_by_id("username").unwrap();
                let password = value_by_id("pwd").unwrap();
                async move {
                    let api = ServerApi::login(&login, &password, onmessage).await;
                    AppMsg::LoggedIn(api.unwrap())
                }
            });
            match route {
                Route::Home => html! { <><h1> {"Home"} </h1> <button onclick={gotologin.clone()} /> </>},
                Route::Login => html! {<Login onlogin={onlogin} />},
                Route::GamesList => {
                    if let Some(api) = &api {
                        html! { <GameList api={Rc::clone(api)} />}
                    } else {
                        html! { "Not logged in"}
                    }
                }
                Route::NotFound => html!("Not found"),
            }
        };

        html! {
            <BrowserRouter>
                <Switch<Route> render={Switch::render(switch)} />
            </BrowserRouter>
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
