export type Brand<K, T, S extends symbol> = K & {
  __type: T;
  readonly __symbol: S;
};
export type BrandSymbol<K extends Brand<any, any, any>> = K['__symbol'];
