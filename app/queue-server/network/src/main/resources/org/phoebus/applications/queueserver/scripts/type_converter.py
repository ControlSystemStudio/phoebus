"""
Type converter script for Queue Server parameter parsing.

This script uses Python's ast.literal_eval to safely parse parameter values
from string representations, matching the behavior of the Python Qt implementation.

Usage from Java:
    - Pass 'parameters_json' as a JSON string containing parameter definitions
    - Returns 'result' as a JSON string with parsed values
"""

import ast
import json
import sys

# Sentinel value for empty/unset parameters (Jython 2.7 doesn't have inspect.Parameter.empty)
EMPTY = object()

def parse_literal_value(value_str):
    """
    Parse a string representation of a value into its proper Python type.
    Mimics ast.literal_eval behavior used in the PyQt implementation.

    Args:
        value_str: String representation of the value

    Returns:
        Parsed value with appropriate Python type
    """
    if value_str is None or value_str == '':
        return EMPTY

    value_str = value_str.strip()

    # Handle None/null
    if value_str in ('None', 'null'):
        return None

    # Handle booleans
    if value_str in ('True', 'true'):
        return True
    if value_str in ('False', 'false'):
        return False

    # Handle quoted strings
    if ((value_str.startswith("'") and value_str.endswith("'")) or
        (value_str.startswith('"') and value_str.endswith('"'))):
        return value_str[1:-1]

    # Try numeric parsing
    try:
        if '.' in value_str:
            return float(value_str)
        else:
            return int(value_str)
    except ValueError:
        pass

    # Try ast.literal_eval for lists, dicts, tuples, etc.
    try:
        return ast.literal_eval(value_str)
    except (ValueError, SyntaxError):
        # If all else fails, return as string
        return value_str


def convert_parameters(parameters_data):
    """
    Convert parameter values from string representations to typed objects.

    Args:
        parameters_data: List of parameter dictionaries with 'name', 'value', 'enabled' fields

    Returns:
        Dictionary mapping parameter names to their parsed values
    """
    result = {}

    for param in parameters_data:
        param_name = param.get('name')
        param_value = param.get('value')
        is_enabled = param.get('enabled', True)
        default_value = param.get('defaultValue')

        # Skip disabled parameters entirely - they should not be included in kwargs
        if not is_enabled:
            continue

        # Skip parameters with no value (empty and no default)
        if param_value is None or param_value == '':
            continue

        try:
            # Parse the value string to proper type
            parsed_value = parse_literal_value(param_value)

            # Only include if it's not empty
            if parsed_value != EMPTY:
                result[param_name] = parsed_value

        except Exception as e:
            # Log error but continue processing other parameters
            sys.stderr.write("Warning: Failed to parse parameter '%s' with value '%s': %s\n" % (param_name, param_value, e))
            # Fall back to string value
            result[param_name] = param_value

    return result


def validate_parameters(parameters_data):
    """
    Validate parameter values and return validation results.

    Args:
        parameters_data: List of parameter dictionaries

    Returns:
        Dictionary with validation results for each parameter
    """
    validation_results = {}

    for param in parameters_data:
        param_name = param.get('name')
        param_value = param.get('value')
        is_enabled = param.get('enabled', True)
        is_optional = param.get('isOptional', False)

        if not is_enabled:
            validation_results[param_name] = {'valid': True, 'message': 'Disabled'}
            continue

        if param_value is None or param_value == '':
            is_valid = is_optional
            validation_results[param_name] = {
                'valid': is_valid,
                'message': 'Required parameter missing' if not is_valid else 'OK'
            }
            continue

        try:
            # Try to parse the value
            parse_literal_value(param_value)
            validation_results[param_name] = {'valid': True, 'message': 'OK'}
        except Exception as e:
            validation_results[param_name] = {
                'valid': False,
                'message': 'Parse error: %s' % str(e)
            }

    return validation_results


# Main execution
if __name__ == '__main__':
    # When run directly (for testing)
    test_params = [
        {'name': 'detector', 'value': "'det1'", 'enabled': True, 'isOptional': False},
        {'name': 'num_points', 'value': '10', 'enabled': True, 'isOptional': False},
        {'name': 'exposure', 'value': '0.5', 'enabled': True, 'isOptional': False},
        {'name': 'metadata', 'value': "{'key': 'value'}", 'enabled': True, 'isOptional': True},
    ]

    result = convert_parameters(test_params)
    # Don't print - just for testing
    pass

# Script entry point for Jython execution
# Expects: parameters_json (input), sets: result (output)
try:
    if 'parameters_json' in dir():
        # Parse input JSON
        params_data = json.loads(parameters_json)

        # Convert parameters
        converted = convert_parameters(params_data)

        # Set result as JSON string
        result = json.dumps(converted)

except Exception as e:
    # Return error as result
    result = json.dumps({
        'error': str(e),
        'type': type(e).__name__
    })
    sys.stderr.write("Error in type_converter.py: %s\n" % e)
