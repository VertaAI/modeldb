import {
  IObjectToObjectWithDiffColor,
  getCssDiffColor,
} from '../../../../model';

import { isKeyOf } from 'core/shared/utils/isKeyOf';

export const getDatasetDiffCellStyle = (columnType: string) => <T>(
  row: IObjectToObjectWithDiffColor<T>
) => {
  if (isKeyOf(row, columnType)) {
    return {
      backgroundColor:
        row[columnType].diffColor && getCssDiffColor(row[columnType].diffColor),
    };
  }

  throw Error(`Couldn\`t get data by columnType ${columnType}`);
};
