package org.eclipse.slm.awx_jwt_authenticator.awx.model

import java.util.*

data class Job(
        var id: Int,
        var type: String,
        var url: String,
        var related: Map<String, String>,
        var summary_fields: Map<String, Any>,
        var created: Date? = null,
        var modified: Date? = null,
        var name: String,
        var description: String,
        var unified_job_template: Int,
        var launch_type: String,
        var status: String,
        var failed: String,
        var started: Date? = null,
        var finished: Date? = null,
        var canceled_on: String? = null,
        var elapsed: Float,
        var job_explanation: String,
        var execution_node: String,
        var controller_node: String,
        var job_type: String,
        var inventory: Int,
        var project: Int,
        var playbook: String,
        var scm_branch: String,
        var forks: Int,
        var limit: String,
        var verbosity: Int,
        var extra_vars: String,
        var job_tags: String,
        var force_handlers: Boolean,
        var skip_tags: String,
        var start_at_task: String,
        var timeout: Int,
        var use_fact_cache: Boolean,
        var organization: Int,
        var job_template: Int,
        var passwords_needed_to_start: List<String>,
        var allow_simultaneous: Boolean,
        var artifacts: Map<String, Any>,
        var scm_revision: String,
        var instance_group: Int,
        var diff_mode: Boolean,
        var job_slice_number: Int,
        var job_slice_count: Int,
        var webhook_service: String,
        var webhook_credential: String? = null,
        var webhook_guid: String,
)
