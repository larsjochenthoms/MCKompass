#!/bin/bash
# macOS 12+; no Homebrew or Python required. Run only with Minecraft closed.
set -euo pipefail
manifest_url='https://github.com/larsjochenthoms/MCKompass/releases/latest/download/simple-mod-sync.json'
prepare_only=false
game_dir=''
while [ "$#" -gt 0 ]; do
    case "$1" in
        --manifest-url) [ "$#" -gt 1 ] || exit 2; manifest_url="$2"; shift 2 ;;
        --prepare-only) prepare_only=true; shift ;;
        -*) echo "Unbekannte Option: $1" >&2; exit 2 ;;
        *) [ -z "$game_dir" ] || { echo 'Nur einen Spielordner angeben.' >&2; exit 2; }; game_dir="$1"; shift ;;
    esac
done

die() { echo "Fehler: $*" >&2; exit 1; }
assert_game_closed() {
    # Inspect full command lines, allowing launchers to remain open.
    local processes
    processes=$(/bin/ps -axo comm=,args=) || die 'Laufende Programme konnten nicht geprüft werden.'
    if /usr/bin/grep -E '(^|/)(java|javaw|minecraft)( |$)' <<< "$processes" |
        /usr/bin/grep -Eiq 'net\.minecraft|net\.fabricmc|KnotClient|KnotServer|org\.(prismlauncher|multimc)\.EntryPoint|--gameDir|(fabric|minecraft|server).*\.jar'; then
        die 'Minecraft läuft noch. Alle Minecraft-Spiele und Server schließen und erneut starten.'
    fi
}
download() {
    case "$1" in https://*) ;; *) die 'Nur HTTPS-Downloadadressen sind erlaubt.' ;; esac
    /usr/bin/curl --fail --location --silent --show-error --connect-timeout 20 --max-time 180 \
        --proto '=https' --proto-redir '=https' --max-filesize 67108864 \
        --user-agent 'MCKompass-Installer/1.0' --output "$2" "$1"
}
json_get() { /usr/bin/plutil -extract "$2" raw -expect "$3" -o - "$1"; }
mod_id() {
    /usr/bin/unzip -p "$1" fabric.mod.json > "$stage/mod-metadata.json" 2>/dev/null || return 1
    json_get "$stage/mod-metadata.json" id string 2>/dev/null
}
set_string() { /usr/bin/plutil -replace "$1" -string "$2" "$new_config"; }
set_bool() { /usr/bin/plutil -replace "$1" -bool "$2" "$new_config"; }

if [ -z "$game_dir" ]; then
    echo 'Minecraft vollständig schließen. Der Launcher darf offen bleiben.'
    echo 'Im Launcher beim vorhandenen Fabric-26.3-Profil das Spielverzeichnis nachsehen.'
    echo 'Standard auf dem Mac: ~/Library/Application Support/minecraft'
    printf 'Spielordner eingeben (ohne umschließende Anführungszeichen): '
    IFS= read -r game_dir
fi
case "$game_dir" in '~/'*) game_dir="$HOME/${game_dir#\~/}" ;; esac
[ -d "$game_dir" ] || die 'Der Spielordner existiert nicht.'
game_dir=$(cd "$game_dir" && pwd -P)
[ "$(basename "$game_dir")" != mods ] || die 'Den Spielordner über mods wählen.'
mods="$game_dir/mods"
config_dir="$game_dir/config"
config_path="$config_dir/simplemodsync.json"
for directory in "$mods" "$config_dir" "$game_dir/.kompass-backups"; do
    [ ! -L "$directory" ] || die "Verknüpfte Ordner werden nicht unterstützt: $directory"
    [ ! -e "$directory" ] || [ -d "$directory" ] || die "Kein Ordner: $directory"
