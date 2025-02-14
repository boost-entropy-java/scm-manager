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

import { NamespaceCollection, Repository } from "@scm-manager/ui-types";

import groupByNamespace from "./groupByNamespace";
import RepositoryGroupEntry from "./RepositoryGroupEntry";
import { ExtensionPoint, extensionPoints } from "@scm-manager/ui-extensions";
import { KeyboardIterator, KeyboardSubIterator } from "@scm-manager/ui-shortcuts";

type Props = {
  repositories: Repository[];
  namespaces: NamespaceCollection;
  page: number;
  search: string;
  namespace?: string;
};

class RepositoryList extends React.Component<Props> {
  render() {
    const { repositories, namespaces, namespace, page, search } = this.props;

    const groups = groupByNamespace(repositories, namespaces);
    return (
      <div className="content">
        <KeyboardIterator>
          <KeyboardSubIterator>
            <ExtensionPoint<extensionPoints.RepositoryOverviewTop>
              name="repository.overview.top"
              renderAll={true}
              props={{
                page,
                search,
                namespace,
              }}
            />
          </KeyboardSubIterator>
          {groups.map((group) => {
            return <RepositoryGroupEntry group={group} key={group.name} />;
          })}
        </KeyboardIterator>
      </div>
    );
  }
}

export default RepositoryList;
