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
  // @ts-ignore
  props: Omit<UseAutocompleteProps<T>, 'groupBy'>,
  groupKey: GroupKey
): Result<T, GroupKey> => {
  return materialUseAutocomplete({
    ...props,
    multiple: false,
    // Typescript gave up here before I found it
    groupBy: (option: any) => option[groupKey] as any,
  } as any) as any;
};

export default useAutocomplete;
