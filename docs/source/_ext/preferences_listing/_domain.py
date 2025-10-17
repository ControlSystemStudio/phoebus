"""A Sphinx domain for documenting Phoebus' preference settings.

Contains the "package" and "preference" object types,
and the "listing" "autopackage" directives.

Also contains the "pref", "pack", and "ref" roles
for cross-referencing preferences and packages from other documents.
"""

from collections import defaultdict
from collections.abc import Generator
from dataclasses import dataclass
from enum import StrEnum
from pathlib import Path
from typing import Iterable, cast

from docutils import nodes
from docutils.parsers.rst import directives
from docutils.statemachine import StringList, string2lines
from sphinx import addnodes
from sphinx.builders import Builder
from sphinx.config import Config
from sphinx.directives import ObjectDescription
from sphinx.domains import Domain, Index, IndexEntry, ObjType
from sphinx.environment import BuildEnvironment
from sphinx.roles import XRefRole
from sphinx.util import logging
from sphinx.util.docutils import SphinxDirective
from sphinx.util.nodes import make_refnode
from sphinx.util.parsing import nested_parse_to_nodes

from ._parsing import Package, Preference, get_package, package_names

logger = logging.getLogger(__name__)


class ListingDirective(SphinxDirective):
    """An RST directive that documents all preferences from all packages."""

    def run(self) -> list[nodes.Node]:
        result = []

        for name in package_names():
            result += AutoPackageDirective(
                "prefs:autopackage",
                arguments=[name],
                options={},
                content=StringList(),
                lineno=self.lineno,
                content_offset=self.content_offset,
                block_text=self.block_text,
                state=self.state,
                state_machine=self.state_machine,
            ).run()

        return result


class AutoPackageDirective(SphinxDirective):
    """An RST directive that lists all preferences of a given package."""

    required_arguments = 1

    def _preference_to_nodes(self, pref: Preference, file: Path) -> list[nodes.Node]:
        """Convert a Preference to a call to the "preference" object type directive."""
        content = StringList(
            string2lines(
                pref.documentation,
                self.state.document.settings.tab_width,
                convert_whitespace=True,
            ),
            source=str(file),
        )

        return PreferenceDirective(
            "prefs:preference",
            arguments=[pref.fullname],
            options={
                "default-value": pref.default_value,
            },
            content=content,
            lineno=self.lineno,
            content_offset=self.content_offset,
            block_text=self.block_text,
            state=self.state,
            state_machine=self.state_machine,
        ).run()

    def _package_to_nodes(self, pack: Package) -> list[nodes.Node]:
        """Convert a package to docutils nodes.

        This calls to the "package" object type directive,
        and for every preference in the package,
        calls the "preference" object type directive in the "package" content.

        If the package contains freestanding text, it adds the text as-is.
        """
        result = PackageDirective(
            "prefs:package",
            arguments=[pack.name],
            options={"file": str(pack.file)},
            content=StringList(),
            lineno=self.lineno,
            content_offset=self.content_offset,
            block_text=self.block_text,
            state=self.state,
            state_machine=self.state_machine,
        ).run()

        for content in pack.content:
            if isinstance(content, str):
                # freestanding text,
                # add it inside the package
                result[-1][-1] += nested_parse_to_nodes(
                    state=self.state,
                    text=content,
                    source=str(pack.file),
                    offset=self.content_offset,
                )
            elif isinstance(content, Preference):
                # add the preference inside the package
                result[-1][-1] += self._preference_to_nodes(content, pack.file)

        return result

    def run(self) -> list[nodes.Node]:
        try:
            package = get_package(self.arguments[0])
        except KeyError:
            logger.warning(
                "no such package: %s",
                self.arguments[0],
                location=self.get_location(),
            )
            return []

        return self._package_to_nodes(package)


def _package_target(name: str) -> str:
    """The HTML anchor for referencing a package."""
    return f"package-{name}"


def _preference_target(fullname: str) -> str:
    """The HTML anchor for referencing a preference."""
    return f"preference-{fullname}"


def _get_phoebus_revision(settings: Config) -> str:
    """Find the GitHub revision of the Phoebus' source code.

    Depending on the "release" Sphinx setting.
    """
    if "SNAPSHOT" in settings.release or settings.release == "1.0":
        return "master"
    return f"v{settings.release}"


