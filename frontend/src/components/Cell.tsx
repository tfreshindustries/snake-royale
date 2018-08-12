import * as React from 'react';

export interface ICell {
    style: React.CSSProperties;
}

const Cell = ({ style }: ICell) => {
    return (
        <div
            className={'cell'}
            style={style}
        />
    );
}

export default Cell;