import * as React from 'react';

export interface ICell {
    id: number;
}

const Cell = ({ id }: ICell) => <div className={`cell player${id}`} />;

export default Cell;