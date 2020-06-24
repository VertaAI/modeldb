const matchBy = <D extends Record<any, any>, Prop extends keyof D>(
  data: D,
  prop: Prop
): (<Result>(
  cases: { [K in D[Prop]]: (data: Extract<D, Record<Prop, K>>) => Result }
) => Result) => {
  return cases => {
    return cases[data[prop]](data as any);
  };
};

export default matchBy;
