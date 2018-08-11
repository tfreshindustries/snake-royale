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

function sendMoveRequest (direction: royale.Direction) {
  console.log("sending request to move " + direction.toString());
  const moveRequest = new royale.MoveRequest({ direction: direction });
  const clientEvent = new royale.ClientEvent({ moveRequest: moveRequest });
  socket.send(royale.ClientEvent.encode(clientEvent).finish());
}

document.onkeydown = function (e) {
  e = e || window.event;
  switch (e.which || e.keyCode) {
    case 37: {
      sendMoveRequest(royale.Direction.LEFT);
      return false;
    }
    case 38: {
      sendMoveRequest(royale.Direction.UP);
      return false;
    }
    case 39: {
      sendMoveRequest(royale.Direction.RIGHT);
      return false;
    }
    case 40: {
      sendMoveRequest(royale.Direction.DOWN);
      return false;
    }
  }

  return false;
}

ReactDOM.render(
  <Grid matrix={Array(50).fill(Array(50).fill(0))} />,
  document.getElementById('root') as HTMLElement
);

registerServiceWorker();
