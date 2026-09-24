package com.foodies.recipe.client.impl;

import com.foodies.recipe.client.UserAuthenticationException;
import com.foodies.recipe.client.UserDto;
import com.foodies.recipe.client.UserServiceClient;
import com.foodies.recipe.client.UserServiceUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClientImpl implements UserServiceClient {

    private final RestClient userServiceRestClient;

    public UserServiceClientImpl(RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttemptsExpression = "${user-service.retry.max-attempts}",
            backoff = @Backoff(delay = 500))
    public UserDto getCurrentUser(String authorizationHeader) {
        try {
            return userServiceRestClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserDto.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new UserAuthenticationException("Token invalide ou expiré", ex);
        } catch (HttpClientErrorException ex) {
            throw new UserServiceUnavailableException(
                    "Réponse inattendue de user-service: " + ex.getStatusCode(), ex);
        }
    }

    @Recover
    UserDto recoverFromConnectivityFailure(ResourceAccessException ex, String authorizationHeader) {
        throw new UserServiceUnavailableException("user-service injoignable après plusieurs tentatives", ex);
    }

    @Recover
    UserDto recoverFromServerError(HttpServerErrorException ex, String authorizationHeader) {
        throw new UserServiceUnavailableException("user-service en erreur après plusieurs tentatives", ex);
    }

    // Spring Retry routes every exception thrown by a @Retryable method through
    // recovery as soon as any @Recover method exists on the class - even for
    // exception types outside retryFor (e.g. UserAuthenticationException below,
    // thrown on the very first attempt with no retry involved). Without this
    // catch-all, such exceptions surface as a misleading ExhaustedRetryException
    // instead of propagating as-is.
    @Recover
    UserDto recoverFromOtherFailure(RuntimeException ex, String authorizationHeader) {
        throw ex;
    }
}
