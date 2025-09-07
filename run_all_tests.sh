#!/bin/bash

# Script to run all ComprehensiveTestSuite tests and organize results
# Creates test_results folder with individual output files and graphs

# Create main results directory
RESULTS_DIR="test_results"
mkdir -p "$RESULTS_DIR"

# Get timestamp for this run
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
echo "Starting comprehensive test run at: $TIMESTAMP"

# Function to run a single test and capture output
run_test() {
    local test_name=$1
    local test_class="ComprehensiveTestSuite.$test_name"
    local output_file="$RESULTS_DIR/${test_name}_${TIMESTAMP}.txt"
    
    echo "========================================" | tee -a "$output_file"
    echo "Running: $test_name" | tee -a "$output_file"
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
    
    # Copy analysis graphs if they exist
    copy_analysis_graphs "$test_name"
    
    return $exit_code
}

# Function to copy analysis graphs to test_results
copy_analysis_graphs() {
    local test_name=$1
    local graphs_dir="$RESULTS_DIR/${test_name}_graphs_${TIMESTAMP}"
    
    # Create graphs directory for this test
    mkdir -p "$graphs_dir"
    
    # Map test names to their expected output directories
    case "$test_name" in
        testOverflow*)
            if [ -d "outputs/overflow" ]; then
                cp -r outputs/overflow/* "$graphs_dir/" 2>/dev/null || echo "No overflow graphs found for $test_name"
            fi
            ;;
        testDivisionByZero*)
            if [ -d "outputs/divbyzero" ]; then
                cp -r outputs/divbyzero/* "$graphs_dir/" 2>/dev/null || echo "No divbyzero graphs found for $test_name"
            fi
            ;;
        testTaint*)
            if [ -d "outputs/taint" ]; then
                cp -r outputs/taint/* "$graphs_dir/" 2>/dev/null || echo "No taint graphs found for $test_name"
            fi
            ;;
        testCombined*)
            if [ -d "outputs/combined" ]; then
                cp -r outputs/combined/* "$graphs_dir/" 2>/dev/null || echo "No combined graphs found for $test_name"
            fi
            ;;
        testWeb* | testFinancial* | testHealthcare*)
            if [ -d "outputs/taint" ]; then
                cp -r outputs/taint/* "$graphs_dir/" 2>/dev/null || echo "No security taint graphs found for $test_name"
            fi
            ;;
    esac
    
    # Count files copied
    local file_count=$(find "$graphs_dir" -type f 2>/dev/null | wc -l)
    echo "Copied $file_count analysis files to $graphs_dir"
}

# Array of all tests to run
tests=(
    "testOverflowComplexUINT8"
    "testOverflowComplexINT16" 
    "testOverflowComplexFLOAT32"
    "testOverflowBankingUINT8"
    "testOverflowImageProcessingINT32"
    "testOverflowCryptominingUINT32"
    "testOverflowPentagonsComplex"
    "testDivisionByZeroComplex"
    "testDivisionByZeroBanking"
    "testDivisionByZeroImageProcessing"
    "testDivisionByZeroCryptomining"
    "testDivisionByZeroPentagonsComplex"
    "testTaintThreeLevelsComplex"
    "testTaintThreeLevelsBanking"
    "testTaintThreeLevelsImageProcessing"
    "testCombinedAnalysisBanking"
    "testCombinedAnalysisImageProcessing"
    "testCombinedAnalysisCryptomining"
    "testWebApplicationSecurity" 
    "testFinancialApiSecurity" 
    "testHealthcareSecurity"
)

# Summary file
SUMMARY_FILE="$RESULTS_DIR/test_summary_${TIMESTAMP}.txt"

echo "========================================" | tee "$SUMMARY_FILE"
echo "COMPREHENSIVE TEST SUITE EXECUTION" | tee -a "$SUMMARY_FILE"
echo "========================================" | tee -a "$SUMMARY_FILE"
echo "Start time: $TIMESTAMP" | tee -a "$SUMMARY_FILE"
echo "Total tests: ${#tests[@]}" | tee -a "$SUMMARY_FILE"
echo "" | tee -a "$SUMMARY_FILE"

# Run all tests
passed=0
failed=0
failed_tests=()

for test in "${tests[@]}"; do
    echo "Running test $((passed + failed + 1))/${#tests[@]}: $test"
    
    if run_test "$test"; then
        echo "✓ PASSED: $test" | tee -a "$SUMMARY_FILE"
        ((passed++))
    else
        echo "✗ FAILED: $test" | tee -a "$SUMMARY_FILE"
        failed_tests+=("$test")
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
    <title>Test Results - $TIMESTAMP</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .summary { background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 30px; }
        .test-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .test-card { border: 1px solid #ddd; padding: 15px; border-radius: 5px; }
        .passed { border-left: 5px solid #4CAF50; }
        .failed { border-left: 5px solid #f44336; }
        .links a { display: block; margin: 5px 0; }
    </style>
</head>
<body>
    <h1>Comprehensive Test Suite Results</h1>
    
    <div class="summary">
        <h2>Summary</h2>
        <p><strong>Execution Time:</strong> $TIMESTAMP</p>
        <p><strong>Total Tests:</strong> ${#tests[@]}</p>
        <p><strong>Passed:</strong> $passed</p>
        <p><strong>Failed:</strong> $failed</p>
        <p><a href="test_summary_${TIMESTAMP}.txt">View Full Summary</a></p>
    </div>
    
    <div class="test-grid">
EOF

# Add test cards to index
for test in "${tests[@]}"; do
    if [[ " ${failed_tests[@]} " =~ " ${test} " ]]; then
        status="failed"
        icon="✗"
    else
        status="passed"
        icon="✓"
    fi
    
    cat >> "$INDEX_FILE" << EOF
        <div class="test-card $status">
            <h3>$icon $test</h3>
            <div class="links">
                <a href="${test}_${TIMESTAMP}.txt">Output Log</a>
                <a href="${test}_graphs_${TIMESTAMP}/">Analysis Graphs</a>
            </div>
        </div>
EOF
done

cat >> "$INDEX_FILE" << EOF
    </div>
</body>
</html>
EOF

echo "========================================"
echo "All tests completed!"
echo "Results saved in: $RESULTS_DIR/"
echo "Open $INDEX_FILE in a browser to view results"
echo "Summary: $passed passed, $failed failed"
echo "========================================"

# Return appropriate exit code
if [ $failed -gt 0 ]; then
    exit 1
else
    exit 0
fi
