import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Grid from './components/Grid';
import './index.css';
import registerServiceWorker from './registerServiceWorker';

ReactDOM.render(
  <Grid matrix={ Array(50).fill(Array(50).fill(0)) } />,
  document.getElementById('root') as HTMLElement
);

registerServiceWorker();
