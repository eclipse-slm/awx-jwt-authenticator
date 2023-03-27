package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class JobTemplateDTOApiCreate(

    val organization: Int,

    val project: Int,

    val inventory: Int,

    val name: String,

    val playbook: String,

) {
    var ask_variables_on_launch: Boolean = true
}
