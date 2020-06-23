export interface IHistory<Data> {
    past: Data[];
    current: Data;
    future: Data[];
}

export const make = <Data>(data: Data): IHistory<Data> => {
    return {
        past: [],
        current: data,
        future: [],
    };
};

export const push = <Data>(data: Data, history: IHistory<Data>): IHistory<Data> => {
    return {
        past: [history.current].concat(history.past),
        current: data,
        future: [],
    };
}

export const back = <Data>(history: IHistory<Data>): [IHistory<Data>, Data?] => {
    if (isBackEnabled(history)) {
        const newCurrent = history.past[0];
        return [{
            past: history.past.slice(1),
            current: newCurrent,
            future: [history.current].concat(history.future),
        }, newCurrent];
    }
    return [history, undefined];
};

export const forward = <Data>(history: IHistory<Data>): [IHistory<Data>, Data?] =>  {
    if (isForwardEnabled(history)) {
        const newCurrent = history.future[0];
        return [
            {
                past: [history.current].concat(history.past),
                current: newCurrent,
                future: history.future.slice(1),
            },
            newCurrent,
        ];
    }
    return [history, undefined];
}

export const isBackEnabled = <Data>(history: IHistory<Data>) => history.past.length > 0; 

export const isForwardEnabled = <Data>(history: IHistory<Data>) => history.future.length > 0;

export const getBackItem = <Data>(history: IHistory<Data>) => history.past[0];

export const getForwardItem = <Data>(history: IHistory<Data>) => history.future[0];
