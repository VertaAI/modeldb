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
  defaultBranch,
} from 'shared/models/Versioning/RepositoryData';
import matchType from 'shared/utils/matchType';
import ClickOutsideListener from 'shared/view/elements/ClickOutsideListener/ClickOutsideListener';
import { Icon } from 'shared/view/elements/Icon/Icon';
import MuiTextInput from 'shared/view/elements/MuiTextInput/MuiTextInput';
import Button from 'shared/view/elements/Button/Button';

import styles from './BranchesAndTagsList.module.css';

interface ILocalProps {
  tags: CommitTag[];
  branches: RepositoryBranches;
  commitPointer: CommitPointer;
  valueLabel?: string;
  dataTest?: string;
  onChangeCommitPointer(commitPointer: CommitPointer): void;
}

type AllProps = ILocalProps;

const BranchesAndTagsList: React.FC<AllProps> = props => {
  const {
    tags,
    onChangeCommitPointer,
    commitPointer,
    branches,
    valueLabel,
    dataTest,
  } = props;

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
      <div
        className={cn(styles.root, { [styles.opened]: isOpened })}
        data-test={dataTest}
      >
        <PopperManager>
          <ReferenceForPopper>
            {({ ref }) => (
              <div ref={ref}>
                <Button
                  size="small"
                  theme="tertiary"
                  dataTest="branches-and-tags-summary-button"
                  onClick={() => changeVisibility(true)}
                >
                  <div className={styles.button__content}>
                    <span className={styles.button__label}>
                      {valueLabel ||
                        matchType(
                          {
                            branch: () => 'Branch',
                            tag: () => 'Tag',
                            commitSha: () => 'Tree',
                          },
                          commitPointer.type
                        )}
                    </span>
                    :&nbsp;
                    <div
                      className={styles.selectedValue}
                      data-test="branches-and-tags-list-selected-value"
                    >
                      {commitPointer.value}
                    </div>
                    &nbsp;
                    <Icon type="caret-down" className={styles.button__icon} />
                  </div>
                </Button>
              </div>
            )}
          </ReferenceForPopper>

          <Popper placement="bottom-start">
            {({ ref, style }) => (
              <div
                className={cn(styles.modal, { [styles.opened]: isOpened })}
                ref={ref}
                style={style}
                data-test="branches-and-tags-menu"
              >
                <div className={styles.title}>Switch branches/tags</div>
                <div className={styles.filter}>
                  <MuiTextInput
                    placeholder="Find..."
                    value={searchValue}
                    name="branche-and-tags-list-filter"
                    size="extraSmall"
                    onChange={({ target: { value } }) =>
                      changeSearchValue(value)
                    }
                  />
                </div>
                <div className={styles.tabs}>
                  <div
                    className={cn(styles.tab, {
                      [styles.active]: activeTab === 'branches',
                    })}
                    data-test="branche-and-tags-list-tab"
                    onClick={() => changeTab('branches')}
                  >
                    Branches
                  </div>
                  <div
                    className={cn(styles.tab, {
                      [styles.active]: activeTab === 'tags',
                    })}
                    data-test="branche-and-tags-list-tab"
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
                        <span
                          className={styles.name}
                          title={item}
                          data-test="branches-and-tags-list-item"
                        >
                          {item}
                        </span>
                        {activeTab === 'branches' && item === defaultBranch && (
                          <span className={styles.defaultBranch}>
                            &nbsp;(default)
                          </span>
                        )}
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
