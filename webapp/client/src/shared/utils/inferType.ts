const inferType = <Union extends string | number>() => <
  T,
  Default extends Union | undefined | null
>(
  preds: Record<Union, (data: T) => Boolean>,
  def: Default,
  data: T
): Union | Default => {
  return (Object.entries<(data: T) => Boolean>(preds).filter(([_, pred]) =>
    pred(data)
  )[0][0] || def) as Union | Default;
};

export default inferType;
