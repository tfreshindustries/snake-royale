import * as React from 'react';

import Cell from './Cell';

export interface IGrid {
    matrix: React.CSSProperties[][];
}

const Grid = ({ matrix }: IGrid) => (
    <div className="container">
        <div className="grid">
            {matrix.map((row, i) =>
                <div key={i} className="row">
                    {row.map((_, j) => <Cell key={j} style={matrix[i][j]} />)}
                </div>
            )}
        </div>
    </div>
);

export default Grid;
