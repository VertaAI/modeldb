import cn from 'classnames';
import React from 'react';
import {
  Manager as PopperManager,
  Popper,
  Reference as ReferenceForPopper,
} from 'react-popper';

import {
  CommitPointerHelpers,
  CommitPointerValue,
  CommitTag,
  RepositoryBranches,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import matchType from 'core/shared/utils/matchType';
import ClickOutsideListener from 'core/shared/view/elements/ClickOutsideListener/ClickOutsideListener';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import MuiTextInput from 'core/shared/view/elements/MuiTextInput/MuiTextInput';

import styles from './BranchesAndTagsList.module.css';

interface ILocalProps {
  tags: CommitTag[];
  branches: RepositoryBranches;
  commitPointer: CommitPointer;
  onChangeCommitPointer(commitPointer: CommitPointer): void;
}

type AllProps = ILocalProps;

const BranchesAndTagsList: React.FC<AllProps> = props => {
  const { tags, onChangeCommitPointer, commitPointer, branches } = props;

  const [isOpened, changeVisibility] = React.useState(false);
  const [activeTab, changeTab] = React.useState<'branches' | 'tags'>(
    'branches'
  );
  const [searchValue, changeSearchValue] = React.useState('');

  const onChange = (value: CommitPointerValue) => {
    changeVisibility(false);

    const newCommitPointer =
      activeTab === 'branches'
        ? CommitPointerHelpers.makeFromBranch(value)
        : CommitPointerHelpers.makeFromTag(value);
    onChangeCommitPointer(newCommitPointer);
  };

  const displayedItems = (activeTab === 'branches' ? branches : tags).filter(
    item => item.toLowerCase().includes(searchValue.toLowerCase())
  );

  return (
    <ClickOutsideListener
      onClickOutside={() => {
        if (isOpened) {
          changeVisibility(false);
        }
      }}
    >
      <div className={cn(styles.root, { [styles.opened]: isOpened })}>
        <PopperManager>
          <ReferenceForPopper>
            {({ ref }) => (
              <button
                className={styles.summary}
                ref={ref}
                title={commitPointer.value}
                onClick={() => changeVisibility(true)}
              >
                {matchType(
                  {
                    branch: () => 'Branch:',
                    tag: () => 'Tag:',
                    commitSha: () => 'Tree:',
                  },
                  commitPointer.type
                )}{' '}
                <div
                  className={styles.summary__value}
                  data-test="branches-and-tags-list-selected-value"
                >
                  {commitPointer.value}
                </div>
              </button>
            )}
          </ReferenceForPopper>

          <Popper placement="bottom-start">
            {({ ref, style }) => (
              <div
                className={cn(styles.modal, { [styles.opened]: isOpened })}
                ref={ref}
                style={style}
              >
                <div className={styles.header}>Switch branches/tags</div>
                <div className={styles.filter}>
                  <MuiTextInput
                    placeholder="Find..."
                    value={searchValue}
                    onChange={({ currentTarget: { value } }) =>
                      changeSearchValue(value)
                    }
                  />
                </div>
                <div className={styles.tabs}>
                  <div
                    className={cn(styles.tab, {
                      [styles.active]: activeTab === 'branches',
                    })}
                    onClick={() => changeTab('branches')}
                  >
                    Branches
                  </div>
                  <div
                    className={cn(styles.tab, {
                      [styles.active]: activeTab === 'tags',
                    })}
                    onClick={() => changeTab('tags')}
                  >
                    Tags
                  </div>
                </div>
                <div className={styles.list}>
                  {displayedItems.length > 0 ? (
                    displayedItems.map(item => (
                      <div
                        className={cn(styles.item, {
                          [styles.active]: item === commitPointer.value,
                        })}
                        key={item}
                        onClick={() => {
                          onChange(item);
                        }}
                      >
                        <Icon
                          type="check-solid"
                          className={styles.item__selectedMark}
                        />
                        <span className={styles.name} title={item}>
                          {item}
                        </span>
                      </div>
                    ))
                  ) : (
                    <div className={styles.list__placeholder}>
                      Nothing to show
                    </div>
                  )}
                </div>
              </div>
            )}
          </Popper>
        </PopperManager>
      </div>
    </ClickOutsideListener>
  );
};

export default React.memo(BranchesAndTagsList);
