package com.letssign.mobile.navigation

object Routes {
    const val LOGIN = "login"
    const val ADMIN_HOME = "admin_home"
    const val SIGNER_HOME = "signer_home"

    const val SIGNER_FORM = "signer_form?signerId={signerId}"
    fun signerForm(signerId: Long? = null) = "signer_form?signerId=${signerId ?: -1}"

    const val DOCUMENT_WIZARD_GRAPH = "document_wizard"
    const val WIZARD_TITLE = "wizard_title"
    const val WIZARD_SIGNERS = "wizard_signers"
    const val WIZARD_POSITIONING = "wizard_positioning"
    const val WIZARD_REVIEW = "wizard_review"
    const val WIZARD_RESULT = "wizard_result"

    const val DOCUMENT_DETAIL = "document_detail/{documentId}"
    fun documentDetail(documentId: Long) = "document_detail/$documentId"

    const val SIGN_DOCUMENT = "sign_document/{documentSignerId}"
    fun signDocument(documentSignerId: Long) = "sign_document/$documentSignerId"
}
