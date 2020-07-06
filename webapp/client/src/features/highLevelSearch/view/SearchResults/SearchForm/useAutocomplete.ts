import materialUseAutocomplete, { UseAutocompleteProps } from '@material-ui/lab/useAutocomplete';

type Result<T, GroupKey extends keyof T> = {
  groupedOptions: Array<
    Record<GroupKey, T[GroupKey]> & {
      index: number;
      options: Array<T>;
    }
  >;
  getRootProps: () => {};
  getClearProps: () => { onClick: () => void };
  getListboxProps: () => {};
  getOptionProps: (props: {
    option: T;
    index: number;
  }) => { 'aria-selected': boolean };
  getInputProps: () => {};
  focused: boolean;
  value: T;
  inputValue: string;
};

const useAutocomplete = <T, GroupKey extends keyof T>(
  props: Omit<UseAutocompleteProps<T, false, false, true>, 'groupBy'>,
  groupKey: GroupKey
): Result<T, GroupKey> => {
  return materialUseAutocomplete({
    ...props,
    multiple: false,
    groupBy: option => option[groupKey] as any,
  }) as any;
};

export default useAutocomplete;