done
[ ! -L "$config_path" ] || die 'Eine verknüpfte Simple-Mod-Sync-Konfiguration wird nicht unterstützt.'
assert_game_closed
stage=$(/usr/bin/mktemp -d "$game_dir/.kompass-install-XXXXXXXX")
installed=()
originals=()
saved=()
success=false
cleanup() {
    local status=$?
    trap - EXIT
    if [ "$success" != true ]; then
        local path i
        for path in "${installed[@]+"${installed[@]}"}"; do /bin/rm -f "$path"; done
        i=0
        while [ "$i" -lt "${#originals[@]}" ]; do
            if [ -e "${saved[$i]}" ]; then /bin/mv "${saved[$i]}" "${originals[$i]}"; fi
            i=$((i+1))
        done
    fi
    /bin/rm -rf "$stage"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT TERM
echo 'Aktuelle Kompass-Version und Abhängigkeiten werden vorbereitet …'
manifest="$stage/manifest.json"
download "$manifest_url" "$manifest"
[ "$(json_get "$manifest" sync_version integer)" = 3 ] || die 'Falsches Manifestformat.'
[ "$(json_get "$manifest" minecraft_version string)" = 26.3 ] || die 'Dieses Installationsprogramm ist für Minecraft 26.3.'
[ "$(json_get "$manifest" sync array)" = 2 ] || die 'Das Manifest muss genau zwei Mods enthalten.'
prepared=()
names=()
seen_kompass=false
seen_api=false
i=0
while [ "$i" -lt 3 ]; do
    if [ "$i" -lt 2 ]; then
        name=$(json_get "$manifest" "sync.$i.name" string)
        version=$(json_get "$manifest" "sync.$i.version" string)
        type=$(json_get "$manifest" "sync.$i.type" string)
        url=$(json_get "$manifest" "sync.$i.url" string)
        hash=$(json_get "$manifest" "sync.$i.sha512" string)
        case "$name" in
            kompass4mc) [ "$seen_kompass" = false ] || die 'Doppelter Kompass-Eintrag.'; seen_kompass=true ;;
            fabric-api) [ "$seen_api" = false ] || die 'Doppelter Fabric-API-Eintrag.'; seen_api=true ;;
            *) die "Unerwarteter Mod-Eintrag: $name" ;;
        esac
    else
        name=simplemodsync
        version=1.4.1
        type=mod
        url='https://cdn.modrinth.com/data/8IFHiuMI/versions/AVuvo48S/simplemodsync-fabric-26.3-1.4.1.jar'
        hash='87d3b8abba706aab9bf942df683b631ae0060c3f034a0c7d7d59231e1717384032745d32f042d0f02436d2223c3176e21bed279d4171ccaf816a263537e273d4'
    fi
    [ "$type" = mod ] || die 'Nur Mod-Einträge werden unterstützt.'
    [[ "$version" =~ ^[0-9A-Za-z][0-9A-Za-z.+_-]{0,79}$ ]] || die 'Ungültige Versionsnummer.'
    [[ "$hash" =~ ^[0-9a-fA-F]{128}$ ]] || die 'SHA-512-Prüfsumme fehlt oder ist ungültig.'
    filename="$name-$version.jar"
    file="$stage/$filename"
    echo "Prüfe $filename …"
    download "$url" "$file"
    actual=$(/usr/bin/shasum -a 512 "$file")
    actual=${actual%% *}
    [ "$(tr 'A-F' 'a-f' <<< "$hash")" = "$actual" ] || die "Prüfsumme stimmt nicht: $filename. Installierte Dateien wurden nicht geändert."
    wanted_id="$name"
    [ "$name" != simplemodsync ] || wanted_id=simple-mod-sync
    [ "$(mod_id "$file")" = "$wanted_id" ] || die "Unerwartete Fabric-Mod-ID: $filename"
    prepared+=("$file")
    names+=("$filename")
    i=$((i+1))
done

new_config="$stage/simplemodsync.json"
if [ -e "$config_path" ]; then
    /usr/bin/plutil -convert xml1 -o "$new_config" "$config_path"
else
    /usr/bin/plutil -create xml1 "$new_config"
fi
for section in seen sync; do
    if /usr/bin/plutil -type "$section" "$new_config" >/dev/null 2>&1; then
        [ "$(/usr/bin/plutil -type "$section" "$new_config")" = dictionary ] || die "Ungültiger Konfigurationsabschnitt: $section"
    else
        /usr/bin/plutil -insert "$section" -json '{}' "$new_config"
    fi
done
previous_url=$(json_get "$new_config" sync.schema_file_url string 2>/dev/null || true)
[ -n "$previous_url" ] || previous_url=$(json_get "$new_config" download_url string 2>/dev/null || true)
if [ -n "$previous_url" ] && [ "$previous_url" != '-' ] && [ "$previous_url" != "$manifest_url" ]; then
    die 'Simple Mod Sync verwaltet bereits ein anderes Modpaket. Ein separates Fabric-Spielverzeichnis für Kompass verwenden.'
fi
/usr/bin/plutil -remove download_url "$new_config" 2>/dev/null || true
/usr/bin/plutil -remove auto_download "$new_config" 2>/dev/null || true
set_string sync.schema_file_url "$manifest_url"
set_bool sync.sync_on_startup true
set_bool seen.init_screen true
set_bool seen.allow_sync_progress_popup true
set_bool seen.allow_needs_restart_popup true
/usr/bin/plutil -convert json -r "$new_config"
old_jars=()
shopt -s nullglob
for file in "$mods"/*.jar; do
    [ ! -L "$file" ] || die "Verknüpfte Mod-Dateien werden nicht unterstützt: $file"
    if id=$(mod_id "$file"); then
        case "$id" in kompass4mc|fabric-api|simple-mod-sync) old_jars+=("$file") ;; esac
    else
        case "$(basename "$file")" in
            kompass4mc-*.jar|fabric-api-*.jar|simplemodsync-*.jar) die "Mod-Datei kann nicht geprüft werden: $file. Bitte zunächst aus mods herausnehmen." ;;
        esac
    fi
done
assert_game_closed
if [ "$prepare_only" = true ]; then
    success=true
    echo 'Vorbereitung und Prüfungen erfolgreich. Installierte Dateien wurden nicht geändert.'
    exit 0
fi
backup="$game_dir/.kompass-backups/$(date '+%Y%m%d-%H%M%S')-$(basename "$stage")"
/bin/mkdir -p "$backup" "$mods" "$config_dir"
if [ -e "$config_path" ]; then old_jars+=("$config_path"); fi
for file in "${old_jars[@]+"${old_jars[@]}"}"; do
    destination="$backup/$(basename "$file")"
    /bin/mv "$file" "$destination"
    originals+=("$file")
    saved+=("$destination")
done
i=0
while [ "$i" -lt "${#prepared[@]}" ]; do
    destination="$mods/${names[$i]}"
    /bin/mv "${prepared[$i]}" "$destination"
    installed+=("$destination")
    i=$((i+1))
done
/bin/mv "$new_config" "$config_path"
installed+=("$config_path")
success=true
echo "Fertig. Sicherung der bisherigen Dateien: $backup"
echo 'Nun das vorhandene Fabric-26.3-Profil starten.'
echo 'Simple Mod Sync prüft beim Start auf neue Versionen. Nach einer Aktualisierung Minecraft neu starten.'
echo 'Bei einem fehlgeschlagenen Update Minecraft schließen und dieses Programm erneut ausführen.'
