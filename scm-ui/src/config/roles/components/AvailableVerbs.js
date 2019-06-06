//@flow
import React from "react";
import { compose } from "redux";
import injectSheet from "react-jss";
import { translate } from "react-i18next";
import type { RepositoryRole } from "@scm-manager/ui-types";

type Props = {
  role: RepositoryRole,

  // context props
  classes: any,
  t: string => string
};

const styles = {
  spacing: {
    padding: "0 !important"
  }
};

class AvailableVerbs extends React.Component<Props> {
  render() {
    const { role, t, classes } = this.props;

    let verbs = null;
    if (role.verbs.length > 0) {
      verbs = (
        <tr>
          <td className={classes.spacing}>
            <ul>
              {role.verbs.map(verb => {
                return (
                  <li>{t("verbs.repository." + verb + ".displayName")}</li>
                );
              })}
            </ul>
          </td>
        </tr>
      );
    }
    return verbs;
  }
}

export default compose(
  injectSheet(styles),
  translate("plugins")
)(AvailableVerbs);