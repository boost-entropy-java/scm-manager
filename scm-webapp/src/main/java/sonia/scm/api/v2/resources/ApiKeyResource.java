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

package sonia.scm.api.v2.resources;

import de.otto.edison.hal.HalRepresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import sonia.scm.ContextEntry;
import sonia.scm.security.ApiKey;
import sonia.scm.security.ApiKeyService;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URI;

import static javax.ws.rs.core.Response.Status.CREATED;
import static sonia.scm.NotFoundException.notFound;

public class  ApiKeyResource {

  private final ApiKeyService apiKeyService;
  private final ApiKeyCollectionToDtoMapper apiKeyCollectionMapper;
  private final ApiKeyToApiKeyDtoMapper apiKeyMapper;
  private final ResourceLinks resourceLinks;

  @Inject
  public ApiKeyResource(ApiKeyService apiKeyService, ApiKeyCollectionToDtoMapper apiKeyCollectionMapper, ApiKeyToApiKeyDtoMapper apiKeyMapper, ResourceLinks links) {
    this.apiKeyService = apiKeyService;
    this.apiKeyCollectionMapper = apiKeyCollectionMapper;
    this.apiKeyMapper = apiKeyMapper;
    this.resourceLinks = links;
  }

  @GET
  @Path("")
  @Produces(VndMediaType.API_KEY_COLLECTION)
  @Operation(summary = "Get the api keys for the current user", description = "Returns the registered api keys for the logged in user.", tags = "User")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = VndMediaType.API_KEY_COLLECTION,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    ))
  public HalRepresentation getForCurrentUser() {
    return apiKeyCollectionMapper.map(apiKeyService.getKeys());
  }

  @GET
  @Path("{id}")
  @Produces(VndMediaType.API_KEY_COLLECTION)
  @Operation(summary = "Get one api key for the current user", description = "Returns the registered api key with the given id for the logged in user.", tags = "User")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = VndMediaType.API_KEY_COLLECTION,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(
    responseCode = "404",
    description = "not found, no api key with the given id for the current user available",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    ))
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    ))
  public ApiKeyDto get(@PathParam("id") String id) {
    return apiKeyService
      .getKeys()
      .stream()
      .filter(key -> key.getId().equals(id))
      .map(apiKeyMapper::map).findAny()
      .orElseThrow(() -> notFound(ContextEntry.ContextBuilder.entity(ApiKey.class, id)));
  }

  @POST
  @Path("")
  @Consumes(VndMediaType.API_KEY)
  @Operation(summary = "Create new api key for the current user", description = "Creates a new api key for the given user with the role specified in the given key.", tags = "User")
  @ApiResponse(
    responseCode = "201",
    description = "create success",
    headers = @Header(
      name = "Location",
      description = "uri to the created user",
      schema = @Schema(type = "string")
    ),
    content = @Content(
      mediaType = MediaType.TEXT_PLAIN
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "409", description = "conflict, a key with the given display name already exists")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    ))
  public Response create(@Valid ApiKeyDto apiKey) {
    final ApiKeyService.CreationResult newKey = apiKeyService.createNewKey(apiKey.getDisplayName(), apiKey.getRole());
    return Response.status(CREATED)
      .entity(newKey.getToken())
      .location(URI.create(resourceLinks.apiKey().self(newKey.getId())))
      .build();
  }
}
