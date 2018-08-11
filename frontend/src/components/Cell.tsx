import * as React from 'react';

export interface ICell {
    id: number;
}

const Cell = ({ id }: ICell) => <span className={`cell player${id}`}>{id}</span>;

export default Cell;