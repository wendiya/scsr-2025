#!/bin/bash

# Script to run student program tests and organize results
# Tests overflow, division by zero, and taint analysis on student submissions

# Create main results directory
RESULTS_DIR="student_test_results"
mkdir -p "$RESULTS_DIR"

# Get timestamp for this run
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
echo "Starting student programs test run at: $TIMESTAMP"

# Define student programs directories
STUDENT_PROGRAMS_DIR="student-programs"
OVERFLOW_DIR="$STUDENT_PROGRAMS_DIR/overflow"
DIVBYZERO_DIR="$STUDENT_PROGRAMS_DIR/divbyzero"
TAINT_DIR="$STUDENT_PROGRAMS_DIR/taint"

# Function to run a single test and capture output
run_student_test() {
    local test_name=$1
    local test_class=$2
    local output_file="$RESULTS_DIR/${test_name}_${TIMESTAMP}.txt"
    
    echo "========================================" | tee -a "$output_file"
    echo "Running: $test_name" | tee -a "$output_file"
    echo "Test Class: $test_class" | tee -a "$output_file"
    echo "Started at: $(date)" | tee -a "$output_file"
    echo "========================================" | tee -a "$output_file"
    echo "" | tee -a "$output_file"
    
    # Run the test and capture both stdout and stderr
    ./gradlew test --tests "$test_class" 2>&1 | tee -a "$output_file"
    
    local exit_code=${PIPESTATUS[0]}
    
    echo "" | tee -a "$output_file"
    echo "========================================" | tee -a "$output_file"
    echo "Test completed at: $(date)" | tee -a "$output_file"
    echo "Exit code: $exit_code" | tee -a "$output_file"
    echo "========================================" | tee -a "$output_file"
    
    # Copy analysis outputs if they exist
    copy_student_outputs "$test_name"
    
    return $exit_code
}