class PackageDirective(ObjectDescription):
    """A Sphinx directive describing a package that has preference settings.

    Preferences documentation must be in the directive's content.
    """

    has_content = True
    required_arguments = 1
    option_spec = {
        # See: https://www.sphinx-doc.org/en/master/usage/domains/index.html#basic-markup
        "no-index": directives.flag,
        "no-index-entry": directives.flag,
        # The file containing the package's preferences
        "file": directives.unchanged,
    }

    def handle_signature(self, sig: str, signode: addnodes.desc_signature) -> str:
        """Parse the package name, and generate the signature node.

        The signature node is basically an XML representation of the signature
        (the package name), which part is punctuation, which part is the main name, etc.
        """
        no_index = "no-index" in self.options or "no-index-entry" in self.options

        signode["name"] = sig

        if not no_index:
            signode += addnodes.index(
                entries=[
                    # (entrytype, entryname, target, ignored, key)
                    (
                        "single",
                        f"{sig} (package)",
                        _package_target(sig),
                        "",
                        None,
                    ),
                ],
            )

        pack_parts = sig.split(".")
        for pack_part in pack_parts[:-1]:
            if pack_part in ["org", "csstudio", "phoebus"]:
                # Make "org.csstudio" and "org.phoebus" less visible than the rest
                signode += addnodes.desc_addname(text=pack_part)
            else:
                signode += addnodes.desc_name(text=pack_part)
            signode += addnodes.desc_sig_punctuation(text=".")
        signode += addnodes.desc_name(text=pack_parts[-1])

        file = self.options.get("file")

        # Add a "[source]" link next to the package's name
        if file and self.config.phoebus_repository is not None:
            ref = _get_phoebus_revision(self.config)
            uri = f"{self.config.phoebus_repository}/blob/{ref}/{file}"

            # Mostly taken from the 'linkcode' builtin extension
            onlynode = addnodes.only(expr="html")
            onlynode += nodes.reference(
                "",
                "",
                nodes.inline("", "[source]", classes=["viewcode-link"]),
                internal=False,
                refuri=uri,
            )
            signode += onlynode

        return sig

    def add_target_and_index(
        self,
        name: str,
        sig: str,
        signode: addnodes.desc_signature,
    ) -> None:
        """A the package to the domain data, for later cross-referencing."""
        signode["ids"].append(f"package-{name}")
        domain = cast("PreferencesDomain", self.env.get_domain("prefs"))
        domain.add_package(name)

    def _toc_entry_name(self, signode: addnodes.desc_signature) -> str:
        """How the package will be named in the table of contents."""
        return signode["name"]

    def before_content(self) -> None:
        """Insert content before a package.

        In this instance, we insert ourself in the context
        so that our children can see us as parent.

        This is used so that "prefs:pref" links resolves
        relative to the package it's declared in.
        """
        packages = self.env.ref_context.setdefault("prefs:packages", [])
        packages.append(self.names[-1])

    def after_content(self) -> None:
        """Insert content after a package.

        In this instance, we remove ourself in the context
        to prevent other objects to see us as parent.
        """
        packages = self.env.ref_context.setdefault("prefs:packages", [])
        if packages:
            packages.pop()
        else:
            self.env.ref_context.pop("prefs:packages")


class PreferenceDirective(ObjectDescription):
    """A Sphinx directive describing a preference setting / property."""

    has_content = True
    required_arguments = 1
    option_spec = {
        "no-index": directives.flag,
        "no-index-entry": directives.flag,
        "default-value": directives.unchanged,
    }

    def handle_signature(self, sig: str, signode: addnodes.desc_signature) -> str:
        """Parse the preference name, and generate the signature node.

        The signature node is basically an XML representation of the signature
        (the preference name), which part is punctuation, which part is the main name, etc.
        """
        no_index = "no-index" in self.options or "no-index-entry" in self.options

        [pack, name] = sig.split("/", 1)

        signode["fullname"] = fullname = sig
        signode["package"] = pack
        signode["name"] = name

        if not no_index:
            signode += addnodes.index(
                entries=[
                    # (entrytype, entryname, target, ignored, key)
                    (
                        "pair",
                        f"{pack} (package); {name} (preference)",
                        _preference_target(fullname),
                        "",
                        None,
                    ),
                ],
            )

        pack_parts = pack.split(".")
        for pack_part in pack_parts[:-1]:
            signode += addnodes.desc_addname(text=pack_part)
            signode += addnodes.desc_sig_punctuation(text=".")
        signode += addnodes.desc_addname(text=pack_parts[-1])

        signode += addnodes.desc_sig_punctuation(text="/")

        name_parts = name.split(".")
        for name_part in name_parts[:-1]:
            signode += addnodes.desc_name(text=name_part)
            signode += addnodes.desc_sig_punctuation(text=".")
        signode += addnodes.desc_name(text=name_parts[-1])

        # If present, add the default value as an "annotation",
        # like Sphinx does for Python variable documentation.
        default_value = self.options.get("default-value")
        if default_value is not None:
            signode += addnodes.desc_annotation(
                default_value,
                "",
                addnodes.desc_sig_space(),
                addnodes.desc_sig_operator(text="="),
                addnodes.desc_sig_space(),
                nodes.Text(default_value),
            )

        return sig

    def add_target_and_index(
        self,
        name: str,
        sig: str,
        signode: addnodes.desc_signature,
    ) -> None:
        """A the preference to the domain data, for later cross-referencing."""
        signode["ids"].append(_preference_target(name))
        domain = cast("PreferencesDomain", self.env.get_domain("prefs"))
        domain.add_preference(name)

    def _toc_entry_name(self, signode: addnodes.desc_signature) -> str:
        """How the preference will be named in the table of contents."""
        return signode["name"]

    def before_content(self) -> None:
        """Insert content before a preference.

        In this instance, we insert our package in the context
        so that our children can see us as parent.

        This is used so that "prefs:pref" links resolves
        relative to the preference it's declared in.
        """
        packages = self.env.ref_context.setdefault("prefs:packages", [])
        packages.append(self.names[-1].split("/")[0])

    def after_content(self) -> None:
        """Insert content after a preference.

        In this instance, we remove our package in the context
        to prevent other objects to see us as parent.
        """
        packages = self.env.ref_context.setdefault("prefs:packages", [])
        if packages:
            packages.pop()
        else:
            self.env.ref_context.pop("prefs:packages")


