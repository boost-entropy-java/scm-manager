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

package sonia.scm.group.cli;

import com.google.common.annotations.VisibleForTesting;
import picocli.CommandLine;
import sonia.scm.cli.ParentCommand;
import sonia.scm.group.Group;
import sonia.scm.group.GroupManager;
import sonia.scm.security.PermissionAssigner;
import sonia.scm.security.PermissionDescriptor;

import javax.inject.Inject;
import java.util.Collection;

@ParentCommand(value = GroupCommand.class)
@CommandLine.Command(name = "add-permissions")
class GroupPermissionAddCommand implements Runnable {

  @CommandLine.Parameters(index = "0", paramLabel = "<name>", descriptionKey = "scm.group.name")
  private String name;

  @CommandLine.Parameters(index = "1..", arity = "1..", paramLabel = "<permission>", descriptionKey = "scm.group.permissions")
  private String[] addedPermissions;

  @CommandLine.Mixin
  private final GroupTemplateRenderer templateRenderer;
  private final PermissionAssigner permissionAssigner;
  private final GroupManager groupManager;
  @CommandLine.Spec
  private CommandLine.Model.CommandSpec spec;

  @Inject
  GroupPermissionAddCommand(GroupTemplateRenderer templateRenderer, PermissionAssigner permissionAssigner, GroupManager groupManager) {
    this.templateRenderer = templateRenderer;
    this.permissionAssigner = permissionAssigner;
    this.groupManager = groupManager;
  }

  @Override
  public void run() {
    Group group = groupManager.get(name);
    if (group == null) {
      templateRenderer.renderNotFoundError();
      return;
    }
    Collection<PermissionDescriptor> permissions = permissionAssigner.readPermissionsForGroup(name);
    for (String addedPermission : addedPermissions) {
      if (isPermissionInvalid(addedPermission)) {
        templateRenderer.renderUnknownPermissionError(addedPermission);
        return;
      }
      permissions.add(new PermissionDescriptor(addedPermission));
    }
    permissionAssigner.setPermissionsForGroup(name, permissions);
  }

  private boolean isPermissionInvalid(String permission) {
    return permissionAssigner.getAvailablePermissions()
      .stream()
      .noneMatch(p -> p.getValue().equals(permission));
  }

  @VisibleForTesting
  void setName(String name) {
    this.name = name;
  }
  @VisibleForTesting
  void setAddedPermissions(String[] addedPermissions) {
    this.addedPermissions = addedPermissions;
  }
}
