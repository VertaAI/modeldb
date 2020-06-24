const matchType = <Union extends string | number, Result>(
  matchers: Record<Union, () => Result>,
  type: Union
): Result => {
  return matchers[type]();
};

export default matchType;
