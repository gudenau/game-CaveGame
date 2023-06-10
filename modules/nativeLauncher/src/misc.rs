use crate::logging;

use fs::File;
use std::collections::VecDeque;
use std::fs;
use std::path::{Path, PathBuf};
use flate2::read::GzDecoder;
use tar::Archive;
use zip::ZipArchive;

pub trait ResultExtension<T, E> {
    fn abort(self, message: impl Into<String>) -> T;
}

impl<T, E> ResultExtension<T, E> for Result<T, E> {
    fn abort(self, message: impl Into<String>) -> T {
        return match(self) {
            Ok(value) => value,
            Err(_) => logging::fatal(message),
        };
    }
}

pub trait OptionExtension<T> {
    fn abort(self, message: impl Into<String>) -> T;
}

impl<T> OptionExtension<T> for Option<T> {
    fn abort(self, message: impl Into<String>) -> T {
        return match(self) {
            Some(value) => value,
            None => logging::fatal(message),
        };
    }
}

pub fn getOs() -> &'static str {
    let mut os = std::env::consts::OS;
    if(os.eq("macos")) {
        os = "mac";
    }
    return os;
}

pub fn getArch() -> &'static str {
    let mut arch = std::env::consts::ARCH;
    if(arch.eq("x86_64")) {
        arch = "x64";
    }
    return arch;
}

pub fn extractTar(destination : &Path, fileName : &Path) {
    let tarGz = File::open(fileName)
        .abort("Failed to open Java tar");
    let tar = GzDecoder::new(tarGz);
    let mut archive = Archive::new(tar);
    archive.unpack(destination)
        .abort("Failed to extract Java tar");
}

pub fn extractZip(destination : &Path, fileName : &Path) {
    let file = File::open(fileName)
        .abort("Failed to open Java ZIP");
    let mut archive = ZipArchive::new(file)
        .abort("Failed to parse Java ZIP file");

    for i in 0..archive.len() {
        let mut file = archive.by_index(i)
            .abort(format!("Failed to get entry {}", i));
        let path = match file.enclosed_name() {
            Some(path) => path.to_owned(),
            None => continue,
        };
        let path = destination.join(path);

        if(file.is_dir()) {
            continue;
        }

        if let Some(p) = path.parent() {
            if(!p.exists()) {
                fs::create_dir_all(p)
                    .abort(format!("Failed to create directory {}", p.display()));
            }
        }

        let mut out = File::create(&path)
            .abort(format!("Failed to create file {}", path.display()));
        std::io::copy(&mut file, &mut out)
            .abort(format!("Failed to write file {}", path.display()));
    }
}

pub fn walk<T>(path: PathBuf, action: fn(&Path, &Path, &mut T), user: &mut T) {
    let mut paths = VecDeque::new();
    paths.push_back(path.clone());

    while(!paths.is_empty()) {
        let current = paths.pop_front().abort("paths was empty?");
        let directory = fs::read_dir(current)
            .abort("Failed to list contents of directory");

        directory.for_each(|result| {
            let child = result.abort("Failed to get child from a directory?");
            let childPath = child.path();
            let fileType = child.file_type()
                .abort(format!("Failed to get file type for {}", &path.display()));

            if (fileType.is_file()) {
                action(
                    &childPath,
                    childPath.strip_prefix(&path)
                        .abort("Failed to strip prefix"),
                    user
                );
            } else if(fileType.is_dir()) {
                paths.push_back(childPath.clone());
            }
        });
    }
}
