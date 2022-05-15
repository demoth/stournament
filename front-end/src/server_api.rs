#![allow(non_snake_case, non_camel_case_types)]

use anyhow::format_err;
use futures::{
    channel::mpsc::{channel, Sender},
    stream::{SplitSink, SplitStream},
    SinkExt, StreamExt,
};
use gloo::net::websocket::{futures::WebSocket, Message as WsMessage, WebSocketError};
use log::*;
use serde::Deserialize;
use serde_json::json;
use wasm_bindgen_futures::spawn_local;
use yew::Callback;

#[derive(Deserialize, Debug)]
pub enum GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won,
}

#[derive(Deserialize, Debug)]
#[serde(tag = "_type")]
pub enum RigelServerMessage {
    #[serde(rename = "status")]
    GameStatusUpdate { newStatus: GameSessionStatus },
    #[serde(rename = "game_list")]
    GamesListResponse { games: Vec<String> },
    #[serde(rename = "new_game_created")]
    NewGameCreated { gameId: String },
}

#[derive(Deserialize, Debug)]
pub struct LoginResponse {
    pub jwt: String,
}

pub struct ServerApi {
    onmessage: Callback<RigelServerMessage>,
    sink: Option<Sender<WsMessage>>,
}

/// The server api is always the same
/// FIXME: This is needed to use api in props,
/// maybe better to use an agent and talk to it
impl PartialEq for ServerApi {
    fn eq(&self, other: &Self) -> bool {
        true
    }
}

impl ServerApi {
    pub async fn login(&mut self, username: &str, password: &str) -> anyhow::Result<()> {
        let onmessage = self.onmessage.clone();
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
        sink.send(WsMessage::Text(
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
                    Ok(m) => {
                        if let WsMessage::Text(text_message) = m {
                            let rigel_message: Result<RigelServerMessage, _> =
                                serde_json::from_str(&text_message);
                            match rigel_message {
                                Ok(m) => onmessage.emit(m),
                                Err(e) => error!("Cannot parse server message {e:?}"),
                            }
                        }
                    }
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

        self.sink = Some(tx);
        Ok(())
    }

    pub fn list_games(&mut self) {
        self.send_message(json!({
            "_type": "game_list"
        }))
    }

    pub fn new_game(&mut self) {
        self.send_message(json!({
            "_type": "new_game"
        }));
    }

    fn send_message(&mut self, message: serde_json::Value) {
        self.sink
            .as_mut()
            .unwrap()
            .try_send(WsMessage::Text(message.to_string()))
            .unwrap()
    }

    pub(crate) fn new(onmessage: Callback<RigelServerMessage>) -> Self {
        ServerApi {
            sink: None,
            onmessage,
        }
    }
}
