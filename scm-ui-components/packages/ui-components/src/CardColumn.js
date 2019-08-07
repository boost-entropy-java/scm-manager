//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";

import { Link } from "react-router-dom";

const styles = {
  inner: {
    position: "relative",
    pointerEvents: "none",
    zIndex: 1
  },
  innerLink: {
    pointerEvents: "all"
  },
  centerImage: {
    marginTop: "0.8em",
    marginLeft: "1em !important"
  },
  flexFullHeight: {
    display: "flex",
    flexDirection: "column",
    alignSelf: "stretch"
  },
  content: {
    display: "flex",
    flexGrow: 1
  },
  footer: {
    display: "flex",
    marginTop: "auto",
    paddingBottom: "1.5rem"
  }
};

type Props = {
  title: string,
  description: string,
  avatar: React.Node,
  footerLeft: React.Node,
  footerRight: React.Node,
  link: string,
  // context props
  classes: any
};

class CardColumn extends React.Component<Props> {
  createLink = () => {
    const { link } = this.props;
    if (link) {
      return <Link className="overlay-column" to={link} />;
    }
    return null;
  };

  render() {
    const { avatar, title, description, footerLeft, footerRight, classes } = this.props;
    const link = this.createLink();
    return (
      <>
        {link}
        <article className={classNames("media", classes.inner)}>
          <figure className={classNames(classes.centerImage, "media-left")}>
            {avatar}
          </figure>
          <div className={classNames("media-content", "text-box", classes.flexFullHeight)}>
            <div className={classes.content}>
              <div className="content shorten-text">
                <p className="is-marginless">
                  <strong>{title}</strong>
                </p>
                <p className="shorten-text">{description}</p>
              </div>
            </div>
            <div className={classNames(classes.footer, "level")}>
              <div className="level-left is-hidden-mobile">{footerLeft}</div>
              <div className="level-right is-mobile">{footerRight}</div>
            </div>
          </div>
        </article>
      </>
    );
  }
}

export default injectSheet(styles)(CardColumn);