export type RecordValues<R extends Record<any, any>> = R[keyof R];
