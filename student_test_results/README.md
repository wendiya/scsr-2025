# Student Programs Test Results

## Overview
This directory contains the results of running automated tests on student-submitted programs for static analysis validation.

**Execution Time:** 2025-09-07_19-09-38  
**Tests Run:** 1  
**Passed:** 0  
**Failed:** 1  

## Test Categories

### StudentOverflowCheckerTest
Tests overflow/underflow detection on student programs in `student-programs/overflow/`
- **Target:** Integer and floating-point overflow detection
- **Analysis:** Interval analysis with various numerical sizes

### StudentDivisionByZeroCheckerTest  
Tests division by zero detection on student programs in `student-programs/divbyzero/`
- **Target:** Division by zero vulnerability detection
- **Analysis:** Value analysis to detect zero divisors

### StudentTaintCheckerTest
Tests taint analysis on student programs in `student-programs/taint/`
- **Target:** Information flow security analysis
- **Analysis:** Three-level taint tracking (CLEAN/TAINT/TOP)

## Files Structure

- `index.html` - Interactive overview of all test results
- `student_test_summary_${TIMESTAMP}.txt` - Detailed execution summary
- `${TestName}_${TIMESTAMP}.txt` - Individual test output logs
- `${TestName}_outputs_${TIMESTAMP}/` - Analysis outputs and graphs

## Student Programs Analyzed

- **Overflow Programs:**       16 files
- **Division by Zero Programs:**       13 files
- **Taint Analysis Programs:**       12 files

## Usage

1. Open `index.html` in a web browser for an interactive overview
2. Review individual test logs for detailed execution information
3. Examine analysis outputs for specific programs that triggered warnings
4. Check the summary file for overall test execution statistics

## Interpreting Results

- **PASSED:** Test executed successfully and analyzed all student programs
- **FAILED:** Test encountered errors during execution
- **Analysis Outputs:** Generated graphs and reports showing detected vulnerabilities