# Function to copy analysis outputs to student_test_results
copy_student_outputs() {
    local test_name=$1
    local outputs_dir="$RESULTS_DIR/${test_name}_outputs_${TIMESTAMP}"
    
    # Create outputs directory for this test
    mkdir -p "$outputs_dir"
    
    # Copy outputs based on test type
    case "$test_name" in
        *Overflow*)
            if [ -d "outputs/student-programs/overflow" ]; then
                cp -r outputs/student-programs/overflow/* "$outputs_dir/" 2>/dev/null || echo "No overflow outputs found for $test_name"
            fi
            ;;
        *DivisionByZero*)
            if [ -d "outputs/student-programs/divisionbyzero" ]; then
                cp -r outputs/student-programs/divisionbyzero/* "$outputs_dir/" 2>/dev/null || echo "No division by zero outputs found for $test_name"
            fi
            ;;
        *Taint*)
            if [ -d "outputs/student-programs/taintthreelevels" ]; then
                cp -r outputs/student-programs/taintthreelevels/* "$outputs_dir/" 2>/dev/null || echo "No taint outputs found for $test_name"
            fi
            ;;
    esac
    
    # Count files copied
    local file_count=$(find "$outputs_dir" -type f 2>/dev/null | wc -l)
    echo "Copied $file_count analysis files to $outputs_dir"
}

# Function to check if student programs exist
check_student_programs() {
    local category=$1
    local dir_path="$STUDENT_PROGRAMS_DIR/$category"
    
    if [ ! -d "$dir_path" ]; then
        echo "Warning: Student programs directory not found: $dir_path"
        return 1
    fi
    
    local file_count=$(find "$dir_path" -name "*.imp" | wc -l)
    if [ $file_count -eq 0 ]; then
        echo "Warning: No .imp files found in: $dir_path"
        return 1
    fi
    
    echo "Found $file_count .imp files in $dir_path"
    return 0
}

# Array of tests to run
declare -A tests=(
    ["StudentOverflowChecker"]="StudentOverflowCheckerTest"
    ["StudentDivisionByZeroChecker"]="StudentDivisionByZeroCheckerTest" 
    ["StudentTaintChecker"]="StudentTaintCheckerTest"
)

# Summary file
SUMMARY_FILE="$RESULTS_DIR/student_test_summary_${TIMESTAMP}.txt"

echo "========================================" | tee "$SUMMARY_FILE"
echo "STUDENT PROGRAMS TEST SUITE EXECUTION" | tee -a "$SUMMARY_FILE"
echo "========================================" | tee -a "$SUMMARY_FILE"
echo "Start time: $TIMESTAMP" | tee -a "$SUMMARY_FILE"
echo "Total tests: ${#tests[@]}" | tee -a "$SUMMARY_FILE"
echo "" | tee -a "$SUMMARY_FILE"

# Check student programs directories
echo "Checking student programs directories..." | tee -a "$SUMMARY_FILE"
check_student_programs "overflow" | tee -a "$SUMMARY_FILE"
check_student_programs "divbyzero" | tee -a "$SUMMARY_FILE"
check_student_programs "taint" | tee -a "$SUMMARY_FILE"
echo "" | tee -a "$SUMMARY_FILE"

# List student programs
echo "========================================" | tee -a "$SUMMARY_FILE"
echo "STUDENT PROGRAMS INVENTORY" | tee -a "$SUMMARY_FILE"
echo "========================================" | tee -a "$SUMMARY_FILE"

if [ -d "$OVERFLOW_DIR" ]; then
    echo "Overflow programs:" | tee -a "$SUMMARY_FILE"
    find "$OVERFLOW_DIR" -name "*.imp" | sort | while read file; do
        echo "  - $(basename "$file")" | tee -a "$SUMMARY_FILE"
    done
    echo "" | tee -a "$SUMMARY_FILE"
fi

if [ -d "$DIVBYZERO_DIR" ]; then
    echo "Division by Zero programs:" | tee -a "$SUMMARY_FILE"
    find "$DIVBYZERO_DIR" -name "*.imp" | sort | while read file; do
        echo "  - $(basename "$file")" | tee -a "$SUMMARY_FILE"
    done
    echo "" | tee -a "$SUMMARY_FILE"
fi

if [ -d "$TAINT_DIR" ]; then
    echo "Taint Analysis programs:" | tee -a "$SUMMARY_FILE"
    find "$TAINT_DIR" -name "*.imp" | sort | while read file; do
        echo "  - $(basename "$file")" | tee -a "$SUMMARY_FILE"
    done
    echo "" | tee -a "$SUMMARY_FILE"
fi

echo "========================================" | tee -a "$SUMMARY_FILE"
echo "RUNNING TESTS" | tee -a "$SUMMARY_FILE"
echo "========================================" | tee -a "$SUMMARY_FILE"

# Run all tests
passed=0
failed=0
failed_tests=()
test_count=0

for test_name in "${!tests[@]}"; do
    test_class="${tests[$test_name]}"
    test_count=$((test_count + 1))
    
    echo "Running test $test_count/${#tests[@]}: $test_name"
    echo "Test class: $test_class"
    
    if run_student_test "$test_name" "$test_class"; then
        echo "✓ PASSED: $test_name" | tee -a "$SUMMARY_FILE"
        ((passed++))
    else
        echo "✗ FAILED: $test_name" | tee -a "$SUMMARY_FILE"
        failed_tests+=("$test_name")
        ((failed++))
    fi
    
    echo "" | tee -a "$SUMMARY_FILE"
    
    # Small delay between tests
    sleep 2
done

# Final summary
echo "========================================" | tee -a "$SUMMARY_FILE"
echo "EXECUTION SUMMARY" | tee -a "$SUMMARY_FILE"
echo "========================================" | tee -a "$SUMMARY_FILE"
echo "Total tests run: ${#tests[@]}" | tee -a "$SUMMARY_FILE"
echo "Passed: $passed" | tee -a "$SUMMARY_FILE"
echo "Failed: $failed" | tee -a "$SUMMARY_FILE"
echo "End time: $(date)" | tee -a "$SUMMARY_FILE"
echo "" | tee -a "$SUMMARY_FILE"

if [ $failed -gt 0 ]; then
    echo "FAILED TESTS:" | tee -a "$SUMMARY_FILE"
    for failed_test in "${failed_tests[@]}"; do
        echo "  - $failed_test" | tee -a "$SUMMARY_FILE"
    done
    echo "" | tee -a "$SUMMARY_FILE"
fi

# Create index file
INDEX_FILE="$RESULTS_DIR/index.html"
cat > "$INDEX_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Student Programs Test Results - $TIMESTAMP</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .summary { background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 30px; }
        .test-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; }
        .test-card { border: 1px solid #ddd; padding: 15px; border-radius: 5px; }
        .passed { border-left: 5px solid #4CAF50; }
        .failed { border-left: 5px solid #f44336; }
        .links a { display: block; margin: 5px 0; }
        .programs-list { margin-top: 20px; }
        .programs-category { margin-bottom: 15px; }
        .programs-category h4 { margin-bottom: 5px; color: #333; }
        .program-files { font-size: 0.9em; color: #666; margin-left: 20px; }
    </style>
</head>
<body>
    <h1>Student Programs Test Results</h1>
    
    <div class="summary">
        <h2>Summary</h2>
        <p><strong>Execution Time:</strong> $TIMESTAMP</p>
        <p><strong>Total Tests:</strong> ${#tests[@]}</p>
        <p><strong>Passed:</strong> $passed</p>
        <p><strong>Failed:</strong> $failed</p>
        <p><a href="student_test_summary_${TIMESTAMP}.txt">View Full Summary</a></p>
        
        <div class="programs-list">
            <h3>Student Programs Analyzed</h3>
EOF

# Add program listings to HTML
if [ -d "$OVERFLOW_DIR" ]; then
    cat >> "$INDEX_FILE" << EOF
            <div class="programs-category">
                <h4>Overflow Analysis Programs:</h4>
                <div class="program-files">
EOF
    find "$OVERFLOW_DIR" -name "*.imp" | sort | while read file; do
        echo "                    $(basename "$file")<br>" >> "$INDEX_FILE"
    done
    cat >> "$INDEX_FILE" << EOF
                </div>
            </div>
EOF
fi

if [ -d "$DIVBYZERO_DIR" ]; then
    cat >> "$INDEX_FILE" << EOF
            <div class="programs-category">
                <h4>Division by Zero Analysis Programs:</h4>
                <div class="program-files">
EOF
    find "$DIVBYZERO_DIR" -name "*.imp" | sort | while read file; do
        echo "                    $(basename "$file")<br>" >> "$INDEX_FILE"
    done
    cat >> "$INDEX_FILE" << EOF
                </div>
            </div>
EOF
fi

if [ -d "$TAINT_DIR" ]; then
    cat >> "$INDEX_FILE" << EOF
            <div class="programs-category">
                <h4>Taint Analysis Programs:</h4>
                <div class="program-files">
EOF
    find "$TAINT_DIR" -name "*.imp" | sort | while read file; do
        echo "                    $(basename "$file")<br>" >> "$INDEX_FILE"
    done
    cat >> "$INDEX_FILE" << EOF
                </div>
            </div>
EOF
fi

cat >> "$INDEX_FILE" << EOF
        </div>
    </div>
    
    <div class="test-grid">
EOF

# Add test cards to index
for test_name in "${!tests[@]}"; do
    if [[ " ${failed_tests[@]} " =~ " ${test_name} " ]]; then
        status="failed"
        icon="✗"
    else
        status="passed"
        icon="✓"
    fi
    
    cat >> "$INDEX_FILE" << EOF
        <div class="test-card $status">
            <h3>$icon $test_name</h3>
            <div class="links">
                <a href="${test_name}_${TIMESTAMP}.txt">Test Output Log</a>
                <a href="${test_name}_outputs_${TIMESTAMP}/">Analysis Outputs</a>
            </div>
        </div>
EOF
done

cat >> "$INDEX_FILE" << EOF
    </div>
</body>
</html>
EOF

# Create README for results
README_FILE="$RESULTS_DIR/README.md"
cat > "$README_FILE" << EOF
# Student Programs Test Results

## Overview
This directory contains the results of running automated tests on student-submitted programs for static analysis validation.

**Execution Time:** $TIMESTAMP  
**Tests Run:** ${#tests[@]}  
**Passed:** $passed  
**Failed:** $failed  

## Test Categories

### StudentOverflowCheckerTest
Tests overflow/underflow detection on student programs in \`student-programs/overflow/\`
- **Target:** Integer and floating-point overflow detection
- **Analysis:** Interval analysis with various numerical sizes

### StudentDivisionByZeroCheckerTest  
Tests division by zero detection on student programs in \`student-programs/divbyzero/\`
- **Target:** Division by zero vulnerability detection
- **Analysis:** Value analysis to detect zero divisors

### StudentTaintCheckerTest
Tests taint analysis on student programs in \`student-programs/taint/\`
- **Target:** Information flow security analysis
- **Analysis:** Three-level taint tracking (CLEAN/TAINT/TOP)

## Files Structure

- \`index.html\` - Interactive overview of all test results
- \`student_test_summary_\${TIMESTAMP}.txt\` - Detailed execution summary
- \`\${TestName}_\${TIMESTAMP}.txt\` - Individual test output logs
- \`\${TestName}_outputs_\${TIMESTAMP}/\` - Analysis outputs and graphs

## Student Programs Analyzed

EOF

# Add program counts to README
if [ -d "$OVERFLOW_DIR" ]; then
    overflow_count=$(find "$OVERFLOW_DIR" -name "*.imp" | wc -l)
    echo "- **Overflow Programs:** $overflow_count files" >> "$README_FILE"
fi

if [ -d "$DIVBYZERO_DIR" ]; then
    divbyzero_count=$(find "$DIVBYZERO_DIR" -name "*.imp" | wc -l)
    echo "- **Division by Zero Programs:** $divbyzero_count files" >> "$README_FILE"
fi

if [ -d "$TAINT_DIR" ]; then
    taint_count=$(find "$TAINT_DIR" -name "*.imp" | wc -l)
    echo "- **Taint Analysis Programs:** $taint_count files" >> "$README_FILE"
fi

cat >> "$README_FILE" << EOF

## Usage

1. Open \`index.html\` in a web browser for an interactive overview
2. Review individual test logs for detailed execution information
3. Examine analysis outputs for specific programs that triggered warnings
4. Check the summary file for overall test execution statistics

## Interpreting Results

- **PASSED:** Test executed successfully and analyzed all student programs
- **FAILED:** Test encountered errors during execution
- **Analysis Outputs:** Generated graphs and reports showing detected vulnerabilities

EOF

echo "========================================"
echo "Student programs testing completed!"
echo "Results saved in: $RESULTS_DIR/"
echo "Open $INDEX_FILE in a browser to view results"
echo "Summary: $passed passed, $failed failed"
echo ""
echo "Quick start:"
echo "  cd $RESULTS_DIR && python -m http.server 8080"
echo "  Then open: http://localhost:8080"
echo "========================================"

# Return appropriate exit code
if [ $failed -gt 0 ]; then
    exit 1
else
    exit 0
fi
