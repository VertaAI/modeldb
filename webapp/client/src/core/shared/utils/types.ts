export type URL = string;
export type Milliseconds = number;
export type Timestamp = Milliseconds;
export type Markdown = string;

export type RecordValues<R extends Record<any, any>> = R[keyof R];

export type NonFunctionPropertyNames<T> = {
  [K in keyof T]: T[K] extends Function ? never : K
}[keyof T];

export type DeepPartial<T> = T extends any[]
  ? IDeepPartialArray<T[number]>
  : T extends object
  ? DeepPartialObject<T>
  : T;
export interface IDeepPartialArray<T> extends Array<DeepPartial<T>> {}
export type DeepPartialObject<T> = {
  readonly [P in NonFunctionPropertyNames<T>]?: DeepPartial<T[P]>
};

export type MapRecord<
  T extends Record<keyof R, any>,
  R extends Record<any, any>
> = { [K in keyof R]: T[K] };

export type RecordFromUnion<
  T extends string | number,
  R extends Record<T, any>
> = { [K in T]: R[K] };

export type ArgumentTypes<F extends Function> = F extends (
  ...args: infer A
) => any
  ? A
  : never;

export type FirstArgument<T extends Function> = ArgumentTypes<T>[0];

export type PromiseValue<T> = T extends PromiseLike<infer U> ? U : T;
