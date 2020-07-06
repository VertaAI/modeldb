import { IObjectToObjectWithDiffColor } from '../../../../model';

export const getDatasetDiffGroupStyle = <T>(
  group: Array<IObjectToObjectWithDiffColor<T>>
) => {
  if (group.length > 1) {
    return {
      margin: '10px 0',
    };
  }

  return {};
};
