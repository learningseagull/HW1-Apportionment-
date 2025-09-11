plugins {
    application
    java
}
repositories { mavenCentral() }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test { useJUnitPlatform() }

application {
    // Change to your real main class (with package if any)
    mainClass.set("Main")
}
