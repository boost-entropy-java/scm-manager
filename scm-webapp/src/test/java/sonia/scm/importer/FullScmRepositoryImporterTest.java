package sonia.scm.importer;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.ImportFailedException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.UnbundleCommandBuilder;
import sonia.scm.store.RepositoryStoreImporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FullScmRepositoryImporterTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService service;
  @Mock
  private UnbundleCommandBuilder unbundleCommandBuilder;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private ScmEnvironmentCompatibilityChecker compatibilityChecker;
  @Mock
  private RepositoryStoreImporter storeImporterFactory;

  @InjectMocks
  private FullScmRepositoryImporter fullImporter;

  @BeforeEach
  void initRepositoryService() {
    lenient().when(serviceFactory.create(REPOSITORY)).thenReturn(service);
    lenient().when(service.getUnbundleCommand()).thenReturn(unbundleCommandBuilder);
  }

  @Test
  void shouldNotImportRepositoryIfFileNotExists(@TempDir Path temp) throws IOException {
    File emptyFile = new File(temp.resolve("empty").toString());
    Files.touch(emptyFile);
    assertThrows(ImportFailedException.class, () -> fullImporter.importFromFile(REPOSITORY, new FileInputStream(emptyFile)));
  }

  @Test
  void shouldFailIfScmEnvironmentIsIncompatible() {
    when(compatibilityChecker.check(any())).thenReturn(false);

    assertThrows(
      ImportFailedException.class,
      () -> fullImporter.importFromFile(REPOSITORY, Resources.getResource("sonia/scm/repository/import/scm-import.tar.gz").openStream())
    );
  }

  @Test
  void shouldImportScmRepositoryArchive() throws IOException {
    when(compatibilityChecker.check(any())).thenReturn(true);
    when(repositoryManager.create(eq(REPOSITORY), any())).thenReturn(REPOSITORY);

    fullImporter.importFromFile(REPOSITORY, Resources.getResource("sonia/scm/repository/import/scm-import.tar.gz").openStream());

    verify(storeImporterFactory).doImport(eq(REPOSITORY));
  }
}
