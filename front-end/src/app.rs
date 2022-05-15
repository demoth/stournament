use crate::{
    game_list::GameList,
    login::Login,
    server_api::{RigelServerMessage, ServerApi},
};
use futures::lock::Mutex;
use log::*;
use std::rc::Rc;
use yew::prelude::*;
use yew_hooks::{use_list, UseListHandle};
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

#[derive(Clone)]
pub struct ApiContext {
    pub api: Rc<Mutex<ServerApi>>,
    pub games_list: UseListHandle<String>
}

// The context is singleton so it is equal to itself
impl PartialEq for ApiContext {
    fn eq(&self, other: &Self) -> bool {
        self.games_list == other.games_list
    }
}

fn switch(route: &Route) -> Html {
    match route {
        Route::Login => html! {<Login />},
        Route::GamesList | Route::Home => html! { <GameList />},
        Route::NotFound => html!("Not found"),
    }
}

#[function_component(App)]
pub fn app() -> Html {
    // FIXME: changing this does not notify the game_list component
    let games_list = use_list::<String>(vec![]);

    let onmessage = {
        let games_list = games_list.clone();
        Callback::from(move |m| {
            info!("Server says {m:?}");
            match m {
                RigelServerMessage::GameStatusUpdate { newStatus } => {
                    info!("New status {newStatus:?}")
                }
                RigelServerMessage::GamesListResponse { games } => games_list.set(games),
                RigelServerMessage::NewGameCreated { gameId } => games_list.push(gameId),
            };
        })
    };
    let api = ApiContext {
        api: Rc::new(Mutex::new(ServerApi::new(onmessage))),
        games_list,
    };
    html! {
        <ContextProvider<ApiContext> context = {api}>
            <BrowserRouter>
                <Switch<Route> render={Switch::render(switch)} />
            </BrowserRouter>
        </ContextProvider<ApiContext>>
    }
}
