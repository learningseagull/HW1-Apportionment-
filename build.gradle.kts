//plugins {
//    application
//    java
//}
//repositories { mavenCentral() }
//
//dependencies {
//    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
//    implementation("org.apache.poi:poi-ooxml:5.2.5")
//}
//
//tasks.test { useJUnitPlatform() }
//
//application {
//    // Change to your real main class (with package if any)
//    mainClass.set("edu.virginia.sde.hw1.Main")
//}
plugins {
    id("java")
}
tasks.jar {
    archiveBaseName.set("Apportionment")
    manifest {
        attributes["Main-Class"] = "edu.virginia.sde.hw1.Main"
    }
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "edu.virginia.sde.hw1.Main"
    }
}

