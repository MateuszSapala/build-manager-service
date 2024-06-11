package sapala.s2sauthservice.buildmanagerservice.build

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@RestController
@RequestMapping("/build-manager-service/api/v1/build")
class BuildController(private val buildService: BuildService) {
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun build(@RequestParam("zippedFile") zippedFile: MultipartFile): ByteArrayResource {
        val zip = buildService.build(zippedFile)
        return ByteArrayResource(Files.readAllBytes(zip.toPath()))
    }
}
