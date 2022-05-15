use std::{cell::RefCell, rc::Rc};

use yew::prelude::*;

use crate::server_api::ServerApi;

#[derive(Properties, PartialEq)]
pub struct Props {
    pub api: Rc<RefCell<ServerApi>>
}

#[function_component(GameList)]
pub fn game_list(props: &Props) -> Html {
    html! { {"Games"} }
}
