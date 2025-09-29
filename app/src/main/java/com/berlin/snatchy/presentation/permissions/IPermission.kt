package com.berlin.snatchy.presentation.permissions

sealed interface IPermission {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class ReadPermission : IPermission {
    override fun getDescription(isPermanentlyDeclined: Boolean) = if (isPermanentlyDeclined) {
        "It seems you permanently declined read files permission. " +
                "You can go to the app settings to grant it."
    } else {
        "This app needs access to read status files."
    }
}

class WritePermission : IPermission {
    override fun getDescription(isPermanentlyDeclined: Boolean) = if (isPermanentlyDeclined) {
        "It seems you permanently declined download files permission. " +
                "You can go to the app settings to grant it."
    } else {
        "This app needs access to write (download) status files."
    }
}

class ManageStoragePermission: IPermission{
    override fun getDescription(isPermanentlyDeclined: Boolean) = if (isPermanentlyDeclined) {
        "It seems you permanently declined manager permission. " +
                "You can go to the app settings to grant it."
    } else {
        "This app needs access to manager permission in order to read and write in modern android versions.."
    }
}