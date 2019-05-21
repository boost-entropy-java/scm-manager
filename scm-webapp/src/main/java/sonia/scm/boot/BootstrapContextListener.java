/**
 * Copyright (c) 2010, Sebastian Sdorra All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of SCM-Manager;
 * nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * http://bitbucket.org/sdorra/scm-manager
 */


package sonia.scm.boot;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingletonModule;
import sonia.scm.SCMContext;
import sonia.scm.ScmContextListener;
import sonia.scm.ScmEventBusModule;
import sonia.scm.ScmInitializerModule;
import sonia.scm.Stage;
import sonia.scm.event.ScmEventBus;
import sonia.scm.plugin.DefaultPluginLoader;
import sonia.scm.plugin.Plugin;
import sonia.scm.plugin.PluginException;
import sonia.scm.plugin.PluginLoadException;
import sonia.scm.plugin.PluginWrapper;
import sonia.scm.plugin.PluginsInternal;
import sonia.scm.plugin.SmpArchive;
import sonia.scm.update.UpdateEngine;
import sonia.scm.util.ClassLoaders;
import sonia.scm.util.IOUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Sebastian Sdorra
 */
public class BootstrapContextListener implements ServletContextListener {

  /** Field description */
  private static final String DIRECTORY_PLUGINS = "plugins";

  /** Field description */
  private static final String PLUGIN_DIRECTORY = "/WEB-INF/plugins/";

  /**
   * the logger for BootstrapContextListener
   */
  private static final Logger logger =
    LoggerFactory.getLogger(BootstrapContextListener.class);

  /** Field description */
  private static final String PLUGIN_COREINDEX =
    PLUGIN_DIRECTORY.concat("plugin-index.xml");

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param sce
   */
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    contextListener.contextDestroyed(sce);

    for (PluginWrapper plugin : contextListener.getPlugins()) {
      ClassLoader pcl = plugin.getClassLoader();

      if (pcl instanceof Closeable) {
        try {
          ((Closeable) pcl).close();
        } catch (IOException ex) {
          logger.warn("could not close plugin classloader", ex);
        }
      }
    }

