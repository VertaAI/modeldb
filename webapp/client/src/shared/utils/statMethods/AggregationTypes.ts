export const listSum = (array: any) =>
  array.reduce((a: number, b: number) => a + b);
export const listAverage = (array: any) => listSum(array) / array.length;
export const listMedian = (array: any) => {
  array.sort((a: number, b: number) => a - b);
  const lowMiddle = Math.floor((array.length - 1) / 2);
  const highMiddle = Math.ceil((array.length - 1) / 2);
  return (array[lowMiddle] + array[highMiddle]) / 2;
};
export const listVariance = (array: any) => {
  const mean = listAverage(array);
  return listAverage(
    array.map((num: number) => {
      return Math.pow(num - mean, 2);
    })
  );
};
export const listStdev = (array: any) => Math.sqrt(listVariance(array));
export const listCount = (array: any) => array.length;
