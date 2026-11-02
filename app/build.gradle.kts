import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val releaseProperties=Properties().apply {
 val config=rootProject.file(".local/release-signing.properties")
 if(config.isFile)config.inputStream().use {load(it)}
}
fun signingValue(environment:String,property:String):String?=providers.environmentVariable(environment).orNull?:releaseProperties.getProperty(property)
val releaseStore=signingValue("POCKETSURF_KEYSTORE_PATH","storeFile")
android {
 namespace="org.pocketsurf.browser"; compileSdk=35
 buildFeatures { buildConfig=true }
 defaultConfig { applicationId="org.pocketsurf.browser"; minSdk=26; targetSdk=35; versionCode=32; versionName="1.3.1" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 signingConfigs.getByName("debug") { storeFile=rootProject.file(".local/debug.keystore") }
 signingConfigs.create("release") {
  if(releaseStore!=null){
   storeFile=rootProject.file(releaseStore)
   storePassword=signingValue("POCKETSURF_STORE_PASSWORD","storePassword")
   keyAlias=signingValue("POCKETSURF_KEY_ALIAS","keyAlias")
   keyPassword=signingValue("POCKETSURF_KEY_PASSWORD","keyPassword")
  }
 }
 buildTypes.getByName("release") {
  isDebuggable=false
  if(releaseStore!=null)signingConfig=signingConfigs.getByName("release")
 }
 testOptions {
  unitTests.isIncludeAndroidResources=true
  unitTests.all {
   it.systemProperty("java.io.tmpdir",layout.buildDirectory.dir("test-temp").get().asFile.apply {mkdirs()}.absolutePath)
   it.systemProperty("user.home",layout.buildDirectory.dir("test-home").get().asFile.apply {mkdirs()}.absolutePath)
   it.systemProperty("maven.repo.local",layout.buildDirectory.dir("test-maven").get().asFile.absolutePath)
   it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED","--add-opens=java.base/java.util=ALL-UNNAMED")
   it.systemProperty("robolectric.dependency.repo.url","https://repo.maven.apache.org/maven2")
   it.systemProperty("pocketsurf.snapshots",layout.buildDirectory.dir("snapshots").get().asFile.absolutePath)
  }
 }
}
dependencies { implementation("androidx.browser:browser:1.8.0"); implementation("androidx.webkit:webkit:1.14.0"); testImplementation("junit:junit:4.13.2"); testImplementation("org.robolectric:robolectric:4.14.1") }
val createLocalDebugKey by tasks.registering(Exec::class) {
 val key=rootProject.file(".local/debug.keystore"); onlyIf { !key.exists() }; doFirst { key.parentFile.mkdirs() }
 commandLine("${System.getProperty("java.home")}/bin/keytool", "-genkeypair", "-noprompt", "-keystore", key.absolutePath, "-storepass", "android", "-keypass", "android", "-alias", "androiddebugkey", "-dname", "CN=PocketSurf Debug", "-keyalg", "RSA", "-validity", "10000")
}
tasks.configureEach { if(name=="validateSigningDebug") dependsOn(createLocalDebugKey) }