class EntityType(StrEnum):
    """A type of object for the "prefs" Sphinx domain."""

    PACKAGE = "package"
    PREFERENCE = "preference"

    def directive_name(self) -> str:
        return str(self)

    def human_name(self) -> str:
        match self:
            case EntityType.PACKAGE:
                return "Preference package"
            case EntityType.PREFERENCE:
                return "Preference setting"


@dataclass
class RefEntity:
    """A referenceable entity."""

    name: str
    fullname: str
    typ: EntityType
    docname: str
    anchor: str
    priority: int

    def to_tuple(self) -> tuple[str, str, str, str, str, int]:
        """Get this entity as tuple.

        As needed by Sphinx' `get_object()` domain method.

        See: https://www.sphinx-doc.org/en/master/extdev/domainapi.html#sphinx.domains.Domain.get_objects
        """
        # name, dispname, type, docname, anchor, priority
        return (
            self.name,
            self.fullname,
            self.typ.directive_name(),
            self.docname,
            self.anchor,
            self.priority,
        )

    def __lt__(self, other: "RefEntity") -> bool:
        """Sort entities by their full names."""
        return self.fullname < other.fullname


class PreferencesIndex(Index):
    """An index of all preference settings / properties."""

    name = "index"
    localname = "Preference setting index"
    shortname = "preference"

    def _find_package(self, name: str) -> RefEntity:
        """Find the package entity that matches the given name."""
        domain = cast("PreferencesDomain", self.domain)
        return next(x for x in domain.get_packages() if x.name == name)

    def _package_entry(self, name: str) -> IndexEntry:
        """Emit an index entry for the given package."""
        pack = self._find_package(name)
        # subtype 1, because it will contain preference entries
        #
        # name, subtype, docname, anchor, extra, qualifier, description
        return IndexEntry(
            name,
            1,
            pack.docname,
            pack.anchor,
            "",
            "",
            pack.typ,
        )

    def generate(
        self,
        docnames: Iterable[str] | None = None,
    ) -> tuple[list[tuple[str, list[IndexEntry]]], bool]:
        """Generate the prefs-index index."""
        content = defaultdict(list)

        domain = cast("PreferencesDomain", self.domain)

        preferences = sorted(list(domain.get_preferences()))

        currpack = ""

        for preference in sorted(preferences):
            # Group by the first letter after "org.phoebus." or "org.csstudio",
            # else everything would be under the "o" letter.
            pack = preference.fullname.split("/")[0]
            letter = (
                pack.removeprefix("org.phoebus.")
                .removeprefix("org.csstudio.")[0]
                .lower()
            )

            # If this is the first option of the package
            if pack != currpack:
                currpack = pack
                try:
                    # Generate a package entry that will contain subsequent options
                    content[letter].append(self._package_entry(pack))
                except Exception:
                    logger.warning("Couldn't find package %s for index")

            # Generate the preference entry,
            # with subtype 2 so that it's inside the package entry
            #
            # name, subtype, docname, anchor, extra, qualifier, description
            content[letter].append(
                IndexEntry(
                    preference.name,
                    2,
                    preference.docname,
                    preference.anchor,
                    "",
                    "",
                    preference.typ,
                )
            )

        # False here means that the entries won't be collapsed by default
        return sorted(content.items()), False


