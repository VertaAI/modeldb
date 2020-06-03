import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import Button from 'core/shared/view/elements/Button/Button';
import { resetExperimentRunsSettings } from 'store/experimentRuns';

interface ILocalProps {
  projectId: string;
}

interface IActionProps {
  resetExperimentRunsSettings: typeof resetExperimentRunsSettings;
}

type AllProps = ILocalProps & IActionProps;

class ResetExperimentRunsSettings extends React.PureComponent<AllProps> {
  public render() {
    return (
      <Button onClick={this.resetExperimentRunsSettings}>Reset settings</Button>
    );
  }

  @bind
  private resetExperimentRunsSettings() {
    this.props.resetExperimentRunsSettings(this.props.projectId);
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators({ resetExperimentRunsSettings }, dispatch);
};

export default connect(
  null,
  mapDispatchToProps
)(ResetExperimentRunsSettings);
