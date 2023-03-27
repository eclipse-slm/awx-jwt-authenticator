package org.eclipse.slm.awx_jwt_authenticator.awx.model

import java.util.*

data class ProjectUpdateRun(
    var project_update: Int?,
    var id: Int?,
    var type: String?,
    var url: String?,
    var related: Map<String?, String>?,
    var summary_fields: Map<String?, Any>?,
    var created: Date?,
    var modified: Date?,
    var name: String?,
    var description: String?,
    var local_path: String?,
    var scm_type: String?,
    var scm_url: String?,
    var scm_branch: String?,
    var scm_refspec: String?,
    var scm_clean: Boolean? = false,
    var scm_delete_on_update: Boolean? = false,
    var timeout: Int?,
    var scm_revision: String?,
    var unified_job_template: Int?,
    var launch_type: String,
    var status: String?,
    var failed: Boolean,
    var elapsed: Float,
    var job_args: String,
    var job_cwd: String,
    var job_env: Map<String,String>,
    var job_explanation: String,
    var execution_node: String,
    var result_traceback: String,
    var event_processing_finished: Boolean,
    var project: Int,
    var job_type: String,
    var job_tags: String
)
