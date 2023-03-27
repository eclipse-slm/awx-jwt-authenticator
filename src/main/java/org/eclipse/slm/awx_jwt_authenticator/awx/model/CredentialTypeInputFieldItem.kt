package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class CredentialTypeInputFieldItem(
    val id: String,
    val label: String,
    val secret: Boolean,
    val type: String
)
