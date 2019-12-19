package sonia.scm.repository.spi;

import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;

import java.util.Set;

public interface MergeCommand {
  /**
   * Executes the merge.
   * @param request The parameters specifying the merge.
   * @return Result holding either the new revision or a list of conflicting files.
   * @throws sonia.scm.NoChangesMadeException If the merge neither had a conflict nor made any change.
   */
  MergeCommandResult merge(MergeCommandRequest request);

  MergeDryRunCommandResult dryRun(MergeCommandRequest request);

  boolean isSupported(MergeStrategy strategy);

  Set<MergeStrategy> getSupportedMergeStrategies();
}
