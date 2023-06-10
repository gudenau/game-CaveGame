use crate::logging;
use crate::misc;
use crate::misc::ResultExtension;
use crate::misc::OptionExtension;

use fs::File;
use std::cmp::Ordering;
use std::{fs, io, slice};
use std::fmt::{Display, Error, format, Formatter};
use std::io::{BufRead, BufReader, Read, Write};
use std::path::{Path, PathBuf};
use serde::{Deserialize};
use sha1::Sha1;
use sha2::{Sha512, Digest};

pub static JAVA_VERSION : u32 = 20;

#[derive(Deserialize)]
pub struct JavaVersion {
    pub major: u32,
    pub minor: u32,
    pub security: u32,
    pub build: u32,
    pub openjdk_version: String,
}

impl JavaVersion {
    fn cmp(&self, other: &JavaVersion) -> Ordering {
        let result = self.major.cmp(&other.major);
        if(result != Ordering::Equal) {
            return result;
        }
        let result = self.minor.cmp(&other.minor);
        if(result != Ordering::Equal) {
            return result;
        }
        let result = self.security.cmp(&other.security);
        if(result != Ordering::Equal) {
            return result;
        }
        return self.build.cmp(&other.build);
    }
}

impl Display for JavaVersion {
    fn fmt(&self, f: &mut Formatter) -> Result<(), Error> {
        return write!(f, "{}", self.openjdk_version);
    }
}

#[derive(Deserialize)]
pub struct JavaVersionRoot {
    versions: Vec<JavaVersion>,
}

pub fn getBestJavaVersion() -> JavaVersion {
    let arch = misc::getArch();
    let os = misc::getOs();

    let url = format!(
        "https://api.adoptium.net/v3/info/release_versions?architecture={}&heap_size=normal&image_type=jre&jvm_impl=hotspot&lts=false&os={}&page=0&page_size=10&project=jdk&release_type=ga&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse",
        arch,
        os
    );
    logging::debug("Downloading Java version information");
    let mut json: JavaVersionRoot = ureq::get(&url)
        .call().abort("Failed to download Java version metadata")
        .into_json().abort("Failed to parse Java version metadata");

    json.versions.retain(|version| {
        return version.major == JAVA_VERSION;
    });

    if(json.versions.is_empty()) {
        logging::fatal("No valid Java versions found");
    }

    json.versions.sort_by(|a, b| a.cmp(b));

    return json.versions.remove(json.versions.len() - 1);
}

fn download(url: String, target: &Path) {
    logging::info(format!("Downloading {} to {}", url, target.display()));

    if(target.exists()) {
        return;
    }

    match(target.parent()) {
        Some(parent) => fs::create_dir_all(parent)
            .abort("Failed to create parent directories"),
        None => {},
    }

    let reader = ureq::get(&url).call()
        .abort(format!("Failed to create ureq for {}", url));
    let mut file = File::create(target)
        .abort(format!("Failed to create file {}", target.display()));
    std::io::copy(&mut reader.into_reader(), &mut file)
        .abort("Failed to copy file from url");
}

pub fn downloadJava(version: &String, destination: &Path) {
    let parent = destination.parent()
        .abort("Failed to determine parent directory");
    if(!parent.exists()) {
        fs::create_dir_all(parent)
            .abort("Failed to create parent directories");
    }

    download(format!(
        "https://api.adoptium.net/v3/binary/version/jdk-{}/{}/{}/jdk/hotspot/normal/eclipse?project=jdk",
        version,
        misc::getOs(),
        misc::getArch()
    ), destination);
}

fn hashFile512(path: &Path) -> io::Result<[u8; 512 / 8]> {
    let input = File::open(path)?;
    let mut bufferedInput = BufReader::new(input);
    let mut hasher = Sha512::new();
    std::io::copy(&mut bufferedInput, &mut hasher)?;
    return Ok(hasher.finalize().into());
}

fn hashFile1(path: &Path) -> io::Result<[u8; 160 / 8]> {
    let input = File::open(path)?;
    let mut bufferedInput = BufReader::new(input);
    let mut hasher = Sha1::new();
    std::io::copy(&mut bufferedInput, &mut hasher)?;
    return Ok(hasher.finalize().into());
}

