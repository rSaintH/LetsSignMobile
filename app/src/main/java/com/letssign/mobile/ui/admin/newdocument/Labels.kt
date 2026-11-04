package com.letssign.mobile.ui.admin.newdocument

import com.letssign.mobile.data.local.entity.SignerClassification

fun classificationLabelPublic(classification: SignerClassification): String = when (classification) {
    SignerClassification.CONTRATANTE -> "Contratante"
    SignerClassification.CONTRATADO -> "Contratado"
    SignerClassification.TESTEMUNHA -> "Testemunha"
}
