/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Repository } from "@scm-manager/ui-types";
import { SubSubtitle } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  repository: Repository;
  target: string;
  source: string;
};

class GitMergeInformation extends React.Component<Props> {
  render() {
    const { source, target, t } = this.props;

    return (
      <div>
        <SubSubtitle>{t("scm-git-plugin.information.merge.heading")}</SubSubtitle>
        {t("scm-git-plugin.information.merge.checkout")}
        <pre>
          <code>git checkout {target}</code>
        </pre>
        {t("scm-git-plugin.information.merge.update")}
        <pre>
          <code>git pull</code>
        </pre>
        {t("scm-git-plugin.information.merge.merge")}
        <pre>
          <code>git merge {source}</code>
        </pre>
        {t("scm-git-plugin.information.merge.resolve")}
        <pre>
          <code>git add &lt;conflict file&gt;</code>
        </pre>
        {t("scm-git-plugin.information.merge.commit")}
        <pre>
          <code>
            git commit -m "Merge {source} into {target}"
          </code>
        </pre>
        {t("scm-git-plugin.information.merge.push")}
        <pre>
          <code>git push</code>
        </pre>
      </div>
    );
  }
}

export default withTranslation("plugins")(GitMergeInformation);
