use wasm_bindgen::JsCast;
use web_sys::HtmlInputElement;

pub fn value_by_id(id: &str) -> Option<String> {
    let element = gloo::utils::document().get_element_by_id(id)?;
    let element = element.dyn_ref::<HtmlInputElement>()?;
    Some(element.value())
}
