import { InputAdornment } from '@material-ui/core';
import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';

import { Icon } from 'shared/view/elements/Icon/Icon';
import MuiTextInput from 'shared/view/elements/MuiTextInput/MuiTextInput';
import routes from 'shared/routes';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import styles from './HeaderSearch.module.css';
import { defaultFilter } from '../../constants';

const mapStateToProps = (state: IApplicationState) => ({
  currentWorkspaceName: selectCurrentWorkspaceName(state),
});

type AllProps = ReturnType<typeof mapStateToProps>;

const HeaderSearch = ({ currentWorkspaceName }: AllProps) => {
  const [value, changeValue] = React.useState<string>('');
  const history = useHistory();

  const currentQueryParams =
    routes.highLevelSearch.parseQueryParams(history.location.search) || {};
  const queryParams = {
    ...currentQueryParams,
    q: value,
    type: currentQueryParams.type || defaultFilter,
  };

  const redirectToHighLevelSearchWithValue = () => {
    history.push(
      routes.highLevelSearch.getRedirectPathWithQueryParams({
        params: { workspaceName: currentWorkspaceName },
        queryParams,
      })
    );
  };

  return (
    <div className={styles.root}>
      <MuiTextInput
        value={value}
        placeholder="Search"
        classes={{
          root: styles.root,
        }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <Icon
                type="search"
                className={styles.icon}
                onClick={redirectToHighLevelSearchWithValue}
              />
            </InputAdornment>
          ),
        }}
        onKeyUp={(e) => {
          // when press enter
          if (e.keyCode === 13) {
            redirectToHighLevelSearchWithValue();
          }
        }}
        onChange={(e) => changeValue(e.currentTarget.value)}
      />
    </div>
  );
};

export default connect(mapStateToProps)(HeaderSearch);
