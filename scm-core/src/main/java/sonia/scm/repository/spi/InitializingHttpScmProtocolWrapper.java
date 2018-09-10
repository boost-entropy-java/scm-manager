package sonia.scm.repository.spi;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.api.v2.resources.UriInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.web.filter.PermissionFilter;
import sonia.scm.web.filter.ProviderPermissionFilter;

import javax.inject.Provider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class InitializingHttpScmProtocolWrapper {

  private static final Logger logger =
    LoggerFactory.getLogger(InitializingHttpScmProtocolWrapper.class);

  private final Provider<? extends HttpServlet> delegateProvider;
  private final Provider<? extends ProviderPermissionFilter> permissionFilterProvider;
  private final Provider<UriInfoStore> uriInfoStore;

  private volatile boolean isInitialized = false;


  protected InitializingHttpScmProtocolWrapper(Provider<? extends HttpServlet> delegateProvider, Provider<? extends ProviderPermissionFilter> permissionFilterProvider, Provider<UriInfoStore> uriInfoStore) {
    this.delegateProvider = delegateProvider;
    this.permissionFilterProvider = permissionFilterProvider;
    this.uriInfoStore = uriInfoStore;
  }

  protected void initializeServlet(ServletConfig config, HttpServlet httpServlet) throws ServletException {
    httpServlet.init(config);
  }

  public HttpScmProtocol get(Repository repository) {
    return new ProtocolWrapper(repository);
  }

  private class ProtocolWrapper extends HttpScmProtocol {

    public ProtocolWrapper(Repository repository) {
      super(repository, uriInfoStore.get().get());
    }

    @Override
    public void serve(HttpServletRequest request, HttpServletResponse response, ServletConfig config) throws ServletException, IOException {
      if (!isInitialized) {
        synchronized (InitializingHttpScmProtocolWrapper.this) {
          if (!isInitialized) {
            HttpServlet httpServlet = delegateProvider.get();
            initializeServlet(config, httpServlet);
            isInitialized = true;
          }
        }
      }

      if (getRepository() != null)
      {
        Subject subject = SecurityUtils.getSubject();

        PermissionFilter permissionFilter = permissionFilterProvider.get();
        boolean writeRequest = permissionFilter.isWriteRequest(request);

        if (permissionFilter.hasPermission(getRepository(), writeRequest))
        {
//          logger.trace("{} access to repository {} for user {} granted",
//            getActionAsString(writeRequest), repository.getName(),
//            getUserName(subject));

          delegateProvider.get().service(request, response);
        }
        else
        {
//          logger.info("{} access to repository {} for user {} denied",
//            getActionAsString(writeRequest), repository.getName(),
//            getUserName(subject));

          permissionFilter.sendAccessDenied(request, response, subject);
        }
      }

    }
  }

}
