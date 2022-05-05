use yew::{function_component, html};

#[function_component(App)]
fn app() -> yew::Html {
    html! {
        <h1>{ "Hi!" }</h1>
    }
}

fn main() {
    yew::start_app::<App>();
}
