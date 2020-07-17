import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';
import cn from 'classnames';

import Button from 'shared/view/elements/Button/Button';
import MuiTextInput from 'shared/view/elements/MuiTextInput/MuiTextInput';
import findHighlight from 'shared/utils/findHighlight';
import { Icon } from 'shared/view/elements/Icon/Icon';
import * as Suggestions from 'shared/models/HighLevelSearch/Suggestions';
import matchType from 'shared/utils/matchType';
import useSuggestionsFromLocalStorage from 'features/highLevelSearch/store/useSuggestionsFromLocalStorage';

import { actions } from '../../../store';
import styles from './SearchForm.module.css';
import useAutocomplete from './useAutocomplete';

interface ILocalProps {
  initialValue: string;
}

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      setSearchValue: actions.setSearchValue,
    },
    dispatch
  );
};

type AllProps = ReturnType<typeof mapDispatchToProps> & ILocalProps;

const SearchForm = ({ initialValue, setSearchValue }: AllProps) => {
  const [value, changeValue] = React.useState(initialValue);
  React.useEffect(() => {
    if (value !== initialValue) {
      changeValue(initialValue);
    }
  }, [initialValue]);

  const onSetSearchValue = React.useCallback(
    (value: string) => {
      if (value) {
        changeValue(value);
        setSearchValue(value);
      }
    },
    [value]
  );

  const {
    suggestions,
    addFavoriteToSuggestions,
    removeFavoriteFromSuggestions,
  } = useSuggestionsFromLocalStorage(initialValue);

  return (
    <div className={styles.root}>
      <Search
        inputValue={value}
        suggestions={suggestions}
        onInputChange={changeValue}
        onEnter={() => setSearchValue(value)}
        onChange={option => setSearchValue(option.value)}
        onAddFavoriteToSuggestions={addFavoriteToSuggestions}
        onRemoveFavoriteFromSuggestions={removeFavoriteFromSuggestions}
      />
      <div className={styles.button}>
        <Button
          disabled={!value}
          theme="primary"
          fullHeight={true}
          onClick={() => onSetSearchValue(value)}
        >
          Search
        </Button>
      </div>
    </div>
  );
};

type ISearchOption = {
  group: keyof Suggestions.ISuggestions;
  value: string;
};
interface ISearchLocalProps {
  suggestions: Suggestions.ISuggestions;
  inputValue: string;
  onEnter: () => void;
  onChange: (option: ISearchOption) => void;
  onInputChange: (inputValue: string) => void;
  onRemoveFavoriteFromSuggestions: (recentSearch: string) => void;
  onAddFavoriteToSuggestions: (favorite: string) => void;
}
const Search = ({
  suggestions,
  inputValue,
  onEnter,
  onAddFavoriteToSuggestions,
  onChange,
  onInputChange,
  onRemoveFavoriteFromSuggestions,
}: ISearchLocalProps) => {
  const suggestionOptions = getSuggestionOptions(suggestions);

  const autocompleteProps = useAutocomplete(
    {
      freeSolo: true,
      openOnFocus: true,
      options: suggestionOptions,
      inputValue: inputValue,
      defaultValue: suggestionOptions.find(({ value }) => value === inputValue),
      getOptionLabel: (option: { value: any; }) => option.value,
      getOptionSelected: (option: { value: any; }, selectedOption: { value: any; }) =>
        option.value === selectedOption.value,
      onChange: (e: any, option: ISearchOption | null, reason: string) => {
        if (
          typeof option !== 'string' &&
          option &&
          reason === 'select-option'
        ) {
          onChange(option);
        }
      },
      onInputChange: (e: React.ChangeEvent<{}>) => {
        if (!e) {
          return;
        }
        const value = (e as React.ChangeEvent<HTMLInputElement>).currentTarget
          .value;
        if (typeof value === 'string') {
          onInputChange(value);
        }
      },
    },
    // @ts-ignore
    'group'
  );

  return (
    <div className={styles.field}>
      <div
        className={styles.autocomplete}
        {...autocompleteProps.getRootProps()}
      >
        <MuiTextInput
          {...autocompleteProps.getInputProps()}
          placeholder="Search via names and tags here."
          resetValueControl={
            inputValue && autocompleteProps.focused
              ? {
                onReset: () => onInputChange(''),
              }
              : undefined
          }
          onKeyUp={e => {
            if (e.key === 'Enter') {
              onEnter();
            }
          }}
        />
      </div>
      {autocompleteProps.groupedOptions.length > 0 ? (
        <div
          {...autocompleteProps.getListboxProps()}
          className={styles.autocomplete__optionsList}
        >
          {autocompleteProps.groupedOptions.map(
            // @ts-ignore
            ({ options, group, index: groupIndex }) => (
              <div className={styles.autocomplete__optionsGroup} key={group}>
                <div className={styles.autocomplete__optionsGroupTitle}>
                  {matchType(
                    {
                      favorites: () => 'Favorites',
                      recentSearches: () => 'Recent Searches',
                    },
                    group
                  )}
                </div>
                // @ts-ignore
                {options.map((option, index) => {
                  const resIndex = groupIndex + index;
                  const additionalProps = autocompleteProps.getOptionProps({
                    option,
                    index: resIndex,
                  });
                  return (
                    <Option
                      inputValue={inputValue}
                      option={option}
                      onAddFavoriteToSuggestions={onAddFavoriteToSuggestions}
                      onRemoveFavoriteFromSuggestions={
                        onRemoveFavoriteFromSuggestions
                      }
                      additionalProps={additionalProps}
                      key={resIndex}
                    />
                  );
                })}
              </div>
            )
          )}
        </div>
      ) : null}
    </div>
  );
};

