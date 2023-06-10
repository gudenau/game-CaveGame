use std::ffi::{c_char, c_int, CString};
use std::path::Path;
use crate::logging;

use crate::misc::{OptionExtension, ResultExtension};

extern "C" {
    #[cfg(unix)]
    fn execvp(path: *const c_char, argv: *mut *mut c_char) -> c_int;
    #[cfg(windows)]
    fn _execvp(path: *const c_char, argv: *mut *mut c_char) -> c_int;
}

pub fn exec(path: impl AsRef<Path>, args: &[&str]) {
    let nativePath = CString::new(path.as_ref().as_os_str().to_str().abort("Failed to convert path to UTF-8"))
        .abort("Failed to convert path to c string");

    let mut nativeArgs = Vec::new();
    nativeArgs.push(nativePath.clone().into_raw());
    for arg in args {
        let nativeArg = CString::new(arg.to_string()).unwrap();
        let pointerArg = nativeArg.into_raw();
        nativeArgs.push(pointerArg);
    }
    nativeArgs.push(std::ptr::null_mut());

    unsafe {
        #[cfg(unix)]
        execvp(nativePath.as_ptr(), nativeArgs.as_mut_ptr());
        #[cfg(windows)]
        _execvp(nativePath.as_ptr(), nativeArgs.as_mut_ptr());
    }
}
