package org.eclipse.slm.awx_jwt_authenticator.service.impl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

@Configuration
public class VaultConfiguration extends AbstractVaultConfiguration {

    public final static Logger LOG = LoggerFactory.getLogger(VaultConfiguration.class);

    private String scheme;

    private String host;

    private int port;

    private String authenticationMethod;

    private String token;

    private String appRoleId;

    private String appRoleSecret;



    public VaultConfiguration(
            @Value("${vault.scheme}") String scheme,
            @Value("${vault.host}") String host,
            @Value("${vault.port}") int port,
            @Value("${vault.authentication}") String authenticationMethod,
            @Value("${vault.token:}") String token,
            @Value("${vault.app-role.role-id:}") String appRoleId,
            @Value("${vault.app-role.secret-id:}") String appRoleSecret)
    {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.authenticationMethod = authenticationMethod;
        this.token = token;
        this.appRoleId = appRoleId;
        this.appRoleSecret = appRoleSecret;

        LOG.info("appRoleId: " + appRoleId);
        LOG.info("appRoleSecret: " + appRoleSecret);
        LOG.info("scheme: " + scheme);
        LOG.info("host: " + host);
        LOG.info("port: " + port);
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        if (this.authenticationMethod.equalsIgnoreCase("TOKEN"))
        {
            return new TokenAuthentication(this.token);
        }
        else if (this.authenticationMethod.equalsIgnoreCase("APPROLE")) {
            var roleId = AppRoleAuthenticationOptions.RoleId.provided(this.appRoleId);
            var secretId = AppRoleAuthenticationOptions.SecretId.provided(this.appRoleSecret);

            var builder = AppRoleAuthenticationOptions
                    .builder().roleId(roleId).secretId(secretId);

            return new AppRoleAuthentication(builder.build(), restOperations());
        }
        else
        {
            throw new IllegalArgumentException("Authentication method '" + this.authenticationMethod + "' unkown");
        }
    }

    @Override
    public VaultEndpoint vaultEndpoint() {
        var vaultEndpoint = VaultEndpoint.create(this.host, this.port);
        vaultEndpoint.setScheme(this.scheme);
        return vaultEndpoint;
    }
}
