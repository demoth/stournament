use std::rc::Rc;

use crate::{game_list::GameList, login::Login, server_api::ServerApi};
use futures::lock::Mutex;
use yew::prelude::*;
use yew_router::prelude::*;

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

#[derive(Clone)]
pub struct ApiContext {
    pub api: Rc<Mutex<ServerApi>>,
}

// The context is singleton so it is equal to itself
impl PartialEq for ApiContext {
    fn eq(&self, _other: &Self) -> bool {
        true
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
    let onmessage = Callback::from(|m| {});
    let api = ApiContext {
        api: Rc::new(Mutex::new(ServerApi::new(onmessage))),
    };
    html! {
        <ContextProvider<ApiContext> context = {api}>
            <BrowserRouter>
                <Switch<Route> render={Switch::render(switch)} />
            </BrowserRouter>
        </ContextProvider<ApiContext>>
    }
}
