use crate::app::{App, ClientRequest, Route};
use yew::{html::Scope, prelude::*};
use yew_router::prelude::Link;

use crate::utils::value_by_id;

#[derive(Properties, PartialEq)]
pub struct Props {
    pub client_request: Callback<ClientRequest>,
}

#[function_component(Login)]
pub fn login(props: &Props) -> Html {
    let callback = props.client_request.clone();
    let onclick = Callback::once(move |_e| {
        let login = value_by_id("username").unwrap();
        let password = value_by_id("pwd").unwrap();

        callback.emit(ClientRequest::LoginRequest { login, password });
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
