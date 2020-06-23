import * as R from 'ramda';

const sortArrayByAnotherArrayKeys = <T>(
  getKey: (item: T) => string,
  array1: T[],
  array2: T[]
): T[] => {
  if (array2.length === 0) {
    return R.sortBy(getKey, array1);
  }
  return R.sortWith(
    [
      R.ascend(item =>
        array2.find(array2Item => getKey(item) === getKey(array2Item)) ? 0 : 1
      ),
      R.ascend(getKey),
    ],
    array1
  );
};

export default sortArrayByAnotherArrayKeys;
