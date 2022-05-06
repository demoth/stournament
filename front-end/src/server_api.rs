#![allow(non_snake_case, non_camel_case_types)]

use anyhow::format_err;
use futures::{
    channel::mpsc::{channel, Sender},
    stream::{SplitSink, SplitStream},
    SinkExt, StreamExt,
};
use gloo::net::websocket::{futures::WebSocket, Message, WebSocketError};
use log::*;
use serde::Deserialize;
use serde_json::json;
use wasm_bindgen_futures::spawn_local;
use yew::Callback;

#[derive(Deserialize)]
pub enum GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won,
}

#[derive(Deserialize)]
#[serde(tag = "_type")]
pub enum RigelMessage {
    #[serde(rename = "status")]
    GameStatusUpdate { newStatus: GameSessionStatus },
}

#[derive(Deserialize, Clone, Debug)]
pub struct LoginResponse {
    pub jwt: String,
}

pub struct ServerApi {
    sink: Sender<Message>,
}

impl ServerApi {
    pub async fn login(
        username: &str,
        password: &str,
        onmessage: Callback<Message>,
    ) -> anyhow::Result<ServerApi> {
        let response = gloo::net::http::Request::post("http://localhost:8080/login")
            .json(&json!({
                "name": username,
                "password": password
            }))?
            .send()
            .await?;

        let response = response.json::<LoginResponse>().await?;
        let websocket = WebSocket::open("ws://localhost:8080/web-socket")
            .map_err(|e| format_err!("Error connecting to WS: {e}"))?;
        let (mut sink, mut source) = websocket.split();
        sink.send(Message::Text(
            json!({
                "_type": "jwt",
                "jwt": &format!("Bearer {}", response.jwt)
            })
            .to_string(),
        ))
        .await
        .map_err(|e| format_err!("Error sending to ws: {e}"))?;
        spawn_local(async move {
            while let Some(m) = source.next().await {
                match m {
                    Ok(m) => onmessage.emit(m),
                    Err(WebSocketError::ConnectionClose(e)) => info!("Closing ws {e:?}"),
                    Err(e) => panic!("{e:?}"),
                }
            }
        });

        let (tx, mut rx) = channel(50);
        spawn_local(async move {
            while let Some(m) = rx.next().await {
                sink.send(m).await.unwrap();
            }
        });

        Ok(ServerApi { sink: tx })
    }
    pub fn list_games(&mut self) {
        self.sink
            .try_send(Message::Text(
                json!(
                    {
                        "_type": "game_list"
                    }
                )
                .to_string(),
            ))
            .unwrap()
    }
}