pub fn validateRuntime(hashPath: &Path, runtimePath: &Path) -> bool {
    let input = File::open(hashPath)
        .abort("Failed to read hash file");
    let mut bufferedInput = BufReader::new(input);

    loop {
        let mut size : u8 = 0;
        if(bufferedInput.read_exact(&mut slice::from_mut(&mut size)).is_err()) {
            break;
        }

        let mut data = vec![0u8; size as usize];
        bufferedInput.read_exact(&mut data)
            .abort("Failed to read path");
        let pathString = String::from_utf8(data)
            .abort("Failed to convert path to string");
        let path = Path::new(&pathString);

        let mut readHash = vec![0u8; 512 / 8]; // Sha512 is 512 bits, hence the name
        bufferedInput.read_exact(&mut readHash)
            .abort("Failed to read hash");

        if(!readHash.eq(&hashFile512(&runtimePath.join(path)).abort("Failed to hash file"))) {
            return false;
        }
    }

    return true;
}

pub fn hashRuntime(hashPath: &Path, runtimePath: &Path) {
    logging::debug(format!("Hashing runtime {} to file {}", runtimePath.display(), hashPath.display()));

    let mut output = File::create(hashPath)
        .abort("Failed to create hash file");

    misc::walk(runtimePath.to_path_buf(), |path: &Path, truncatedPath: &Path, output: &mut File| {
        let input= File::open(path)
            .abort("Failed to open file for hashing");
        let mut reader = BufReader::new(input);
        let mut hasher = Sha512::new();
        io::copy(&mut reader, &mut hasher)
            .abort("Failed to hash file");

        let pathString = truncatedPath.to_path_buf().into_os_string().into_string()
            .abort("Failed to convert path into string");
        let pathBytes = pathString.as_bytes();
        let size = pathBytes.len() as u8;
        output.write_all(slice::from_ref(&size))
            .abort("Failed to write path length");
        output.write_all(pathBytes)
            .abort("Failed to write path");
        output.write_all(&hasher.finalize()[..])
            .abort("Failed to write hash");
    }, &mut output);
}

pub fn downloadLibrary(libDir: &Path, package: &str, artifact: &str, module: Option<&str>, version: &str, central: bool) -> PathBuf {
    let mut destination = libDir.to_path_buf();
    for part in package.split(".").into_iter() {
        destination = destination.join(part);
    }
    destination = destination.join(artifact)
        .join(version);

    let fileName = match(module) {
        Some(moduleName) => format!("{}-{}-{}.jar", artifact, version, moduleName),
        None => format!("{}-{}.jar", artifact, version)
    };
    let hashFileName = match(module) {
        Some(moduleName) => format!("{}-{}-{}.jar.sha1", artifact, version, moduleName),
        None => format!("{}-{}.jar.sha1", artifact, version)
    };
    let hash = destination.join(&hashFileName);
    destination = destination.join(&fileName);

    //TODO
    if(!central) {
        return destination;
    }

    let jarUrl = format!(
        "https://repo1.maven.org/maven2/{}/{}/{}/{}",
        package.replace(".", "/"),
        artifact,
        version,
        &fileName
    );
    let hashUrl = format!(
        "https://repo1.maven.org/maven2/{}/{}/{}/{}",
        package.replace(".", "/"),
        artifact,
        version,
        &hashFileName
    );

    if(hash.exists() && destination.exists()) {
        let calculatedHash = hashFile1(&destination).abort("hash").to_vec();
        let hashFile = File::open(&hash)
            .abort("Failed to open hash file");
        let mut hashString = String::new();
        BufReader::new(hashFile)
            .read_line(&mut hashString)
            .abort("Failed to read hash file");
        if(hashString.ends_with('\n')) {
            hashString.remove(hashString.len() - 1);
        }
        let readHash = hex::decode(hashString).abort("Failed to decode hash");
        if(readHash.eq(&calculatedHash)) {
            return destination;
        }
    }
    if(hash.exists()) {
        fs::remove_file(&hash)
            .abort("Failed to delete hash file");
    }
    if(destination.exists()) {
        fs::remove_file(&destination)
            .abort("Failed to delete jar file");
    }

    download(jarUrl, &destination);
    download(hashUrl, &hash);

    return destination;
}
