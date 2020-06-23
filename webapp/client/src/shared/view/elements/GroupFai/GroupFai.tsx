import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import styles from './GroupFai.module.css';

interface ILocalProps {
  groupFai: Array<
    (requiredProps: {
      onHover: () => void;
      onUnhover: () => void;
    }) => React.ReactNode
  >;
}

interface ILocalState {
  hoveredFaiIndex: number | null;
}

class GroupFai extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    hoveredFaiIndex: null,
  };

  public render() {
    const { groupFai } = this.props;
    const { hoveredFaiIndex } = this.state;
    return (
      <div className={styles.root}>
        {groupFai.map((renderFai, i) => (
          <div
            className={cn(styles.item, {
              [styles.fade]: hoveredFaiIndex !== null && hoveredFaiIndex !== i,
            })}
            key={i}
            onClick={this.makeOnFaiUnhover()}
          >
            {renderFai({
              onHover: this.makeOnFaiHover(i),
              onUnhover: this.makeOnFaiUnhover(),
            })}
          </div>
        ))}
      </div>
    );
  }

  @bind
  private makeOnFaiHover(index: number) {
    return () => this.setState(prev => ({ hoveredFaiIndex: index }));
  }

  @bind
  private makeOnFaiUnhover() {
    return () => this.setState(prev => ({ hoveredFaiIndex: null }));
  }
}

export default GroupFai;
