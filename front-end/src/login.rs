use wasm_bindgen_futures::spawn_local;
use yew::prelude::*;

use crate::{app::ApiContext, utils::value_by_ref};

#[function_component(Login)]
pub fn login() -> Html {
    let ctx = use_context::<ApiContext>().unwrap();
    let username = NodeRef::default();
    let password = NodeRef::default();
    let password_clone = password.clone();
    let username_clone = NodeRef::default();
    let onclick = Callback::once(move |e| {
        let login = value_by_ref(username_clone).unwrap();
        let password = value_by_ref(password_clone.clone()).unwrap();
        spawn_local(async move {
            ctx.api.lock().await.login(&login, &password).await.unwrap();
        })
    });
    html! {
        <>
        <label for="username">{ "Username:" }</label><br />
        <input type="text" ref={username} name="username" /><br />
        <label for="pwd">{ "Password:" }</label><br />
        <input type="password" ref={password} name="pwd" /><br />
        <input type="submit" onclick = {onclick} value="Login"/>
        </>
    }
}
