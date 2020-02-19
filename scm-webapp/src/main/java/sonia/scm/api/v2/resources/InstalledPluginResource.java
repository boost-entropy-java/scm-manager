package sonia.scm.api.v2.resources;

import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import de.otto.edison.hal.HalRepresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import sonia.scm.plugin.AvailablePlugin;
import sonia.scm.plugin.InstalledPlugin;
import sonia.scm.plugin.PluginManager;
import sonia.scm.plugin.PluginPermissions;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

public class InstalledPluginResource {

  private final PluginDtoCollectionMapper collectionMapper;
  private final PluginDtoMapper mapper;
  private final PluginManager pluginManager;

  @Inject
  public InstalledPluginResource(PluginManager pluginManager, PluginDtoCollectionMapper collectionMapper, PluginDtoMapper mapper) {
    this.pluginManager = pluginManager;
    this.collectionMapper = collectionMapper;
    this.mapper = mapper;
  }

  /**
   * Returns a collection of installed plugins.
   *
   * @return collection of installed plugins.
   */
  @GET
  @Path("")
  @Produces(VndMediaType.PLUGIN_COLLECTION)
  @Operation(
    summary = "Find all installed plugins",
    description = "Returns a collection of installed plugins.",
    tags = "Plugin Management"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = VndMediaType.PLUGIN_COLLECTION,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"plugin:read\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getInstalledPlugins() {
    PluginPermissions.read().check();
    List<InstalledPlugin> plugins = pluginManager.getInstalled();
    List<AvailablePlugin> available = pluginManager.getAvailable();
    return Response.ok(collectionMapper.mapInstalled(plugins, available)).build();
  }

  /**
   * Updates all installed plugins.
   */
  @POST
  @Path("/update")
  @Operation(
    summary = "Update all installed plugins",
    description = "Updates all installed plugins to the latest compatible version.",
    tags = "Plugin Management"
  )
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"plugin:manage\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response updateAll() {
    pluginManager.updateAll();
    return Response.ok().build();
  }

  /**
   * Returns the installed plugin with the given id.
   *
   * @param name name of plugin
   * @return installed plugin with specified id
   */
  @GET
  @Path("/{name}")
  @Produces(VndMediaType.PLUGIN)
  @Operation(
    summary = "Get installed plugin by name",
    description = "Returns the installed plugin with the given id",
    tags = "Plugin Management"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = VndMediaType.PLUGIN,
      schema = @Schema(implementation = PluginDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"plugin:read\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, plugin by given id could not be found")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getInstalledPlugin(@PathParam("name") String name) {
    PluginPermissions.read().check();
    Optional<InstalledPlugin> pluginDto = pluginManager.getInstalled(name);
    List<AvailablePlugin> available = pluginManager.getAvailable();
    if (pluginDto.isPresent()) {
      return Response.ok(mapper.mapInstalled(pluginDto.get(), available)).build();
    } else {
      throw notFound(entity("Plugin", name));
    }
  }

  /**
   * Triggers plugin uninstall.
   *
   * @param name plugin name
   * @return HTTP Status.
   */
  @POST
  @Path("/{name}/uninstall")
  @Operation(
    summary = "Uninstall plugin",
    description = "Add plugin uninstall to pending queue. The plugin will be removed on restart.",
    tags = "Plugin Management"
  )
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"plugin:manage\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response uninstallPlugin(@PathParam("name") String name, @QueryParam("restart") boolean restartAfterInstallation) {
    pluginManager.uninstall(name, restartAfterInstallation);
    return Response.ok().build();
  }
}
