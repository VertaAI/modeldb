import React from 'react';
import { connect } from 'react-redux';

import { isHttpNotFoundError } from 'shared/models/Error';
import { Datasets } from 'features/datasets';
import { selectCommunications } from 'features/datasets/store';
import { IApplicationState } from 'store/store';

import NotFoundPage from '../../NotFoundPage/NotFoundPage';
import DatasetsPagesLayout from '../shared/DatasetsPagesLayout/DatasetsPagesLayout';

const mapStateToProps = (state: IApplicationState) => {
  return {
    loadingDatasets: selectCommunications(state).loadingDatasets,
  };
};

type AllProps = ReturnType<typeof mapStateToProps>;

class DatasetsPage extends React.PureComponent<AllProps> {
  public render() {
    const { loadingDatasets } = this.props;

    if (isHttpNotFoundError(loadingDatasets.error)) {
      return <NotFoundPage />;
    }

    return (
      <DatasetsPagesLayout>
        <Datasets />
      </DatasetsPagesLayout>
    );
  }
}

export default connect(mapStateToProps)(DatasetsPage);
