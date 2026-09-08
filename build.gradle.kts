plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.ktcloud"
version = "0.0.1-SNAPSHOT"
description = "Travel Planner community service backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	maven {
		name = "GitHubPackages"
		url = uri("https://maven.pkg.github.com/protove/travel-common")
		credentials {
			username = providers.gradleProperty("gpr.user")
				.orElse(providers.environmentVariable("GITHUB_PACKAGES_USER"))
				.orNull
			password = providers.gradleProperty("gpr.key")
				.orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
				.orNull
		}
		content {
			includeGroup("com.ktcloud.travelplanner")
		}
	}
	mavenCentral()
}

dependencies {
	implementation("com.ktcloud.travelplanner:travel-common:0.1.0")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-oauth2-jose")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.flywaydb:flyway-core")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<Test>("test") {
	exclude("**/*IntegrationTest.class", "**/community/adapter/**")
}

// HTTP 어댑터 테스트. Identity/Travel 응답 파싱·헤더 전달·503 승격을 고정한다.
// CI가 "3단계 HTTP Adapter 테스트"로 따로 보여줄 수 있게 test 태스크에서 떼어냈다
// (안 떼면 test에 이미 포함돼 같은 테스트를 두 번 돌린다).
val adapterTest by tasks.registering(Test::class) {
	description = "Runs HTTP adapter tests."
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	include("**/community/adapter/**")
	shouldRunAfter(tasks.named("test"))
}

val integrationTest by tasks.registering(Test::class) {
	description = "Runs backend integration tests."
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	include("**/*IntegrationTest.class")
	shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
	dependsOn(integrationTest, adapterTest)
}

tasks.bootJar {
	archiveFileName.set("app.jar")
}

tasks.jar {
	enabled = false
}
