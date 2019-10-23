core-formula
============

Parser and evaluator for formulas like "2+6" or "sin(PI/2)".

Formula parser operates in three different modes:

1) Parse a formula without variables.
2) Parse a formula with known variables.
3) Parse a formula and automatically detect the referenced variables.

Key classes:

 * `Formula`: Parser and evaluator
 * `VariableNode`: Variable (name, value)
 * `FormulaFunction`: SPI for adding functions to the parser

See `FormulaUnitTest` for examples.

The formula fundamentally operates on `VType` data,
but most operations only support numeric scalars.

This package defines only the formula parser and evaluator.
The core-pv package binds variables to PVs.
