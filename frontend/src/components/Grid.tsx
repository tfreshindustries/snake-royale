import * as React from 'react';

import Cell from './Cell';

export interface IGrid {
    matrix: number[][];
}

const Grid = ({ matrix }: IGrid) => (
    <div className="grid">
        {matrix.map((col, i) =>
            <div key={i} className="col">
                {col.map((row, j) => <Cell key={j} id={row} />)}
            </div>
        )}
    </div>
);

export default Grid;