const Option = ({
  option,
  inputValue,
  additionalProps,
  onAddFavoriteToSuggestions,
  onRemoveFavoriteFromSuggestions,
}: {
  option: ISearchOption;
  inputValue: string;
  additionalProps: { 'aria-selected': boolean };
} & Pick<
  ISearchLocalProps,
  'onAddFavoriteToSuggestions' | 'onRemoveFavoriteFromSuggestions'
>) => {
  return (
    <div
      className={cn(styles.autocomplete__option, {
        [styles.selected]: additionalProps['aria-selected'],
        [styles[option.group]]: option.group,
      })}
    >
      <OptionTitle
        inputValue={inputValue}
        title={option.value}
        additionalProps={additionalProps}
      />
      {option.group === 'recentSearches' ? (
        <OptionAction onClick={() => onAddFavoriteToSuggestions(option.value)}>
          Add to favorites
        </OptionAction>
      ) : (
          <OptionAction
            onClick={() => onRemoveFavoriteFromSuggestions(option.value)}
          >
            Remove from favorites
          </OptionAction>
        )}
    </div>
  );
};

const OptionTitle = ({
  inputValue,
  title,
  additionalProps,
}: {
  inputValue: string;
  title: string;
  additionalProps: {};
}) => {
  const parts = findHighlight({
    searchWord: inputValue,
    textToHighlight: title,
    settings: { allMatches: true, caseIntensive: true },
  });

  return (
    <div className={styles.autocomplete__optionTitle} {...additionalProps}>
      {parts.map((part, i) => (
        <span style={{ fontWeight: part.isMatch ? 700 : 400 }} key={i}>
          {part.text}
        </span>
      ))}
    </div>
  );
};

const getSuggestionOptions = (
  suggestions: Suggestions.ISuggestions
): ISearchOption[] => {
  return [
    ...Object.values(suggestions.recentSearches).map(value => ({
      value,
      group: 'recentSearches' as const,
    })),
    ...Object.values(suggestions.favorites).map(value => ({
      value,
      group: 'favorites' as const,
    })),
  ];
};

const OptionAction = ({
  onClick,
  children,
}: {
  children: string;
  onClick: () => void;
}) => {
  return (
    <div
      className={styles.autocomplete__optionAction}
      onClick={() => {
        onClick();
      }}
    >
      <span className={styles.autocomplete__optionActionLabel}>{children}</span>
      <Icon type="favorite" className={styles.autocomplete__optionActionIcon} />
    </div>
  );
};

export default connect(
  undefined,
  mapDispatchToProps
)(SearchForm);
