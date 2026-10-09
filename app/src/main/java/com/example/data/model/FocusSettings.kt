package com.example.data.model

enum class CallPriorityMode(val labelIt: String, val descriptionIt: String) {
    ANY_CALLER(
        "Tutte le Chiamate",
        "Qualsiasi numero o contatto farà squillare il telefono normalmente"
    ),
    CONTACTS_ONLY(
        "Solo Contatti",
        "Squillano solo le chiamate da numeri salvati nella rubrica"
    ),
    STARRED_ONLY(
        "Solo Preferiti",
        "Squillano solo i contatti contrassegnati come preferiti"
    ),
    REPEAT_CALLERS(
        "Chiamanti Ripetuti",
        "Consente la suoneria se la stessa persona richiama entro 15 minuti"
    )
}

data class FocusSettings(
    val isMasterEnabled: Boolean = true,
    val callPriorityMode: CallPriorityMode = CallPriorityMode.ANY_CALLER,
    val muteNotificationStream: Boolean = true,
    val allowAlarms: Boolean = true,
    val gracePeriodSeconds: Int = 4, // delay before deactivating on pause (e.g. for track skips)
    val notifyOnModeChange: Boolean = true,
    val snoozeMessages: Boolean = false,
    val simulationMode: Boolean = false
)
