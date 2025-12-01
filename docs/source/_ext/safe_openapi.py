import os
from docutils import nodes
from docutils.parsers.rst import Directive
from sphinx.util.nodes import nested_parse_with_titles

class SafeOpenApiDirective(Directive):
    """
    Usage :
        .. safe_openapi:: path/to/openapi.yaml
    """
    required_arguments = 1

    def run(self):
        env = self.state.document.settings.env
        docdir = os.path.dirname(env.doc2path(env.docname))

        # Argument from the directive
        openapi_path = self.arguments[0]

        # Find absolute path
        full_path = os.path.normpath(os.path.join(docdir, openapi_path))

        if os.path.exists(full_path):
            # add the normal `openapi` directive
            node = nodes.paragraph()
            directive_text = f".. openapi:: {openapi_path}\n"
            self.state_machine.insert_input([directive_text], self.state_machine.input_lines.source(0))
            return []
        else:
            # add file not found
            warning = nodes.paragraph(text=f"OpenAPI file {openapi_path} not found")
            return [warning]


def setup(app):
    app.add_directive("safe_openapi", SafeOpenApiDirective)
    return {"version": "1.0"}
