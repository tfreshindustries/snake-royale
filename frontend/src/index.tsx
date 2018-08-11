import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Grid from './components/Grid';
import './index.css';
import registerServiceWorker from './registerServiceWorker';
import { royale } from './proto/model';

const socket = new WebSocket('ws://localhost:12345/ws');
socket.binaryType = 'arraybuffer';

socket.addEventListener('open', function (event) {
  const joinRequest = new royale.JoinRequest({ playerName: 'sam2' })
  const clientEvent = new royale.ClientEvent({ joinRequest: joinRequest })
  socket.send(royale.ClientEvent.encode(clientEvent).finish());
});

socket.addEventListener('message', function (event) {
  var bytearray = new Uint8Array(event.data);
  const serverEvent = royale.ServerEvent.decode(bytearray);
  switch (serverEvent.event) {

    case "gameState": {
      console.log("received GameState")
      console.log(serverEvent.gameState)
      break;
    }

    case "joinResponse": {
      console.log("received JoinResponse")
      console.log(serverEvent.joinResponse)
      break;
    }

  }
});

ReactDOM.render(
  <Grid matrix={ Array(50).fill(Array(50).fill(0)) } />,
  document.getElementById('root') as HTMLElement
);

registerServiceWorker();
