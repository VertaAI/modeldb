const exhaustiveStringTuple = <T extends string>() => <L extends T[]>(
  ...x: L &
    ([T] extends [L[number]]
      ? L
      : [Error, 'You are missing ', Exclude<T, L[number]>])
) => x;

export default exhaustiveStringTuple;
