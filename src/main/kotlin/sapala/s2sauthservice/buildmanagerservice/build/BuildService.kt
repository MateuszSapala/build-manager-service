package sapala.s2sauthservice.buildmanagerservice.build

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter
import com.fasterxml.jackson.module.kotlin.contains
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import sapala.s2sauthservice.buildmanagerservice.config.Env
import java.io.File
import java.util.*
import java.util.zip.ZipInputStream


@Service
class BuildService(private val env: Env, private val remoteBuildService: RemoteBuildService) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BuildService::class.java)
    }

    private val mapper = XmlMapper()

    fun build(zippedFile: MultipartFile): File {
        val buildId = UUID.randomUUID()
        val buildDir = env.buildDirectory().resolve(buildId.toString())
        zippedFile.inputStream.use { inputStream ->
            ZipInputStream(inputStream).use { ZipUtil.extractZip(it, buildDir) }
        }

        val parentPom = buildDir.pomJsonNode()
        val modules = (parentPom.get("modules").get("module") as ArrayNode).map { it.textValue() }
        val childrenPoms = modules.map { Pair(buildDir.resolve(it).pomJsonNode(), buildDir.resolve(it)) }

        buildJars(childrenPoms)
        return ZipUtil.zipFiles(
            childrenPoms.map { buildDir.resolve(it.first.dependency().jarName()) },
            buildDir.resolve("jars.zip")
        )
    }

    private fun buildJars(toBuild: List<Pair<JsonNode, File>>) {
        runBlocking {
            val jobs = mutableListOf<BuildJob>()

            while (jobs.size != toBuild.size) {
                for ((pomNode, dir) in toBuild.filter { (p) -> !jobs.map { it.dependency }.contains(p.dependency()) }) {
                    val dependency = pomNode.dependency();
                    val dependencies = pomNode.getDependencies().map { it.dependency() }
                    val dependenciesToBeBuildBefore = toBuild.map { it.first.dependency() }
                        .filter { dependencies.contains(it) }
                    val canBeScheduledNow = dependenciesToBeBuildBefore.all { jobs.any { d -> d.dependency == it } }
                    if (!canBeScheduledNow) {
                        continue
                    }

                    pomNode.getDependencies().filter { dependenciesToBeBuildBefore.contains(it.dependency()) }
                        .map { (it as ObjectNode).addSystemPath() }

                    savePom(dir.resolve("pom.xml"), pomNode)

                    val job = launchBuild(dependenciesToBeBuildBefore, dependency, jobs, dir)
                    jobs.add(BuildJob(dependency, dir, job))
                }
            }
            jobs.map { it.buildJob }.joinAll()
        }
    }

    private fun CoroutineScope.launchBuild(
        dependenciesToBeBuildBefore: List<Dependency>,
        dependency: Dependency,
        jobs: MutableList<BuildJob>,
        dir: File
    ) = launch(Dispatchers.IO) {
        if (dependenciesToBeBuildBefore.isNotEmpty()) {
            log.info(
                "${dependency.groupId}.${dependency.artifactId} is waiting for building: ${
                    dependenciesToBeBuildBefore.joinToString("; ")
                }"
            )
            dependenciesToBeBuildBefore.map {
                jobs.find { buildJob -> buildJob.dependency == it }!!.buildJob
            }.joinAll()
        }
        val additionalFiles = dependenciesToBeBuildBefore.map {
            val requiredDependency = jobs.find { buildJob -> buildJob.dependency == it }!!
            requiredDependency.buildDir.parentFile.resolve(requiredDependency.dependency.jarName())
        }
        val zipFile = dir.parentFile.resolve("$dependency.zip")
        ZipUtil.zipDirectory(dir, zipFile, additionalFiles)
        remoteBuildService.remoteBuild(dependency, zipFile)
    }
            ;

    private fun savePom(pomFile: File, node: JsonNode) {
        val text = mapper.writer(DefaultXmlPrettyPrinter()).writeValueAsString(node)
            .replace(
                "<ObjectNode>",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">"
            )
            .replace("</ObjectNode>", "</project>")
            .replace(Regex("<schemaLocation>.*</schemaLocation>"), "")
        pomFile.writeText(text)
    }

    private fun File.pomJsonNode() = mapper.readTree(this.resolve("pom.xml"))
    private fun JsonNode.getDependencies() =
        if (contains("dependencies")) get("dependencies").get("dependency") as ArrayNode
        else mapper.createArrayNode()

    private fun JsonNode.groupId() = get("groupId").asText()
    private fun JsonNode.artifactId() = get("artifactId").asText()
    private fun JsonNode.version() = if (contains("version")) get("version").asText() else null
    private fun JsonNode.dependency() = Dependency(this.groupId(), this.artifactId(), this.version())

    private fun ObjectNode.addSystemPath() {
        this.put("scope", "system")
        this.put("systemPath", "\${project.basedir}/${this.dependency().jarName()}")
    }
}
