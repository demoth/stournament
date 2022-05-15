use yew::{prelude::*};

use crate::App;


#[derive(Properties, PartialEq)]
pub struct Props {
    pub onlogin: Callback<MouseEvent>
}

#[function_component(Login)]
pub fn login(props: &Props) -> Html {
    let onlogin = props.onlogin.clone();
    html! {
        <>
        <label for="username">{ "Username:" }</label><br />
        <input type="text" id="username" name="username" /><br />
        <label for="pwd">{ "Password:" }</label><br />
        <input type="password" id="pwd" name="pwd" /><br />
        <input type="submit" value="Login" onclick={onlogin}/>
        </>
    }
}