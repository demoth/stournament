use wasm_bindgen::JsCast;
use web_sys::HtmlInputElement;
use yew::{NodeRef, Html};

pub fn value_by_ref(node_ref: NodeRef) -> Option<String> {
    let element = node_ref.cast::<HtmlInputElement>()?;
    Some(element.value())
}
