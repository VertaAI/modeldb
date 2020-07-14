import withProps from 'shared/utils/react/withProps';

import SimpleKeyValuesColumn from '../shared/SimpleKeyValuesColumn/SimpleKeyValuesColumn';

const HyperparametersColumn = withProps(SimpleKeyValuesColumn)({
  type: 'hyperparameters',
});

export default HyperparametersColumn;
