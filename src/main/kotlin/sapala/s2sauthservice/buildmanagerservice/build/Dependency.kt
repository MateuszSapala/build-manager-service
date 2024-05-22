package sapala.s2sauthservice.buildmanagerservice.build

data class Dependency(val groupId: String, val artifactId: String,val version: String?) {
    override fun toString() = "$groupId.$artifactId"

    fun jarName() = "$artifactId-$version.jar"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dependency

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }
}
