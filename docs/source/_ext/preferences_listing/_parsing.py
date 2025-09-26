"""Fetching properties files, parsing, and storing them."""

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from sphinx.application import Sphinx
from sphinx.config import Config


@dataclass
class Preference:
    name: str
    package: str
    fullname: str
    default_value: str
    documentation: str


@dataclass
class Package:
    name: str
    # May be freestanding text, or a preference with its documentation
    content: list[str | Preference]
    file: Path


# Will store parsed properties, grouped by packages
# package name -> Package
_DATA: dict[str, Package] = {}


def _parse_preference(line: str) -> tuple[str, str]:
    """Parse a preference line to a (name, default_value) tuple."""
    [name, default_value] = line.split("=", maxsplit=1)
    return (name.strip(), default_value.strip())


def _parse_preferences_file(file: Path, pack: str, root: Path) -> Package:
    """Parse a preference file.

    Consecutive comments before a preference is added as documentation
    for that preference.

    Consecutive comments not before a preference is added
    as freestanding text of the package.
    """
    result = []
    consecutive_comments = []

    with file.open() as f:
        for line in f:
            if not line.startswith("#"):
                # Not a comment

                if "=" in line:
                    # A properties line
                    [name, default_value] = _parse_preference(line)
                    result.append(
                        Preference(
                            name,
                            pack,
                            f"{pack}/{name}",
                            default_value,
                            "\n".join(consecutive_comments),
                        )
                    )
                else:
                    # An empty line,
                    # the consecutive comments were freestanding text
                    result.append("\n".join(consecutive_comments))

                # Consecutive comments were processed, flush them
                consecutive_comments = []
                continue

            if line.startswith(("# ----------", "# Package ")):
                # Ignore the header specifying the package name
                continue

            # Strip "# " or "#" from the beginning of the line
            comment_start = 1
            if line.startswith("# "):
                comment_start = 2

            consecutive_comments.append(line[comment_start:].rstrip())

        # If there were comments at the end of the file,
        # we're adding them as freestanding text
        result.append("\n".join(consecutive_comments))

    return Package(pack, result, file.relative_to(root))


def package_name(file: Path) -> str | None:
    """Get the package name of the given `.properties` file.

    Looks for a line like this:

        # Package <package name>
    """
    with file.open() as f:
        for line in f:
            if line.startswith("# Package "):
                return line.removeprefix("# Package ").strip()

    return None


def parse_all_properties_files(_app: Sphinx, config: Config):
    """Find all properties files, parse them, and populate DATA."""
    root = config.phoebus_root

    files: list[tuple[Path, str]] = []

    for dirpath, _dirnames, filenames in root.walk():
        # Skip build directories
        if "target" in dirpath.parts:
            continue

        for filename in filenames:
            if filename.endswith("preferences.properties"):
                file = dirpath / filename
                pack = package_name(file) or filename
                _DATA[pack] = _parse_preferences_file(file, pack, root)


def get_package(name: str) -> Package:
    """Get the parsed package information for the given package name."""
    return _DATA[name]


def package_names() -> Iterable[str]:
    """Get all parsed package names."""
    return sorted(_DATA.keys())
