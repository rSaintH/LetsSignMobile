package com.letssign.mobile.data.local

import androidx.room.TypeConverter
import com.letssign.mobile.data.local.entity.DocumentEventType
import com.letssign.mobile.data.local.entity.DocumentStatus
import com.letssign.mobile.data.local.entity.SignatureStatus
import com.letssign.mobile.data.local.entity.SignerClassification
import com.letssign.mobile.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name
    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)

    @TypeConverter
    fun fromSignerClassification(value: SignerClassification): String = value.name
    @TypeConverter
    fun toSignerClassification(value: String): SignerClassification = SignerClassification.valueOf(value)

    @TypeConverter
    fun fromSignatureStatus(value: SignatureStatus): String = value.name
    @TypeConverter
    fun toSignatureStatus(value: String): SignatureStatus = SignatureStatus.valueOf(value)

    @TypeConverter
    fun fromDocumentEventType(value: DocumentEventType): String = value.name
    @TypeConverter
    fun toDocumentEventType(value: String): DocumentEventType = DocumentEventType.valueOf(value)
}
