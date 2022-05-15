use crate::app::Route;
use wasm_bindgen_futures::spawn_local;
use yew::prelude::*;
use yew_router::prelude::Link;

use crate::{app::ApiContext, utils::value_by_id};

#[function_component(Login)]
pub fn login() -> Html {
    let ctx = use_context::<ApiContext>().unwrap();
    let onclick = Callback::once(move |e| {
        let login = value_by_id("username").unwrap();
        let password = value_by_id("pwd").unwrap();
        spawn_local(async move {
            ctx.api.lock().await.login(&login, &password).await.unwrap();
        })
    });
    html! {
        <>
        <label for="username">{ "Username:" }</label><br />
        <input type="text" id="username" name="username" /><br />
        <label for="pwd">{ "Password:" }</label><br />
        <input type="password" id="pwd" name="pwd" /><br />
        <input type="submit" onclick = {onclick} value="Login"/>
        <Link<Route> to={Route::GamesList}>{"Go to games"}</Link<Route>>
        </>

    }
}
