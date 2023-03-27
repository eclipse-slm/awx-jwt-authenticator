package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class CredentialDTOApiCreate(
    val name: String,
    val description: String,
    val organization: Int?,
    val credential_type: Int,
    val inputs: Map<String, String>
)