    context = null;
    contextListener = null;
  }

  /**
   * Method description
   *
   *
   * @param sce
   */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    context = sce.getServletContext();

    File pluginDirectory = getPluginDirectory();

    try {
      if (!isCorePluginExtractionDisabled()) {
        extractCorePlugins(context, pluginDirectory);
      } else {
        logger.info("core plugin extraction is disabled");
      }

      ClassLoader cl = ClassLoaders.getContextClassLoader(BootstrapContextListener.class);

      Set<PluginWrapper> plugins = PluginsInternal.collectPlugins(cl, pluginDirectory.toPath());

      DefaultPluginLoader pluginLoader = new DefaultPluginLoader(context, cl, plugins);

      Module scmContextListenerModule = new ScmContextListenerModule();
      BootstrapModule bootstrapModule = new BootstrapModule(pluginLoader);
      ScmInitializerModule scmInitializerModule = new ScmInitializerModule();
      EagerSingletonModule eagerSingletonModule = new EagerSingletonModule();
      ScmEventBusModule scmEventBusModule = new ScmEventBusModule();

      Injector bootstrapInjector =
        Guice.createInjector(
          bootstrapModule,
          scmContextListenerModule,
          scmEventBusModule,
          scmInitializerModule,
          eagerSingletonModule
        );

      processUpdates(pluginLoader, bootstrapInjector);

      contextListener = bootstrapInjector.getInstance(ScmContextListener.Factory.class).create(cl, plugins);
    } catch (IOException ex) {
      throw new PluginLoadException("could not load plugins", ex);
    }

    contextListener.contextInitialized(sce);

    // register for restart events
    if (!registered
      && (SCMContext.getContext().getStage() == Stage.DEVELOPMENT)) {
      logger.info("register for restart events");
      ScmEventBus.getInstance().register(this);
      registered = true;
    }
  }

  private void processUpdates(DefaultPluginLoader pluginLoader, Injector bootstrapInjector) {
    Injector updateInjector = bootstrapInjector.createChildInjector(new UpdateStepModule(pluginLoader));

    UpdateEngine updateEngine = updateInjector.getInstance(UpdateEngine.class);
    updateEngine.update();
  }

  private boolean isCorePluginExtractionDisabled() {
    return Boolean.getBoolean("sonia.scm.boot.disable-core-plugin-extraction");
  }

  /**
   * Method description
   *
   *
   * @param context
   * @param pluginDirectory
   * @param entry
   *
   * @throws IOException
   */
  private void extractCorePlugin(ServletContext context, File pluginDirectory,
                                 PluginIndexEntry entry)
    throws IOException {
    URL url = context.getResource(PLUGIN_DIRECTORY.concat(entry.getName()));
    SmpArchive archive = SmpArchive.create(url);
    Plugin plugin = archive.getPlugin();

    File directory = PluginsInternal.createPluginDirectory(pluginDirectory,
      plugin);
    File checksumFile = PluginsInternal.getChecksumFile(directory);

    if (!directory.exists()) {
      logger.warn("install plugin {}", plugin.getInformation().getId());
      PluginsInternal.extract(archive, entry.getChecksum(), directory,
        checksumFile, true);
    } else if (!checksumFile.exists()) {
      logger.warn("plugin directory {} exists without checksum file.",
        directory);
      PluginsInternal.extract(archive, entry.getChecksum(), directory,
        checksumFile, true);
    } else {
      String checksum = Files.toString(checksumFile, Charsets.UTF_8).trim();

      if (checksum.equals(entry.getChecksum())) {
        logger.debug("plugin {} is up to date",
          plugin.getInformation().getId());
      } else {
        logger.warn("checksum mismatch of pluing {}, start update",
          plugin.getInformation().getId());
        PluginsInternal.extract(archive, entry.getChecksum(), directory,
          checksumFile, true);
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param context
   * @param pluginDirectory
   *
   * @throws IOException
   */
  private void extractCorePlugins(ServletContext context, File pluginDirectory) throws IOException {
    IOUtil.mkdirs(pluginDirectory);

    PluginIndex index = readCorePluginIndex(context);

    for (PluginIndexEntry entry : index) {
      extractCorePlugin(context, pluginDirectory, entry);
    }
  }

  /**
   * Method description
   *
   *
   * @param context
   *
   * @return
   */
  private PluginIndex readCorePluginIndex(ServletContext context) {
    PluginIndex index = null;

    try {
      URL indexUrl = context.getResource(PLUGIN_COREINDEX);

      if (indexUrl == null) {
        throw new PluginException("no core plugin index found");
      }

      index = JAXB.unmarshal(indexUrl, PluginIndex.class);
    } catch (MalformedURLException ex) {
      throw new PluginException("could not load core plugin index", ex);
    } catch (DataBindingException ex) {
      throw new PluginException("could not unmarshall core plugin index", ex);
    }

    return index;
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  private File getPluginDirectory() {
    File baseDirectory = SCMContext.getContext().getBaseDirectory();

    return new File(baseDirectory, DIRECTORY_PLUGINS);
  }

  //~--- inner classes --------------------------------------------------------

  /**
   * Class description
   *
   *
   * @version Enter version here..., 14/07/09
   * @author Enter your name here...
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "plugin-index")
  private static class PluginIndex implements Iterable<PluginIndexEntry> {

    /**
     * Method description
     *
     *
     * @return
     */
    @Override
    public Iterator<PluginIndexEntry> iterator() {
      return getPlugins().iterator();
    }

    //~--- get methods --------------------------------------------------------

    /**
     * Method description
     *
     *
     * @return
     */
    public List<PluginIndexEntry> getPlugins() {
      if (plugins == null) {
        plugins = ImmutableList.of();
      }

      return plugins;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    @XmlElement(name = "plugins")
    private List<PluginIndexEntry> plugins;
  }


  /**
   * Class description
   *
   *
   * @version Enter version here..., 14/07/09
   * @author Enter your name here...
   */
  @XmlRootElement(name = "plugins")
  @XmlAccessorType(XmlAccessType.FIELD)
  private static class PluginIndexEntry {

    /**
     * Method description
     *
     *
     * @return
     */
    public String getChecksum() {
      return checksum;
    }

    /**
     * Method description
     *
     *
     * @return
     */
    public String getName() {
      return name;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private String checksum;

    /** Field description */
    private String name;
  }


  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private ServletContext context;

  /** Field description */
  private ScmContextListener contextListener;

  /** Field description */
  private boolean registered = false;

  private static class ScmContextListenerModule extends AbstractModule {
    @Override
    protected void configure() {

      install(new FactoryModuleBuilder().build(ScmContextListener.Factory.class));
    }
  }
}
