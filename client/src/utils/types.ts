export type URL = string;
export type Milliseconds = number;
export type Timestamp = Milliseconds;
export type RecordValues<R extends Record<any, any>> = R[keyof R];
