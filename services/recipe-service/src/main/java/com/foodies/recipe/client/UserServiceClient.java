package com.foodies.recipe.client;

public interface UserServiceClient {

    /**
     * Resolves the caller behind the given bearer token by delegating to
     * user-service's own GET /api/users/me — recipe-service never validates
     * JWTs itself.
     */
    UserDto getCurrentUser(String authorizationHeader);
}
