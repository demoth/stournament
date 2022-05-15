use wasm_bindgen_futures::spawn_local;
use yew::prelude::*;
use yew_hooks::use_list;

use crate::app::ApiContext;

#[function_component(GameList)]
pub fn game_list() -> Html {
    let ctx = use_context::<ApiContext>().unwrap();
    let api = ctx.api.clone();
    spawn_local(async move { ctx.api.lock().await.list_games() });
    let games = ctx.games_list;

    html! {
        <>
        <button onclick={Callback::once(move |_|spawn_local(async move {
            api.lock().await.new_game()
        }))}> { "New game" } </button><br />
        if games.current().is_empty() {
            {"No games"}
        } else {
            <ul> {
                for games.current().iter().map(|g| html!{
                    <li> {g} </li>
                })
            } </ul>
        }
        </>
    }
}
