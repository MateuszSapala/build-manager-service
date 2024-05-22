package sapala.s2sauthservice.buildmanagerservice.build

import kotlinx.coroutines.Job
import java.io.File

data class BuildJob(val dependency: Dependency, val buildDir: File, val buildJob: Job)
