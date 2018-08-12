import * as React from 'react';
import * as ReactDOM from 'react-dom';

import Grid from './components/Grid';
import registerServiceWorker from './registerServiceWorker';
import { royale } from './proto/model';

import './index.css';

////////////////////
// INITIALIZATION //
////////////////////

// CONSTANTS

const defaultBoard: React.CSSProperties[][] = [[]];
const defaultProperties: React.CSSProperties = { backgroundColor: '#ffffff' };
const meProperties: React.CSSProperties = { backgroundColor: '#b300b3' }
const themProperties: React.CSSProperties = { backgroundColor: 'black' }
const foodProperties: React.CSSProperties = { backgroundColor: '#ff9933' }

const loc = window.location;
var serverURL = (loc.protocol === "https") ? "wss" : "ws";
serverURL += "://" + loc.host + "/ws"
console.log(serverURL)

let socket = new WebSocket(serverURL);

socket.binaryType = 'arraybuffer';
socket.onopen = (event) => sendJoinRequest();

////////////////////
// GAME COMPONENT //
////////////////////

interface IGameCompProps { }

interface IGameCompState {
    matrix: React.CSSProperties[][],
    playerId: number
}

class Game extends React.Component<IGameCompProps, IGameCompState> {

    constructor(props: IGameCompProps) {
        super(props);
        this.state = {
            matrix: defaultBoard,
            playerId: 0
        };
    }

    updateMatrixForPlayer(matrix: React.CSSProperties[][], player: royale.Player, point: royale.Point) {
        let isMe = player.playerId == this.state.playerId;
        let properties = isMe ? meProperties : themProperties;
        matrix[point.y][point.x] = properties;
    }

    updateMatrixForFood(matrix: React.CSSProperties[][], point: royale.Point) {
        matrix[point.y][point.x] = foodProperties;
    }

    handleGameState(gameState: royale.GameState) {
        var matrix = makeDefaultMatrix(gameState.boardHeight, gameState.boardWidth);

        // Update matrix for players
        let players = gameState.players as royale.Player[];
        players.forEach(player => {
            let points = player.occupies as royale.Point[];
            points.forEach(point => this.updateMatrixForPlayer(matrix, player, point));
        })

        // Update matrix for food
        let foods = gameState.food as royale.Point[];
        foods.forEach(food => { this.updateMatrixForFood(matrix, food); })

        this.setState({matrix: matrix});
    }

    handleJoinResponse(joinResponse: royale.JoinResponse) {
        let joinSuccess = joinResponse.joinSuccess as royale.JoinSuccess;
        this.setState({playerId: joinSuccess.playerId});
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

    return true;
}

////////////////////
// HELPER METHODS //
////////////////////

function sendJoinRequest() {
    let name = prompt("Choose a name for your snake.", "");
    let joinRequest = new royale.JoinRequest({ playerName: name })
    let clientEvent = new royale.ClientEvent({ joinRequest: joinRequest })
    socket.send(royale.ClientEvent.encode(clientEvent).finish());
}

function sendMoveRequest(direction: royale.Direction) {
    let moveRequest = new royale.MoveRequest({ direction: direction });
    let clientEvent = new royale.ClientEvent({ moveRequest: moveRequest });
    socket.send(royale.ClientEvent.encode(clientEvent).finish());
    console.log("request move " + direction.toString());
}

function makeDefaultMatrix(height: number, width: number) {
    var matrix: React.CSSProperties[][] = Array(height);
    for (var i = 0; i < height; i++) {
        matrix[i] = Array(width).fill(defaultProperties);
    }

    return matrix;
}

//////////
// MAIN //
//////////

ReactDOM.render(
    <Game />,
    document.getElementById('root') as HTMLElement
);

registerServiceWorker();
