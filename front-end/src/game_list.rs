use wasm_bindgen_futures::spawn_local;
use yew::prelude::*;

use crate::app::ClientRequest;

#[derive(Properties, PartialEq)]
pub struct Props {
    pub client_request: Callback<ClientRequest>,
}

#[function_component(GameList)]
pub fn game_list(props: &Props) -> Html {
    let games = vec![];
    let request = props.client_request.clone();
    let callback = move |_e| request.emit(ClientRequest::CreateGame);

    html! {
        <>
        <button onclick={ callback }> { "New game" } </button><br />
        if games.is_empty() {
            {"No games"}
        } else {
            <ul> {
                for games.iter().map(|g: &String| html!{
                    <li> {g} </li>
                })
            } </ul>
        }
        </>
    }
}
