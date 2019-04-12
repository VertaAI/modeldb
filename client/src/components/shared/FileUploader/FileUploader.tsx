import * as React from 'react';

import { bind } from 'decko';

interface ILocalProps {
  acceptFileTypes?: AcceptFileTypes[];
  children(onSelectFile: () => void, isUploading: boolean): React.ReactNode;
  onUpload(file: any): void;
}

interface ILocalState {
  isUploading: boolean;
}

type AcceptFileTypes = 'csv';

class FileUploader extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = { isUploading: false };

  private fileInput = React.createRef<HTMLInputElement>();

  public render() {
    const { children } = this.props;
    return (
      <>
        {children(this.onSelectFile, false)}
        <input
          style={{ display: 'none' }}
          type="file"
          accept={this.getAcceptFiles()}
          ref={this.fileInput}
          onChange={this.onChange}
        />
      </>
    );
  }

  @bind
  private onSelectFile() {
    this.fileInput.current!.click();
  }

  @bind
  private onChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files![0];
    this.props.onUpload(file);
  }

  @bind
  private getAcceptFiles() {
    const { acceptFileTypes = [] } = this.props;
    return acceptFileTypes
      .map(fileType => {
        if (fileType === 'csv') {
          return '.csv, text/csv';
        }
        return '';
      })
      .join(',');
  }
}

export default FileUploader;
