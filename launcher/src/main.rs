use std::env;
use std::process::Command;

fn main() {
    let args: Vec<String> = env::args().collect();

    // If run with no arguments (user double-click), register the protocol
    if args.len() < 2 {
        match register_protocol() {
            Ok(_) => {
                println!("========================================");
                println!("   PHOEBUS URI HANDLER REGISTERED       ");
                println!("========================================");
                println!("You can now launch .bob files from the");
                println!("Flutter demo app using phoebus:// paths.");
                println!("\nPress Enter to exit...");
                let _ = std::io::stdin().read_line(&mut String::new());
            }
            Err(e) => eprintln!("Failed to register URI: {}", e),
        }
        return;
    }

    let raw_uri = &args[1];
    launch_phoebus(raw_uri);
}

#[cfg(target_os = "windows")]
fn register_protocol() -> Result<(), Box<dyn std::error::Error>> {
    use winreg::RegKey;
    use winreg::enums::*;

    let hkcu = RegKey::predef(HKEY_CURRENT_USER);
    let (key, _) = hkcu.create_subkey("Software\\Classes\\phoebus")?;
    key.set_value("", &"URL:Phoebus Protocol")?;
    key.set_value("URL Protocol", &"")?;

    let (cmd_key, _) = key.create_subkey("shell\\open\\command")?;
    let current_exe = env::current_exe()?;
    cmd_key.set_value("", &format!("\"{}\" \"%1\"", current_exe.to_str().unwrap()))?;
    Ok(())
}

#[cfg(target_os = "linux")]
fn register_protocol() -> Result<(), Box<dyn std::error::Error>> {
    let home = env::var("HOME")?;
    let desktop_dir = format!("{}/.local/share/applications", home);
    let current_exe = env::current_exe()?;

    let content = format!(
        "[Desktop Entry]\nType=Application\nName=Phoebus Launcher\nExec={} %u\nMimeType=x-scheme-handler/phoebus;\nNoDisplay=true",
        current_exe.to_str().unwrap()
    );

    std::fs::create_dir_all(&desktop_dir)?;
    std::fs::write(format!("{}/phoebus.desktop", desktop_dir), content)?;
    Command::new("update-desktop-database")
        .arg(&desktop_dir)
        .status()?;
    Ok(())
}

#[cfg(target_os = "macos")]
fn register_protocol() -> Result<(), Box<dyn std::error::Error>> {
    // macOS registration is handled by the Info.plist in the App Bundle.
    // We just return Ok so the launcher can still be run to verify paths.
    println!("macOS detected: Registration is handled via the App Bundle structure.");
    Ok(())
}

fn launch_phoebus(uri: &str) {
    let clean_path = uri
        .trim_start_matches("phoebus://")
        .trim_start_matches("phoebus:");
    let exe_dir = env::current_exe().unwrap().parent().unwrap().to_path_buf();

    #[cfg(target_os = "windows")]
    {
        let script_path = exe_dir.join("phoebus.bat");
        Command::new("cmd")
            .arg("/c")
            .arg(script_path)
            .arg("-server")
            .arg("-resource")
            .arg(clean_path)
            .spawn()
            .expect("Failed to launch Phoebus batch script");
    }

    #[cfg(not(target_os = "windows"))]
    {
        let script_path = exe_dir.join("phoebus.sh");
        // We call the script directly instead of 'sh -c' to ensure args forward correctly
        Command::new(script_path)
            .arg("-server")
            .arg("-resource")
            .arg(clean_path)
            .spawn()
            .expect("Failed to launch Phoebus shell script");
    }
}
