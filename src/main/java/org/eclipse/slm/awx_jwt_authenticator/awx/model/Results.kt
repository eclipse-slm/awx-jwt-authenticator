package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class Results<T> (
    var count: Int,
    var next: String? = null,
    var previous: String? = null,
    var results: Collection<T>
)
