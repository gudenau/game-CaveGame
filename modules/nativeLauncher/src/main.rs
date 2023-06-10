#![allow(unused_parens)]
#![allow(non_snake_case)]

mod logging;
mod java;
mod misc;
mod exec;

use crate::misc::{OptionExtension, ResultExtension};

use std::path::{Path, PathBuf};
use std::fs;
use crate::java::JAVA_VERSION;

/*
TODO:
 - Don't check for updates on every run (timestamps?)
 - Clean up code
 */
fn main() {
    let arch = misc::getArch();
    let os = misc::getOs();
    let isWindows = os.eq("windows");

    logging::debug(format!(r#"OS: {}
Arch: {}"#,
        os,
        arch
    ));

    logging::info(format!("Setting up Java {}", JAVA_VERSION));

    let bestVersion = java::getBestJavaVersion();
    logging::debug(format!("Picked Java {}", bestVersion));

    let extension = (|| if(isWindows) { return "zip"; } else { return "tar.gz"; })();
    let libraryDir = fs::canonicalize(Path::new(".")).abort("Failed to resolve .")
        .join("libs");
    let destination = libraryDir
        .join("com")
        .join("java")
        .join("java")
        .join(&bestVersion.openjdk_version);

    let archivePath = destination.join(format!("java-{}.{}", &bestVersion.openjdk_version, extension));
    let runtimePath = destination.join(format!("jdk-{}", &bestVersion.openjdk_version));
    let hashPath = destination.join(format!("java-{}.hash", &bestVersion.openjdk_version));

    logging::debug(format!("Java archive path: {}", archivePath.display()));
    logging::debug(format!("Java runtime path: {}", runtimePath.display()));

    if(runtimePath.exists()) {
        if(!java::validateRuntime(&hashPath, &runtimePath)) {
            logging::error("Existing runtime failed validation");
            fs::remove_dir_all(&runtimePath)
                .abort("Failed to delete corrupted Java runtime");
            fs::remove_file(&archivePath)
                .abort("Failed to delete java archive");
        }
    }

    if(!archivePath.exists()) {
        logging::info(format!("Downloading Java {}", JAVA_VERSION));
        java::downloadJava(&bestVersion.openjdk_version, &archivePath);
    }

    if(!runtimePath.exists()) {
        logging::info(format!("Extracting Java {}", JAVA_VERSION));
        if(isWindows) {
            misc::extractZip(&destination, &archivePath);
        } else {
            misc::extractTar(&destination, &archivePath);
        }

        java::hashRuntime(&hashPath, &runtimePath);
    }

    let mut modulePath = Vec::new();
    modulePath.push(java::downloadLibrary(&libraryDir, "net.gudenau.cavegame.launcher", "launcher", None, "0.0.0", true));
    modulePath.push(java::downloadLibrary(&libraryDir, "net.gudenau.cavegame", "cavegame", Some("logger"), "0.0.0", true));
    modulePath.push(java::downloadLibrary(&libraryDir, "org.jetbrains", "annotations", None, "24.0.1", true));

    let mut stringModulePath = String::new();
    for entry in modulePath {
        stringModulePath += entry.to_str().abort("Failed to convert path to string");
        #[cfg(unix)]
        {
            stringModulePath += ":";
        }
        #[cfg(windows)]
        {
            stringModulePath += ";";
        }
    }

    let java = runtimePath.join("bin")
        .join((|| if(isWindows) { return "java.exe"; } else { return "java"; })());

    logging::debug(format!("Executing {}", java.display()));
    let mut arguments = Vec::new();
    arguments.push("-p");
    arguments.push(&stringModulePath);
    arguments.push("-m");
    arguments.push("net.gudenau.cavegame.launcher/net.gudenau.cavegame.launcher.Launcher");
    exec::exec(java, &arguments);
    logging::fatal("Failed to execute Java");
}
