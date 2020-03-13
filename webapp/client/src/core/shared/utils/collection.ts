import * as R from 'ramda';

export const getRandomItem = <T>(items: T[]): T => {
  return items[Math.floor(Math.random() * items.length)];
};

export const last = <T>(arr: T[]): T => arr[arr.length - 1];

export const mapObjWithKey = <T extends Record<any, any>, B>(
  f: (key: keyof T, value: T[keyof T]) => B,
  obj: T
): { [K in keyof T]: B } => {
  return R.fromPairs(
    R.toPairs(obj).map(([key, value]) => [key, f(key, value)])
  ) as any;
};
export const mapObj = <T extends Record<any, any>, B>(
  f: (value: T[keyof T]) => B,
  obj: T
): { [K in keyof T]: B } => {
  return R.fromPairs(
    R.toPairs(obj).map(([key, value]) => [key, f(value)])
  ) as any;
};

interface IKeyValuePair<V = string | number | Array<string | number>> {
  key: string | number;
  value: V;
}
const keyValuePairsToObj = (keyValuePairs: IKeyValuePair[]) =>
  R.fromPairs(keyValuePairs.map(({ key, value }) => [key, value] as any));
export const getKeyValuePairsDiff = (
  keyValuePairs1: IKeyValuePair[],
  keyValuePairs2: IKeyValuePair[]
) => {
  return getObjsPropsDiff(
    keyValuePairsToObj(keyValuePairs1),
    keyValuePairsToObj(keyValuePairs2)
  );
};
export const getObjsPropsDiffByPred = <T extends Record<any, any>>(
  diffPred: (
    key: keyof T,
    valueObj1: T[keyof T],
    valueObj2: T[keyof T]
  ) => boolean,
  obj1: T,
  obj2: T
): Record<keyof T, boolean> => {
  const objsKeys = Array.from(
    new Set([...Object.keys(obj1), ...Object.keys(obj2)])
  );
  return R.fromPairs(
    objsKeys.map(key => [key, diffPred(key, obj1[key], obj2[key])])
  ) as { [K in keyof T]: boolean };
};
export const getObjsPropsDiff = <T extends Record<any, any>>(
  obj1: T,
  obj2: T
) => {
  return getObjsPropsDiffByPred(
    (_, value1, value2) => !R.equals(value1, value2),
    obj1,
    obj2
  );
};

export const updateById = <T extends { id: string }>(
  f: (x: T) => T,
  id: string,
  array: T[]
): T[] => {
  return array.map(el => (el.id === id ? f(el) : el));
};

interface IObject {
  [key: string]: any;
}

export const mapToObject = (aMap: Map<string, IObject>) => {
  const obj: IObject = {};
  aMap.forEach((v: IObject, k: string) => {
    obj[k] = v;
  });
  return obj;
};

export const objectToMap = (obj: IObject) => {
  const mp: Map<string, IObject> = new Map();
  Object.keys(obj).forEach(k => {
    mp.set(k, obj[k]);
  });
  return mp;
};

export const upsert = <T extends { id: string }>(item: T, items: T[]): T[] => {
  return items.some(x => x.id === item.id)
    ? updateById(() => item, item.id, items)
    : items.concat(item);
};

export const renameObjectKey = <T extends Record<any, any>>(
  oldKey: keyof T,
  newKey: keyof T,
  obj: T
): T => {
  const { [oldKey]: _, ...objWithOldKey } = obj;
  return {
    ...objWithOldKey,
    [newKey]: obj[oldKey],
  } as T;
};
