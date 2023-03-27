package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class CredentialTypeDTOApiCreate(
    val name: String,
    val description: String,
    val kind: String,
    val inputs: Map<String, List<org.eclipse.slm.awx_jwt_authenticator.awx.model.CredentialTypeInputFieldItem>>,
    val injectors: Map<String, Map<String, String>>
)
