package org.eclipse.slm.awx_jwt_authenticator.awx.model

data class Survey(
    var name: String,
    var description: String,
    var spec: MutableList<SurveyItem>
) {
    constructor(surveyDTOApi: SurveyDTOApi) : this(
        surveyDTOApi.name,
        surveyDTOApi.description,
        ArrayList()
    ) {
        surveyDTOApi.spec.forEach {
            this.spec.add(SurveyItem(it))
        }
    }
}
