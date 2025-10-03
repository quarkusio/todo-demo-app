# Agent protocol between the front end and the back end

The protocol is WebSocket based.
The URL is /todo-agent/{todoId} (might change and drop the todoId at some point)

## Protocol messages

The protocol payload will be JSON with the following format

```
{ "kind": "test",
"todoId": 123,
"payload": "Some String"
}
```

`payload` is optional unless explicitly mentioned, `kind` and `todoId` are mandatory

The kinds of messages are as follow:
* `initialize` : a message the client must send right after opening the websocket
* `cancel` : a message to send when the user clicks on `Cancel AI` ; after which the websocket connection is closed from the client side
* `activity_log` : message sent by the server to the client when an activity info is to be displayed. Must have a `payload` field, this is text sent from the server that is to be displayed in **grey**, these strings are tokens / short and need to be concatenated in the "activity" screen, the server is responsible for sending `\n\n` (in a separate message or appended) when a message made of several tokens ends.
* `agent_message` : message sent by the server to the client when a request to the user is made. Must have a `payload` field, this is text sent from the server that is to be displayed in **dark**, these strings are tokens / short strings and need to be concatenated in the "activity" screen, the server is responsible for sending `\n\n` (in a separate message or appended) when a message made of several tokens ends.
* `user_message` : message sent by the client to the server when a user hits send on a chat message. Must have a `payload` field. The whole chat message is sent as one message.

## UX and interaction

Here is the UX and interaction 
* the UI will have one popup but bigger
* it will allow you to edit or add a description and have `mark as done`, `delete` and `Do with AI` set of buttons
* when clicking on `Do with AI` the UI will expand a bit below to show a "chat UI" which will act as the activity window and that's when the connection with the websocket is initialized
* This activity shows logs from the server and so called agent requests (for context), this is when a user would send a chat message to the server
* the `do with AI` button is replaced by a `Cancel AI work` button