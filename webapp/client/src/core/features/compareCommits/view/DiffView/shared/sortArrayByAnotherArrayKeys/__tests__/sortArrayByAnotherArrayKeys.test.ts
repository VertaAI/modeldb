import sortArrayByAnotherArrayKeys from '../sortArrayByAnotherArrayKeys';

describe('sortArrayByAnotherArrayKeys', () => {
  it('should work correct', () => {
    const array1 = [
      { key: 'zcvzcvzcv', value: 4 },
      { key: 'adfadf', value: 5 },
      { key: 'string', value: 1 },
    ];
    const array2 = [
      { key: 'string', value: 1 },
      { key: 'zcvzcvzcv', value: 7 },
    ];

    expect(
      sortArrayByAnotherArrayKeys(({ key }) => key, array1, array2)
    ).toEqual([array1[2], array1[0], array1[1]]);
    expect(
      sortArrayByAnotherArrayKeys(({ key }) => key, array2, array1)
    ).toEqual([array2[0], array2[1]]);
  });
});
