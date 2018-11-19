/**
 * Copyright (c) 2010, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.repository.xml;

//~--- non-JDK imports --------------------------------------------------------

import sonia.scm.repository.InitialRepositoryLocationResolver;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.xml.XmlDatabase;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

//~--- JDK imports ------------------------------------------------------------

@XmlRootElement(name = "repository-db")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlRepositoryDatabase implements XmlDatabase<Repository> {

  private final InitialRepositoryLocationResolver initialRepositoryLocationResolver;

  private final XmlRepositoryDatabasePersistence storage;

  public XmlRepositoryDatabase(XmlRepositoryDatabasePersistence storage, InitialRepositoryLocationResolver initialRepositoryLocationResolver) {
    if (storage == null) {
      throw new NullPointerException("storage must not be null");
    }
    if (initialRepositoryLocationResolver == null) {
      throw new NullPointerException("resolver must not be null");
    }
    this.initialRepositoryLocationResolver = initialRepositoryLocationResolver;
    this.storage = storage;

  }

  static String createKey(NamespaceAndName namespaceAndName)
  {
    return namespaceAndName.getNamespace() + ":" + namespaceAndName.getName();
  }

  static String createKey(Repository repository)
  {
    return createKey(repository.getNamespaceAndName());
  }

  @Override
  public void add(Repository repository)
  {
    String path = initialRepositoryLocationResolver.getDirectory(repository).getAbsolutePath();
    RepositoryPath repositoryPath = new RepositoryPath(path, repository.getId(), repository.clone());
    storage.add(repositoryPath);
  }

  public boolean contains(NamespaceAndName namespaceAndName)
  {
    return storage.containsKey(createKey(namespaceAndName));
  }

  @Override
  public boolean contains(String id)
  {
    return storage.get(id) != null;
  }

  public boolean contains(Repository repository)
  {
    return storage.containsKey(createKey(repository));
  }

  public void remove(Repository repository)
  {
    storage.remove(createKey(repository));
  }

  @Override
  public Repository remove(String id)
  {
    return storage.remove(createKey(get(id)));
  }

  public Collection<Repository> getRepositories() {
    return storage.getRepositories();
  }

  @Override
  public Collection<Repository> values()
  {
    return storage.values();
  }

  public Collection<RepositoryPath> getPaths() {
    return storage.getPaths();
  }


  public Repository get(NamespaceAndName namespaceAndName) {
    return storage.get(namespaceAndName);
  }

  @Override
  public Repository get(String id) {
    return values().stream()
      .filter(repo -> repo.getId().equals(id))
      .findFirst()
      .orElse(null);
  }

  /**
   * Method description
   *
   *
   * @return
   */
  @Override
  public long getCreationTime()
  {
    return storage.getCreationTime();
  }

  /**
   * Method description
   *
   *
   * @return
   */
  @Override
  public long getLastModified()
  {
    return storage.getLastModified();
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param creationTime
   */
  @Override
  public void setCreationTime(long creationTime)
  {
    storage.setCreationTime(creationTime);
  }

  /**
   * Method description
   *
   *
   * @param lastModified
   */
  @Override
  public void setLastModified(long lastModified)
  {
    storage.setLastModified(lastModified);
  }
}
