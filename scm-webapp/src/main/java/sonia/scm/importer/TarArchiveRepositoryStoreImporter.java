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

package sonia.scm.importer;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import sonia.scm.ContextEntry;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.ImportFailedException;
import sonia.scm.store.RepositoryStoreImporter;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TarArchiveRepositoryStoreImporter {

  private final RepositoryStoreImporter repositoryStoreImporter;

  @Inject
  public TarArchiveRepositoryStoreImporter(RepositoryStoreImporter repositoryStoreImporter) {
    this.repositoryStoreImporter = repositoryStoreImporter;
  }

  public void importFromTarArchive(Repository repository, InputStream inputStream) {
    try (TarArchiveInputStream tais = new TarArchiveInputStream(inputStream)) {
      ArchiveEntry entry = tais.getNextEntry();
      while (entry != null) {
        String[] entryPathParts = entry.getName().split(File.separator);
        validateStorePath(repository, entryPathParts);
        importStoreByType(repository, tais, entryPathParts);
        entry = tais.getNextEntry();
      }
    } catch (IOException e) {
      throw  new ImportFailedException(ContextEntry.ContextBuilder.entity(repository).build(), "Could not import stores from metadata file.", e);
    }
  }

  private void importStoreByType(Repository repository, TarArchiveInputStream tais, String[] entryPathParts) {
    if (entryPathParts[1].equals("data")) {
      repositoryStoreImporter
        .doImport(repository)
        .importStore(entryPathParts[1], entryPathParts[2])
        .importEntry(entryPathParts[3], tais);
    } else if (entryPathParts[1].equals("config")){
      repositoryStoreImporter
        .doImport(repository)
        .importStore(entryPathParts[1], "")
        .importEntry(entryPathParts[2], tais);
    }
  }

  private void validateStorePath(Repository repository, String[] entryPathParts) {
    if (!isValidStorePath(entryPathParts)) {
      throw new ImportFailedException(ContextEntry.ContextBuilder.entity(repository).build(), "Invalid store path in metadata file");
    }
  }

  private boolean isValidStorePath(String[] entryPathParts) {
    if (entryPathParts.length < 3 || entryPathParts.length > 4) {
      return false;
    }
    if (entryPathParts[1].equals("data")) {
      return entryPathParts.length == 4;
    }
    if (entryPathParts[1].equals("config")) {
      return entryPathParts.length == 3;
    }
    // We only support config and data stores yet
    return false;
  }
}