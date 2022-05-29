use crate::{
    game_list::GameList,
    login::Login,
    server_api::{RigelServerMessage, ServerApi},
};
use futures::lock::Mutex;
use log::*;
use std::rc::Rc;
use wasm_bindgen_futures::spawn_local;
use yew::prelude::*;
use yew_router::prelude::*;

#[derive(Clone, Routable, PartialEq)]
pub enum Route {
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

#[derive(Debug)]
pub enum ClientRequest {
    LoginRequest { login: String, password: String },
    ListGames,
    CreateGame,
}

#[derive(Debug)]
pub enum GlobalMessage {
    ClientRequest(ClientRequest),
    LoggedIn(ServerApi),
    GetGamesList,
    ServerMessage(RigelServerMessage),
}

pub struct App {
    api: Option<ServerApi>,
}

impl Component for App {
    type Message = GlobalMessage;
    type Properties = ();

    fn create(ctx: &Context<Self>) -> Self {
        App { api: None }
    }

    fn update(&mut self, ctx: &Context<Self>, msg: Self::Message) -> bool {
        match msg {
            GlobalMessage::ClientRequest(ClientRequest::LoginRequest { login, password }) => {
                let onmsg = ctx.link().callback(GlobalMessage::ServerMessage);
                let onlogin = ctx.link().callback(GlobalMessage::LoggedIn);

                spawn_local(async move {
                    let api = ServerApi::login(&login, &password, onmsg).await.unwrap();
                    onlogin.emit(api);
                })
            }
            GlobalMessage::LoggedIn(api) => self.api = Some(api),
            // FIXME: make serveApi accept ClientRequest instead of separate methods
            GlobalMessage::ClientRequest(ClientRequest::ListGames) => {
                self.api.as_mut().unwrap().list_games()
            }
            GlobalMessage::ClientRequest(ClientRequest::CreateGame) => {
                self.api.as_mut().unwrap().new_game()
            }
            GlobalMessage::GetGamesList => self.api.as_mut().unwrap().list_games(),
            GlobalMessage::ServerMessage(sr) => {
                info!("{sr:?}");
            }
        }
        true
    }

    fn view(&self, ctx: &Context<Self>) -> Html {
        let client_request = ctx.link().callback(GlobalMessage::ClientRequest);

        let switch = move |route: &Route| match route {
            Route::Login | Route::Home => html! {<Login client_request = {client_request.clone()}/>},
            Route::GamesList => html! { <GameList client_request = {client_request.clone()} />},
            Route::NotFound => html!("Not found"),
        };
        html! {
            <BrowserRouter>
                <Switch<Route> render={Switch::render(switch)} />
            </BrowserRouter>
        }
    }
}
