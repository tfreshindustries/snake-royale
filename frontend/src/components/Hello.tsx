import * as React from 'react';
import { royale } from '../proto/model';
import './Hello.css';

export interface IProps {
  name: string;
  enthusiasmLevel?: number;
}

function Hello({ name, enthusiasmLevel = 1 }: IProps) {
  if (enthusiasmLevel <= 0) {
    throw new Error('You could be a little more enthusiastic. :D');
  }

  sendJoinRequest();

  return (
    <div className="hello">
      <div className="greeting">
        Hello {name + getExclamationMarks(enthusiasmLevel)}
      </div>
    </div>
  );
}

export default Hello;

// helpers

function sendJoinRequest() {
  const msg = new royale.JoinRequest({playerName: 'sam'})
  return msg;
}

function getExclamationMarks(numChars: number) {
  return Array(numChars + 1).join('!');
}