class PreferencesXRefRole(XRefRole):
    """A cross-reference role that will store the current package as context.

    So that it can be used later when resolving the cross-reference."""

    def process_link(
        self,
        env: BuildEnvironment,
        refnode: nodes.Element,
        has_explicit_title: bool,
        title: str,
        target: str,
    ) -> tuple[str, str]:
        refnode["prefs:packages"] = env.ref_context.get(
            "prefs:packages", [""]
        )[-1]
        return super().process_link(env, refnode, has_explicit_title, title, target)


# (name, dispname, type, docname, anchor, priority)
object_data = tuple[str, str, str, str, str, int]


class PreferencesDomain(Domain):
    """A Sphinx domain for documenting Phoebus' preferences, grouped by packages."""

    name = "prefs"
    label = "Preference"

    object_types: dict[str, ObjType] = {
        "package": ObjType("preference", "pack", "ref"),
        "preference": ObjType("preference", "pref", "ref"),
    }

    roles = {
        "pack": PreferencesXRefRole(warn_dangling=True),
        "pref": PreferencesXRefRole(warn_dangling=True),
        "ref": PreferencesXRefRole(warn_dangling=True),
    }
    directives = {
        # Individual objects
        "package": PackageDirective,
        "preference": PreferenceDirective,
        # Autodoc
        "autopackage": AutoPackageDirective,
        "listing": ListingDirective,
    }
    indices = [PreferencesIndex]
    initial_data = {
        "packages": [],
        "preferences": [],
    }
    data_version = 0

    def resolve_xref(
        self,
        _env: BuildEnvironment,
        fromdocname: str,
        builder: Builder,
        typ: str,
        target: str,
        node: addnodes.pending_xref,
        contnode: nodes.Element,
    ) -> nodes.Element | None:
        """Resolve the pending_xref node with the given role and target."""
        # 'typ' is the role, which can be "pack", "pref", "ref".
        # "ref" can refer to multiple type of objects,
        # so we need to iterate over that.
        objtypes = self.objtypes_for_role(typ)

        if not objtypes:
            return None

        for objtype in objtypes:
            if res := self._resolve_single_type_xref(
                fromdocname,
                builder,
                objtype,
                target,
                node,
                contnode,
            ):
                return res

        return None

    def _resolve_single_type_xref(
        self,
        fromdocname: str,
        builder: Builder,
        objtype: str,
        target: str,
        node: addnodes.pending_xref,
        contnode: nodes.Element,
    ) -> nodes.Element | None:
        """Resolve the pending_xref node with the given object type and target."""
        object_getter = None
        if objtype == "package":
            object_getter = self.get_packages
        elif objtype == "preference":
            object_getter = self.get_preferences
        else:
            logger.warning("Unknown preference object type: %s", objtype, location=node)
            return None

        candidates = [target]
        # Get the saved package context,
        # so that we can resolve the cross-reference relative to that.
        context_package = node.get("prefs:packages", [])
        if context_package:
            candidates.append(f"{context_package}/{target}")

        matches = [
            entity for entity in object_getter() if entity.fullname in candidates
        ]

        if len(matches) > 0:
            entity = matches[0]
            return make_refnode(
                builder,
                fromdocname,
                entity.docname,
                entity.anchor,
                contnode,
                f"{entity.typ.human_name()}",
            )

        return None

    def get_packages(self) -> Generator[RefEntity]:
        yield from self.data["packages"]

    def get_preferences(self) -> Generator[RefEntity]:
        yield from self.data["preferences"]

    def get_entities(self) -> Generator[RefEntity]:
        yield from self.get_packages()
        yield from self.get_preferences()

    def get_objects(self) -> Generator[object_data]:
        for entity in self.get_entities():
            yield entity.to_tuple()

    def add_package(self, name: str):
        """Save a package in the domain."""
        self.data["packages"].append(
            RefEntity(
                name,
                name,
                EntityType.PACKAGE,
                self.env.docname,
                _package_target(name),
                0,
            )
        )

    def add_preference(self, fullname: str):
        """Save a preference in the domain."""
        self.data["preferences"].append(
            RefEntity(
                fullname.split("/")[-1],
                fullname,
                EntityType.PREFERENCE,
                self.env.docname,
                _preference_target(fullname),
                0,
            )
        )
