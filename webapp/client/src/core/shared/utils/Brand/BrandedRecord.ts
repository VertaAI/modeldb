import { Brand, BrandSymbol } from './Brand';

export type BrandedRecord<K extends Brand<any, any, any>, V> = Record<
  BrandSymbol<K>,
  V
>;
export const getBrandedRecordKey = <T extends Brand<any, any, any>>(
  key: T
): BrandSymbol<T> => {
  return key as any;
};
export const makeEmptyBrandedRecord = <
  T extends BrandedRecord<any, any>
>(): T => {
  return {} as any;
};

export const makeGetBrandedRecordKey = <
  T extends string | number | symbol,
  B extends string | number | symbol
>(
  f: (key1: T) => B
) => <Record extends BrandedRecord<B, any>>(key2: T) => {
  return getBrandedRecordKey<Record>(f(key2) as any);
};
