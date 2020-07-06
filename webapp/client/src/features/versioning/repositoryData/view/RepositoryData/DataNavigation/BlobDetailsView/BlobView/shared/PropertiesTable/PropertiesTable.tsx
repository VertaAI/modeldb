import { Paper } from '@material-ui/core';
import React from 'react';

import VerticalTable, {
  IPropDefinition,
} from 'shared/view/elements/VerticalTable/VerticalTable';

interface ILocalProps<T> {
  data: T;
  propDefinitions: Array<IPropDefinition<T>>;
}

function PropertiesTable<T>({ propDefinitions, data }: ILocalProps<T>) {
  return (
    <Paper>
      <VerticalTable
        columnsData={[{ data }]}
        propDefinitions={propDefinitions}
      />
    </Paper>
  );
}

export default PropertiesTable;
