import * as React from 'react';
import { connect } from 'react-redux';

import { isHttpNotFoundError } from 'shared/models/Error';
import { selectCommunications } from 'features/projects/store';
import Projects from 'features/projects/view/Projects/Projects';
import NotFoundPage from 'pages/authorized/NotFoundPage/NotFoundPage';
import { IApplicationState } from 'store/store';

import ProjectsPagesLayout from '../shared/ProjectsPagesLayout/ProjectsPagesLayout';

const mapStateToProps = (state: IApplicationState) => ({
  loadingProjects: selectCommunications(state).loadingProjects,
});

type AllProps = ReturnType<typeof mapStateToProps>;

interface ILocalState {
  isNeedResetPagination: boolean;
}

class ProjectsPage extends React.PureComponent<AllProps, ILocalState> {
  public render() {
    const { loadingProjects } = this.props;

    if (isHttpNotFoundError(loadingProjects.error)) {
      return <NotFoundPage />;
    }

    return (
      <ProjectsPagesLayout>
        <Projects />
      </ProjectsPagesLayout>
    );
  }
}

export default connect(mapStateToProps)(ProjectsPage);
