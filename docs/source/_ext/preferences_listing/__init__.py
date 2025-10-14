"""Extension that creates a new Sphinx domain for documenting Phoebus' preference settings.

Sphinx domain: https://www.sphinx-doc.org/en/master/usage/domains/index.html
Creating a Sphinx domain tutorial: https://www.sphinx-doc.org/en/master/development/tutorials/adding_domain.html
"""

from pathlib import Path

from sphinx.application import Sphinx
from sphinx.util.typing import ExtensionMetadata

from ._domain import PreferencesDomain
from ._parsing import parse_all_properties_files


def setup(app: Sphinx) -> ExtensionMetadata:
    app.add_config_value(
        "phoebus_root",
        (Path(app.confdir) / "../..").resolve(),
        "html",
        Path,
    )
    app.add_config_value(
        "phoebus_repository",
        "https://github.com/ControlSystemStudio/phoebus",
        "html",
        str,
    )

    app.add_domain(PreferencesDomain)
    app.connect("config-inited", parse_all_properties_files)

    return {
        "version": "0.1",
        "parallel_read_safe": True,
        "parallel_write_safe": True,
    }
