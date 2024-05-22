package sapala.s2sauthservice.buildmanagerservice.build

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import sapala.s2sauthservice.api.S2sTokenService
import sapala.s2sauthservice.buildmanagerservice.config.Env
import java.io.File
import java.io.FileOutputStream

@Service
class RemoteBuildService(
    private val env: Env,
    private val client: OkHttpClient,
    private val tokenService: S2sTokenService,
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RemoteBuildService::class.java)
    }

    fun remoteBuild(dependency: Dependency, zipFile: File): File {
        log.info("Remote building dependency $dependency")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("zippedFile", zipFile.name, zipFile.asRequestBody("application/zip".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder()
            .url("${env.buildServiceUrl()}/api/v1/build")
            .post(requestBody)
            .header("Authorization", "Bearer ${tokenService.s2sToken}")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            log.error("Remote build dependency failed with code ${response.code}: ${response.body?.string()}")
            throw RuntimeException()
        }
        log.info("Remote building dependency $dependency was successfull")
        val outputFile = zipFile.parentFile.resolve(dependency.jarName())
        FileOutputStream(outputFile).use { it.write(response.body!!.bytes()) }
        return outputFile
    }
}
