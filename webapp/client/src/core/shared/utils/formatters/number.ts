export const withScientificNotationOrRounded = (num: number) => {
  if (isNaN(num)) {
    return num;
  }
  const exponential = parseInt(
    String(num.toExponential())
      .split('e')[1]
      .substring(1)
  );
  const sign = String(num.toExponential())
    .split('e')[1]
    .substring(0)[0];
  return sign === '-' && exponential > 4
    ? num.toExponential()
    : Math.round(num * 10000) / 10000;
};

export const numberRounded = (num: number) => {
  return Math.round(num * 10000) / 10000;
};
