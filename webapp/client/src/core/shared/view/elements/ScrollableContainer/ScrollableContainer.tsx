import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './ScrollableContainer.module.css';

interface ILocalProps {
  maxHeight?: number;
  listContent?: string[];
  children?: React.ReactChild | React.ReactChildren;
  containerOffsetValue: number;
  minRowCount?: number;
  elementRowCount?: number;
  hiddenChildren?: React.ReactChild | React.ReactChildren;
  getValueAdditionalClassname?(value: string): string | undefined;
}

interface ILocalState {
  containerOffset: number;
  containerOffsetHeight: number;
  containerScrollHeight: number;
}

class ScrollableContainer extends React.Component<ILocalProps, ILocalState> {
  public state = {
    containerOffset: this.props.containerOffsetValue + 1,
    containerHeight: 0,
    containerOffsetHeight: 0,
    containerScrollHeight: 0,
  };
  private scrollableContainer = React.createRef<HTMLDivElement>();

  public componentDidMount() {
    const scrollableDiv = this.scrollableContainer.current;
    if (scrollableDiv) {
      this.setState({
        containerOffsetHeight: scrollableDiv.offsetHeight,
        containerScrollHeight: scrollableDiv.scrollHeight,
      });
    }
  }

  public componentDidUpdate(prevProps: ILocalProps) {
    const scrollableDiv = this.scrollableContainer.current;
    if (scrollableDiv && prevProps.maxHeight !== this.props.maxHeight) {
      this.setState({
        containerOffsetHeight: scrollableDiv.offsetHeight,
        containerScrollHeight: scrollableDiv.scrollHeight,
      });
    }
  }

  public render() {
    const {
      maxHeight,
      listContent,
      children,
      minRowCount,
      elementRowCount,
      hiddenChildren,
      getValueAdditionalClassname = () => undefined,
    } = this.props;
    const isShowScroll =
      this.state.containerScrollHeight > this.state.containerOffsetHeight;
    return (
      <div className={styles.root}>
        <div
          className={styles.list_block}
          style={{
            maxHeight: maxHeight ? maxHeight : '',
          }}
          ref={this.scrollableContainer}
          onScroll={this.onScrollEvent}
        >
          {listContent && listContent.length > 0 && (
            <div className={styles.features}>
              {listContent.map((val, i) => {
                return (
                  <div
                    key={i}
                    className={cn(
                      styles.list_field_value,
                      getValueAdditionalClassname(val)
                    )}
                  >
                    {String(val)}
                  </div>
                );
              })}
            </div>
          )}
          {children && <div>{children}</div>}
          {hiddenChildren && <div>{hiddenChildren}</div>}
        </div>
        {((children && isShowScroll) || (listContent && isShowScroll)) && (
          <this.ScrollDownMarker />
        )}
        {hiddenChildren &&
          elementRowCount &&
          minRowCount &&
          elementRowCount > minRowCount && <this.ScrollDownMarker />}
      </div>
    );
  }

  @bind
  private ScrollDownMarker() {
    const isVisible =
      this.state.containerOffset > this.props.containerOffsetValue;
    return (
      <div
        className={styles.scroll_down_marker}
        style={{
          opacity: isVisible ? 0.85 : 0,
          pointerEvents: !isVisible ? 'none' : undefined,
        }}
        onClick={this.scrollDown}
      >
        <Icon type="double-down-lite" className={styles.scroll_icon} />
      </div>
    );
  }

  @bind
  private scrollDown() {
    const scrollableContainerRef = this.scrollableContainer.current;
    if (scrollableContainerRef) {
      scrollableContainerRef.scrollBy(0, scrollableContainerRef.offsetHeight);
    }
  }

  @bind
  private onScrollEvent() {
    const fieldsContainer = this.scrollableContainer.current;
    if (fieldsContainer) {
      const scrollBottom =
        fieldsContainer.scrollHeight -
        fieldsContainer.scrollTop -
        fieldsContainer.clientHeight;
      this.setState({ containerOffset: scrollBottom });
    }
  }
}

export default ScrollableContainer;
