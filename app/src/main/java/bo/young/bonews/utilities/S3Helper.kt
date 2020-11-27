package bo.young.bonews.utilities

import android.util.Log
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageListOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import java.io.File

class S3Helper {

    fun uploadFile(key: String, file: File) {

        Amplify.Storage.uploadFile(
            key,
            file,
            { result -> Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()) },
            { error -> Log.e("MyAmplifyApp", "Upload failed", error) }
        )
    }

    private fun uploadFile(key: String, file: File, accessLevel: StorageAccessLevel) {
        val options = StorageUploadFileOptions.builder()
            .accessLevel(accessLevel)
            .build()

        Amplify.Storage.uploadFile(
            key,
            file,
            options,
            { result -> Log.i("MyAmplifyApp", "Successfully uploaded: $key" )},
            { error -> Log.e("MyAmplifyApp", "Upload failed", error)}
        )
    }

    private fun downloadFile(file: File, key: String) {
        Amplify.Storage.downloadFile(
            key,
            file,
            { Log.i("MyAmplifyApp", "Successfully downloaded: $key") },
            { error -> Log.e("MyAmplifyApp", "Download failed", error) })
    }


    private fun downloadFile(file: File, key: String, userId: String, accessLevel: StorageAccessLevel) {
        val options = StorageDownloadFileOptions.builder()
            .accessLevel(accessLevel)
            .targetIdentityId(userId)
            .build()

        Amplify.Storage.downloadFile(
            key,
            file,
            options,
            { Log.i("MyAmplifyApp", "Successfully downloaded: $key") },
            { error -> Log.e("MyAmplifyApp", "Download failed", error) })
    }

    private fun listFile() {
        Amplify.Storage.list(
            "",
            { result ->
                result.getItems().forEach { item ->
                    Log.i("MyAmplifyApp", "Item: " + item.getKey())
                }
            },
            { error -> Log.e("MyAmplifyApp", "List failure", error) }
        )
    }

    private fun listFile(userId: String, accessLevel: StorageAccessLevel) {

        val options = StorageListOptions.builder()
            .accessLevel(accessLevel)
            .targetIdentityId(userId)
            .build()

        Amplify.Storage.list(
            "",
            options,
            { result ->
                result.getItems().forEach { item ->
                    Log.i("AmplifyApplication", "Item: " + item)
                }
            },
            { error -> Log.e("MyAmplifyApp", "List failure", error) }
        )

    }


}