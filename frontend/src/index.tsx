import * as React from 'react';
import * as ReactDOM from 'react-dom';

import Grid from './components/Grid';
import registerServiceWorker from './registerServiceWorker';
import { royale } from './proto/model';

import './index.css';

////////////////////
// INITIALIZATION //
////////////////////

const boardHeight = 60;
const boardWidth = 100;
const serverURL = 'ws://localhost:12345/ws';
const defaultStyle = {
    backgroundColor: 'white'
};

// WEB SOCKET

let socket = new WebSocket(serverURL);

socket.binaryType = 'arraybuffer';
socket.onopen = (event) => sendJoinRequest

////////////////////
// GAME COMPONENT //
////////////////////

interface IGameProps { }

interface IGameState {
    matrix: React.CSSProperties[][]
}

class Game extends React.Component<IGameProps, IGameState> {

    constructor(props: IGameProps) {
        super(props);
        this.state = {
            matrix: makeEmptyBoard(boardHeight, boardWidth)
        };
    }

    handleGameState(gameState: royale.GameState) {
        console.log("TODO: handle game state");
    }

    handleJoinResponse(joinResponse: royale.JoinResponse) {
        console.log("TODO: handle join response");
    }

    handleServerEvent(serverEvent: royale.ServerEvent) {
        switch (serverEvent.event) {
            case "gameState": {
                this.handleGameState(serverEvent.gameState as royale.GameState);
                break;
            }
            case "joinResponse": {
                this.handleJoinResponse(serverEvent.joinResponse as royale.JoinResponse);
                break;
            }
        }
    }

    componentDidMount() {
        socket.onmessage = (event) => {
            let bytearray = new Uint8Array(event.data);
            let serverEvent = royale.ServerEvent.decode(bytearray);
            this.handleServerEvent(serverEvent);
        }
    }

    render() {
        return <Grid matrix={this.state.matrix} />
    }

}

export default Game;

//////////////
// MOVEMENT //
//////////////

function sendMoveRequest(direction: royale.Direction) {
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

////////////////////
// HELPER METHODS //
////////////////////

function makeEmptyBoard(height: number, width: number): React.CSSProperties[][] {

    var board: React.CSSProperties[][] = new Array(height);
    for (var i = 0; i < height; i++) {
        board[i] = new Array(width).fill(defaultStyle);
    }

    return board;
}

function sendJoinRequest() {
    let name = prompt("Choose a name for your snake.", "");
    let joinRequest = new royale.JoinRequest({ playerName: name })
    let clientEvent = new royale.ClientEvent({ joinRequest: joinRequest })
    socket.send(royale.ClientEvent.encode(clientEvent).finish());
}

//////////
// MAIN //
//////////

ReactDOM.render(
    <Game />,
    document.getElementById('root') as HTMLElement
);

registerServiceWorker();
