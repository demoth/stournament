#![allow(non_snake_case, non_camel_case_types)]

use serde::Deserialize;

#[derive(Deserialize)]
#[allow(non_camel_case_types)]
pub enum GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won,
}

#[derive(Deserialize)]
#[serde(tag = "_type")]
pub enum Message {
    #[serde(rename = "status")]
    GameStatusUpdate { newStatus: GameSessionStatus },
}

#[derive(Deserialize, Clone, Debug)]
pub struct LoginResponse {
    jwt: String,
}
