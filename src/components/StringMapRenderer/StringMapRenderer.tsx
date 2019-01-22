import * as React from 'react';

interface IOwnProps {
  data: Map<string, string>;
  containerClassName?: string;
  itemClassName?: string;
}

export default class StringMapRenderer extends React.Component<IOwnProps> {
  public render() {
    const stringArray = Array<string>();
    this.props.data.forEach((value: string, key: string) => {
      stringArray.push(`${key}: ${value}`);
    });

    return (
      <div className={this.props.containerClassName}>
        {stringArray.map((i, key) => {
          return (
            <div className={this.props.itemClassName} key={key}>
              {i}
            </div>
          );
        })}
      </div>
    );
  }
}
