import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*

description = "Smtp Server"

// version
val vertxVersion = rootProject.ext.get("vertxVersion").toString()
val simpleEmailVersion = rootProject.ext.get("simpleEmailVersion").toString()
val antJschVersion = rootProject.ext.get("antJschVersion").toString()

// ant
val sshAntTask = configurations.create("sshAntTask")

dependencies {

  implementation(project(":bytle-vertx"))
  implementation(project(":bytle-s3"))
  implementation(project(":bytle-type"))
  // Needed for the DNS checks (Spf, block list)
  implementation(project(":bytle-dns"))

  sshAntTask("org.apache.ant:ant-jsch:$antJschVersion")

  // Does not pass a basic test to find a SPF record ...
  // we put it in test only
  // https://mvnrepository.com/artifact/org.apache.james.jspf/apache-jspf-resolver
  testImplementation("org.apache.james.jspf:apache-jspf-resolver:1.0.3") {
    exclude(group = "commons-cli", module = "commons-cli")
    because("We don't use the cli")
  }

  /**
   * For the prometheus metrics
   */
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-micrometer-metrics:$vertxVersion")

  /**
   * To test chunking (ie BDAT command), otherwise we get this error.
   * Suppressed: org.simplejavamail.internal.moduleloader.ModuleLoaderException: Batch module not found,
   * make sure it is on the classpath (https://github.com/bbottema/simple-java-mail/tree/develop/modules/batch-module)
   * 		at app//org.simplejavamail.internal.moduleloader.ModuleLoader.loadModule(ModuleLoader.java:133)
   * 		at app//org.simplejavamail.internal.moduleloader.ModuleLoader.loadBatchModule(ModuleLoader.java:95)
   * 		at app//org.simplejavamail.mailer.internal.MailerImpl.shutdownConnectionPool(MailerImpl.java:395)
   */
  testImplementation("org.simplejavamail:batch-module:$simpleEmailVersion")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi(project(":bytle-vertx"))


}

plugins {
  // https://github.com/jponge/vertx-gradle-plugin
  id("io.vertx.vertx-plugin")
}

val smtpVerticle = "net.bytle.smtp.SmtpVerticle"
val vertxLauncher = "net.bytle.vertx.MainLauncher"
vertx {
  // the launcher in the MANIFEST must be set here otherwise the plugin takes the default
  // even if we set another one in the ShadowJar task
  launcher = vertxLauncher
  mainVerticle = smtpVerticle
  vertxVersion
}


val shadowJarTaskName = "shadowJar"
tasks.named<ShadowJar>(shadowJarTaskName) {

  mergeServiceFiles()
  // Doc https://vertx.io/docs/vertx-core/java/#_using_the_launcher_in_fat_jars
  manifest {
    attributes(
      mapOf(
        "Main-Class" to vertxLauncher,
        "Main-Verticle" to smtpVerticle,
        "Main-Command" to "run",
        "FatJar" to "yes",
        "Multi-Release" to "true"
      )
    )

  }

}

val deployFlyTaskName = "fly"
tasks.register(deployFlyTaskName) {
  dependsOn(shadowJarTaskName)
  doLast {

    println("Fly Update Image")
    exec {
      // fly deploy -c </path/to/fly.toml>
      commandLine(
        "fly",
        "deploy",
        "--local-only"
      )
    }

    println("Fly completed")

  }
}

val deployServerTaskName = "deploy"
tasks.register(deployServerTaskName) {
  dependsOn(shadowJarTaskName)
  doLast {

    // Variable
    val backendServerHost: String by project
    val backendServerPort: String by project
    val backendUserName: String by project
    val backendUserPwd: String by project
    val backendAppName = "inbox"
    val backendAppHome = "/opt/apps/${backendAppName}"
    val backendAppArchive = "bytle-smtp-server-all"

    /**
     * Upload the file
     */
    ant.withGroovyBuilder {
      "taskdef"(
        "name" to "scp",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
        "classpath" to sshAntTask.asPath
      )
    }

    val timeStamp = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss").format(Calendar.getInstance().time)
    val appFile = "${backendAppArchive}.jar"
    val deploymentFile = "build/libs/$appFile"
    val remoteDeploymentFile = "${backendAppArchive}-to-deploy-${timeStamp}.jar"

    println("Uploading the deployment file to ${backendAppHome}/$remoteDeploymentFile")
    // https://ant.apache.org/manual/Tasks/scp.html
    ant.withGroovyBuilder {
      "scp"(
        "file" to deploymentFile,
        "remoteTofile" to "${backendUserName}@${backendServerHost}:${backendAppHome}/$remoteDeploymentFile",
        "sftp" to "true",
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to backendUserPwd,
        "verbose" to true,
        "failonerror" to true
      )
    }
    println("Upload completed")

    // Configuration file
    val configurationFile = "deploy/inbox.eraldy.com/.smtp-server-inbox.yml"
    val remoteConfigurationFile = "${backendAppHome}/.smtp-server.yml"
    println("Uploading the configuration file to ${remoteConfigurationFile}")
    // https://ant.apache.org/manual/Tasks/scp.html
    ant.withGroovyBuilder {
      "scp"(
        "file" to configurationFile,
        "remoteTofile" to "${backendUserName}@${backendServerHost}:${remoteConfigurationFile}",
        "sftp" to "true",
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to backendUserPwd,
        "verbose" to true,
        "failonerror" to true
      )
    }
    println("Upload completed")

    /**
     * Stop the service and rename the jar file
     */
    println("Restarting the service")
    // Add the class path for sshexec

    ant.withGroovyBuilder {
      "taskdef"(
        "name" to "sshexec",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
        "classpath" to sshAntTask.asPath
      )
    }
    // https://ant.apache.org/manual/Tasks/sshexec.html
    // The && means that they should all be successful
    val backupFile = "${backendAppArchive}-backup-${timeStamp}.jar"
    val command =
      "sudo appctl stop $backendAppName &&" +
        " mv ${backendAppHome}/$appFile ${backendAppHome}/${backupFile} &&" +
        " mv ${backendAppHome}/$remoteDeploymentFile ${backendAppHome}/${appFile} &&" +
        " sudo appctl start $backendAppName &&" +
        " echo Stop, Move and Start Done"
    println("with the command")
    println(command)
    ant.withGroovyBuilder {
      "sshexec"(
        "command" to command,
        "host" to backendServerHost,
        "username" to backendUserName,
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to backendUserPwd,
        "verbose" to true,
        "failonerror" to true
      )
    }
    println("Restart completed")
  }
}
