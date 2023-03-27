package org.eclipse.slm.awx_jwt_authenticator.service.impl.endpoints;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.eclipse.slm.awx_jwt_authenticator.awx.model.*;
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.slm.awx_jwt_authenticator.awx.model.PersonalToken;
import org.eclipse.slm.awx_jwt_authenticator.awx.model.User;
import org.eclipse.slm.awx_jwt_authenticator.awx.model.UserCreateRequest;
import org.eclipse.slm.awx_jwt_authenticator.awx.model.UserGetResponse;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

@RestController
@RequestMapping("/token")
public class AuthenticatorRestController {

    Logger logger = LoggerFactory.getLogger(AuthenticatorRestController.class);

    WebClient awxWebClient;

    @Value("${awx.host}")
    String awxHostname;

    @Value("${awx.scheme}")
    String awxScheme;

    @Value("${awx.port}")
    String awxPort;

    @Value("${awx.username}")
    String awxUsername;

    @Value("${awx.password}")
    String awxPassword;

    @Value("${awx.organization-name}")
    String organizationName;

    @Value("${awx.team-name}")
    String teamName;

    @Value("${vault.kv-mount-path}")
    String vaultKvMountPath;

    @Autowired
    public VaultOperations operations;

    private VaultKeyValueOperations keyValueOperations;

    @PostConstruct
    public void init() {
        String awxBaseUrl = this.awxScheme + "://" + this.awxHostname + ":" + this.awxPort + "/api";

        this.keyValueOperations = this.operations.opsForKeyValue(
                this.vaultKvMountPath,
                VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);

        final ObjectMapper mapper = new ObjectMapper()
                .registerModule(new KotlinModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper)))
                .build();

        this.awxWebClient = WebClient.builder()
                .baseUrl(awxBaseUrl)
                .exchangeStrategies(exchangeStrategies)
                .build();

        if(!checkOrganizationExists(this.organizationName)) {
            logger.info("Organization '"+ this.organizationName +"' does not exist.");
            createOrganization(this.organizationName);
        }

        if(!checkTeamExists(this.teamName, this.organizationName)) {
            logger.info("Team '"+ this.teamName +"' in Organization '"+ this.organizationName +"' does not exist!");
            createTeam(this.teamName, this.organizationName);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    @Operation(summary = "Get AWX Access token based on Keycloak jwt token")
    public String getAccessToken(@RequestHeader String Authorization) {
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessToken accessToken = keycloakPrincipal.getKeycloakSecurityContext().getToken();

        String firstName = accessToken.getGivenName();
        String lastName = accessToken.getFamilyName();
        String username = accessToken.getPreferredUsername();
        String email = accessToken.getEmail();
        String id = accessToken.getId();

        User user = this.createAwxUser(firstName, lastName, username, email);
        return this.createOAuthToken(user);
    }

    private boolean checkOrganizationExists(String organizationName) {
        if(getOrganization(organizationName) == null)
            return false;
        else
            return true;
    }

    private void createOrganization(String organizationName) {
        String url = "/v2/organizations/";

        OrganizationCreateRequest request = new OrganizationCreateRequest(organizationName, "", 0, "");

        String response = this.awxWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
    }

    private boolean checkTeamExists(String teamName, String organizationName) {
        Organization organization = getOrganization(organizationName);

        if(getTeam(teamName, organization.getId()) == null)
            return false;
        else
            return true;
    }

    private void createTeam(String teamName, String organizationName) {
        String url = "/v2/teams/";
        Organization organization = getOrganization(organizationName);
        TeamCreateRequest request = new TeamCreateRequest(teamName, "", organization.getId());

        String response = this.awxWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
    }

    private String createOAuthToken(User user) {
        String url = "/v2/users/" + user.getId() + "/personal_tokens/";

        VaultResponse vaultResponse = keyValueOperations.get("user/"+user.getUsername());
        String password = (String) vaultResponse.getData().get("password");

        PersonalToken personalToken = this.awxWebClient.post()
                .uri(url)
                .headers(headers -> headers.setBasicAuth(user.getUsername(), password))
                .retrieve()
                .bodyToMono(PersonalToken.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();

        logger.info("Created access token for user '" + user.getUsername() + "'");

        return personalToken.getToken();
    }

    private User getAwxUser(String username) {
        String url = "/v2/users/";

        UserGetResponse response = this.awxWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(url)
                    .queryParam("username", username)
                    .build())
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .retrieve()
                .bodyToMono(UserGetResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();

        return response.getResults()[0];
    }

    private User createAwxUser(String firstName, String lastName, String username, String email) {
        String password = this.createRandomPassword();

        String url = "/v2/users/";

        UserCreateRequest userCreateRequest = new UserCreateRequest(
                email,
                firstName,
                lastName,
                username,
                password,
                false,
                false
        );

        try {
            String response = this.awxWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
//                    .headers(headers -> headers.setBearerAuth(awxAdminAccessToken))
                    .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                    .body(BodyInserters.fromValue(userCreateRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            logger.info("User has been created.");

            User user = this.getAwxUser(userCreateRequest.getUsername());

            this.savePasswordToVault(user, password);
            this.addUserToTeam(user);
        } catch(WebClientResponseException e) {
            if(e.getStatusCode() == HttpStatus.BAD_REQUEST //&&
//                e.getResponseBodyAsString().contains("A user with that username already exists.")
            ) {
                logger.info("Skip create. User exists already.");
                logger.info("Skipped saving password to vault because creation of AWX User has been skipped.");
                logger.info("Skip adding user to team '" + this.teamName + "'");
            } else {
                logger.error("Status Code: " + e.getStatusCode());
                logger.error("Response Body: " + e.getResponseBodyAsString());
                logger.error("Message: " + e.getMessage());

                throw e;
            }
        }

        return getAwxUser(userCreateRequest.getUsername());
    }

    private void addUserToTeam(User user) {
        Organization organization = getOrganization(this.organizationName);
        Team team = getTeam(this.teamName, organization.getId());

        String url = "/v2/teams/" + team.getId() + "/users/";

        Map<String, Object> body = new HashMap<>();
        body.put("id", user.getId());

        String response = this.awxWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
    }

    private Organization getOrganization(String organizationName) {
        String url = "/v2/organizations/";

        Results<Organization> response = this.awxWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .queryParam("name", organizationName)
                        .build())
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Results<Organization>>() {})
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
        try {
            return response.getResults().iterator().next();
        } catch(NullPointerException e) {
            return null;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private Team getTeam(String teamName, int organizationId) {
        //String awxAdminAccessToken = getAwxAdminAccessToken();
        String url = "/v2/teams/";

        Results<Team> response = this.awxWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .queryParam("name", teamName)
                        .queryParam("organization", organizationId)
                        .build())
                .headers(headers -> headers.setBasicAuth(awxUsername, awxPassword))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Results<Team>>() {})
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();

        try {
            return response.getResults().iterator().next();
        } catch(NullPointerException e) {
            return null;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private void savePasswordToVault(User user, String password) {


        keyValueOperations.put("user/" + user.getUsername(), new HashMap<String, String>() {{
            put("username", user.getUsername());
            put("first_name", user.getFirst_name());
            put("last_name", user.getLast_name());
            put("email", user.getEmail());
            put("password", password);
        }});
        logger.info("Saved password to vault");
    }

    private String createRandomPassword() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 8;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }
}
