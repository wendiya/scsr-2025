package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.TaintThreeLevels;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.TaintChecker;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import it.unive.scsr.NumericalSize;
import it.unive.scsr.checkers.DivisionByZeroChecker;

public class ComprehensiveTestSuite {

	// Define the signatures for matching sources, sanitizers, and sinks
	String[] sources = new String[] {"source1", "source2"};
	String[] sanitizers = new String[] {"sanitizer1", "sanitizer2"};
	String[] sinks = new String[] {"sink1", "sinks"};

	// ===== NEW TAINT ANALYSIS TESTS =====
	
	@Test
	public void testWebApplicationSecurity() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING WEB APPLICATION SECURITY TAINT ANALYSIS ===");
		
		String[] sources = {"source1", "source2", "getUserInput", "getCookieValue", "getUrlParameter"};
		String[] sanitizers = {"sanitizer1", "sanitizer2", "validateInput", "sqlEscape", "htmlEncode"};
		String[] sinks = {"sink1", "sinks", "executeQuery", "renderPage", "logMessage"};
		
		runTaintThreeLevelsAnalysisWithArrays("inputs/web_application_security.imp", "web-application-security", sources, sanitizers, sinks);
		
		System.out.println("=== WEB APPLICATION SECURITY ANALYSIS FINISHED ===\n");
	}

	@Test
	public void testFinancialApiSecurity() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING FINANCIAL API SECURITY TAINT ANALYSIS ===");
		
		String[] sources = {"source1", "source2", "getAPIRequest", "getTransactionData", "getExternalFeed"};
		String[] sanitizers = {"sanitizer1", "sanitizer2", "validateAccount", "validateAmount", "encryptSensitive"};
		String[] sinks = {"sink1", "sinks", "executeTransaction", "updateBalance", "sendAuditLog"};
		
		runTaintThreeLevelsAnalysisWithArrays("inputs/financial_api_security.imp", "financial-api-security", sources, sanitizers, sinks);
		
		System.out.println("=== FINANCIAL API SECURITY ANALYSIS FINISHED ===\n");
	}

	@Test
	public void testHealthcareSecurity() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING HEALTHCARE SECURITY TAINT ANALYSIS ===");
		
		String[] sources = {"source1", "source2", "getPatientInput", "getMedicalDevice", "getExternalLab"};
		String[] sanitizers = {"sanitizer1", "sanitizer2", "validateMedicalID", "anonymizePatient", "encryptPHI"};
		String[] sinks = {"sink1", "sinks", "updateMedicalRecord", "sendInsuranceClaim", "alertMedicalStaff"};
		
		runTaintThreeLevelsAnalysisWithArrays("inputs/healthcare_security.imp", "healthcare-security", sources, sanitizers, sinks);
		
		System.out.println("=== HEALTHCARE SECURITY ANALYSIS FINISHED ===\n");
	}

	// ===== EXISTING OVERFLOW TESTS =====

	@Test
	public void testOverflowComplexUINT8() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: UINT8 COMPLEX ===");
		System.out.println("Input file: inputs/overflow-complex.imp");
		System.out.println("Target type: UINT8 (range: 0-255)");
		System.out.println("Domain Intervals");
		System.out.println("Expected: Multiple overflow warnings for arithmetic operations exceeding 255");
		
		runOverflowAnalysis("inputs/overflow-complex.imp", NumericalSize.UINT8, "overflow-complex-uint8", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/overflow-complex-uint8/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS UINT8 COMPLEX FINISHED ===\n");
	}

	@Test
	public void testOverflowComplexINT16() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: INT16 COMPLEX ===");
		System.out.println("Input file: inputs/overflow-complex.imp");
		System.out.println("Target type: INT16 (range: -32768 to 32767)");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Fewer overflow warnings due to larger range, signed arithmetic handling");
		
		runOverflowAnalysis("inputs/overflow-complex.imp", NumericalSize.INT16, "overflow-complex-int16", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/overflow-complex-int16/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS INT16 COMPLEX FINISHED ===\n");
	}

	@Test
	public void testOverflowComplexFLOAT32() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: FLOAT32 COMPLEX ===");
		System.out.println("Input file: inputs/overflow-complex.imp");
		System.out.println("Target type: FLOAT32 (range: -3.4e38 to 3.4e38)");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Precision loss warnings instead of hard overflows");
		
		runOverflowAnalysis("inputs/overflow-complex.imp", NumericalSize.FLOAT32, "overflow-complex-float32", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/overflow-complex-float32/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS FLOAT32 COMPLEX FINISHED ===\n");
	}

	@Test
	public void testOverflowBankingUINT8() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: BANKING SIMULATION UINT8 ===");
		System.out.println("Input file: inputs/banking-simulation.imp");
		System.out.println("Target type: UINT8 (range: 0-255)");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Overflow in fee calculations, compound interest, balance operations");
		
		runOverflowAnalysis("inputs/banking-simulation.imp", NumericalSize.UINT8, "banking-overflow-uint8", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/banking-overflow-uint8/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS BANKING UINT8 FINISHED ===\n");
	}

	@Test
	public void testOverflowImageProcessingINT32() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: IMAGE PROCESSING INT32 ===");
		System.out.println("Input file: inputs/image-processing.imp");
		System.out.println("Target type: INT32 (range: -2147483648 to 2147483647)");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Pixel count overflow for large images, buffer size overflow");
		
		runOverflowAnalysis("inputs/image-processing.imp", NumericalSize.INT32, "image-overflow-int32", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/image-overflow-int32/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS IMAGE PROCESSING INT32 FINISHED ===\n");
	}

	@Test
	public void testOverflowCryptominingUINT32() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: CRYPTOCURRENCY MINING UINT32 ===");
		System.out.println("Input file: inputs/cryptocurrency-mining.imp");
		System.out.println("Target type: UINT32 (range: 0 to 4294967295)");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Hash rate calculation overflow, reward accumulation overflow");
		
		runOverflowAnalysis("inputs/cryptocurrency-mining.imp", NumericalSize.UINT32, "crypto-overflow-uint32", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/overflow/crypto-overflow-uint32/ for detailed results");
		System.out.println("=== OVERFLOW ANALYSIS CRYPTOCURRENCY UINT32 FINISHED ===\n");
	}

	// ===== EXISTING DIVISION BY ZERO TESTS =====

	@Test
	public void testDivisionByZeroComplex() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING DIVISION BY ZERO ANALYSIS: COMPLEX SCENARIOS ===");
		System.out.println("Input file: inputs/divbyzero-complex.imp");
		System.out.println("Target type: INT32 for divisor analysis");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Multiple definite and possible division by zero cases");
		
		runDivisionByZeroAnalysis("inputs/divbyzero-complex.imp", NumericalSize.INT32, "divbyzero-complex", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/divbyzero/divbyzero-complex/ for detailed results");
		System.out.println("=== DIVISION BY ZERO ANALYSIS COMPLEX FINISHED ===\n");
	}

	@Test
	public void testDivisionByZeroBanking() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING DIVISION BY ZERO ANALYSIS: BANKING SIMULATION ===");
		System.out.println("Input file: inputs/banking-simulation.imp");
		System.out.println("Target type: FLOAT32 for interest rate calculations");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Division by zero in interest calculations, precision issues");
		
		runDivisionByZeroAnalysis("inputs/banking-simulation.imp", NumericalSize.FLOAT32, "banking-divbyzero", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/divbyzero/banking-divbyzero/ for detailed results");
		System.out.println("=== DIVISION BY ZERO ANALYSIS BANKING FINISHED ===\n");
	}

	@Test
	public void testDivisionByZeroImageProcessing() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING DIVISION BY ZERO ANALYSIS: IMAGE PROCESSING ===");
		System.out.println("Input file: inputs/image-processing.imp");
		System.out.println("Target type: INT32 for scaling operations");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Division by zero in scaling operations, compression calculations");
		
		runDivisionByZeroAnalysis("inputs/image-processing.imp", NumericalSize.INT32, "image-divbyzero", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/divbyzero/image-divbyzero/ for detailed results");
		System.out.println("=== DIVISION BY ZERO ANALYSIS IMAGE PROCESSING FINISHED ===\n");
	}

	@Test
	public void testDivisionByZeroCryptomining() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING DIVISION BY ZERO ANALYSIS: CRYPTOCURRENCY MINING ===");
		System.out.println("Input file: inputs/cryptocurrency-mining.imp");
		System.out.println("Target type: FLOAT32 for profitability calculations");
		System.out.println("Domain: Intervals");
		System.out.println("Expected: Division by zero in profitability, efficiency calculations");
		
		runDivisionByZeroAnalysis("inputs/cryptocurrency-mining.imp", NumericalSize.FLOAT32, "crypto-divbyzero", 
			new ValueEnvironment<Intervals>(new Intervals()));
		
		System.out.println("Analysis completed. Check outputs/divbyzero/crypto-divbyzero/ for detailed results");
		System.out.println("=== DIVISION BY ZERO ANALYSIS CRYPTOCURRENCY FINISHED ===\n");
	}

	// ===== EXISTING TAINT TESTS =====

	@Test
	public void testTaintThreeLevelsComplex() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING TAINT ANALYSIS: THREE LEVELS COMPLEX ===");
		System.out.println("Input file: inputs/taint-complex.imp");
		System.out.println("Domain: TaintThreeLevels (TOP/TAINT/CLEAN/BOTTOM)");
		System.out.println("Expected: Definite taint flows, possible taint flows, sanitization effectiveness");
		
		runTaintThreeLevelsAnalysis("inputs/taint-complex.imp", "taint-complex-3lvs");
		
		System.out.println("Analysis completed. Check outputs/taint/taint-complex-3lvs/ for detailed results");
		System.out.println("=== TAINT ANALYSIS THREE LEVELS COMPLEX FINISHED ===\n");
	}

	@Test
	public void testTaintThreeLevelsBanking() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING TAINT ANALYSIS: BANKING SIMULATION ===");
		System.out.println("Input file: inputs/banking-simulation.imp");
		System.out.println("Domain: TaintThreeLevels");
		System.out.println("Expected: Taint analysis of financial data flows");
		
		runTaintThreeLevelsAnalysis("inputs/banking-simulation.imp", "banking-taint-3lvs");
		
		System.out.println("Analysis completed. Check outputs/taint/banking-taint-3lvs/ for detailed results");
		System.out.println("=== TAINT ANALYSIS BANKING FINISHED ===\n");
	}

	@Test
	public void testTaintThreeLevelsImageProcessing() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING TAINT ANALYSIS: IMAGE PROCESSING ===");
		System.out.println("Input file: inputs/image-processing.imp");
		System.out.println("Domain: TaintThreeLevels");
		System.out.println("Expected: Taint analysis of image data processing flows");
		
		runTaintThreeLevelsAnalysis("inputs/image-processing.imp", "image-taint-3lvs");
		
		System.out.println("Analysis completed. Check outputs/taint/image-taint-3lvs/ for detailed results");
		System.out.println("=== TAINT ANALYSIS IMAGE PROCESSING FINISHED ===\n");
	}

	// ===== EXISTING PENTAGONS TESTS =====

	@Test
	public void testOverflowPentagonsComplex() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING OVERFLOW ANALYSIS: PENTAGONS DOMAIN COMPLEX ===");
		System.out.println("Input file: inputs/overflow-complex.imp");
		System.out.println("Target type: UINT8 (range: 0-255)");
		System.out.println("Domain: Pentagons (relational domain with upper bounds)");
		System.out.println("Expected: More precise analysis with relational information");
		
		runOverflowAnalysisPentagons("inputs/overflow-complex.imp", NumericalSize.UINT8, "overflow-pentagons-complex");
		
		System.out.println("Analysis completed. Check outputs/overflow/overflow-pentagons-complex/ for detailed results");
		System.out.println("Compare with intervals domain results for precision differences");
		System.out.println("=== OVERFLOW ANALYSIS PENTAGONS COMPLEX FINISHED ===\n");
	}

	@Test
	public void testDivisionByZeroPentagonsComplex() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING DIVISION BY ZERO ANALYSIS: PENTAGONS DOMAIN COMPLEX ===");
		System.out.println("Input file: inputs/divbyzero-complex.imp");
		System.out.println("Target type: INT32 for divisor analysis");
		System.out.println("Domain: Pentagons (relational domain)");
		System.out.println("Expected: precision for relational constraints on divisors");
		
		runDivisionByZeroAnalysisPentagons("inputs/divbyzero-complex.imp", NumericalSize.INT32, "divbyzero-pentagons-complex");
		
		System.out.println("Analysis completed. Check outputs/divbyzero/divbyzero-pentagons-complex/ for detailed results");
		System.out.println("Compare with intervals domain for relational analysis benefits");
		System.out.println("=== DIVISION BY ZERO ANALYSIS PENTAGONS COMPLEX FINISHED ===\n");
	}

	// ===== EXISTING COMBINED TESTS =====

	@Test
	public void testCombinedAnalysisBanking() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING COMBINED ANALYSIS: BANKING SIMULATION ===");
		System.out.println("Input file: inputs/banking-simulation.imp");
		System.out.println("Target type: INT32");
		System.out.println("Checkers: Overflow + Division by Zero + Taint Analysis");
		System.out.println("Expected: Comprehensive vulnerability detection across multiple categories");
		
		runCombinedAnalysis("inputs/banking-simulation.imp", NumericalSize.INT32, "banking-combined");
		
		System.out.println("Analysis completed. Check outputs/combined/banking-combined/ for detailed results");
		System.out.println("Review all vulnerability types detected in single analysis run");
		System.out.println("=== COMBINED ANALYSIS BANKING FINISHED ===\n");
	}

	@Test
	public void testCombinedAnalysisImageProcessing() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING COMBINED ANALYSIS: IMAGE PROCESSING ===");
		System.out.println("Input file: inputs/image-processing.imp");
		System.out.println("Target type: UINT32");
		System.out.println("Checkers: Overflow + Division by Zero + Taint Analysis");
		System.out.println("Expected: Buffer overflow, scaling division by zero, pixel processing issues");
		
		runCombinedAnalysis("inputs/image-processing.imp", NumericalSize.UINT32, "image-combined");
		
		System.out.println("Analysis completed. Check outputs/combined/image-combined/ for detailed results");
		System.out.println("=== COMBINED ANALYSIS IMAGE PROCESSING FINISHED ===\n");
	}

	@Test
	public void testCombinedAnalysisCryptomining() throws ParsingException, AnalysisException {
		System.out.println("=== STARTING COMBINED ANALYSIS: CRYPTOCURRENCY MINING ===");
		System.out.println("Input file: inputs/cryptocurrency-mining.imp");
		System.out.println("Target type: UINT32");
		System.out.println("Checkers: Overflow + Division by Zero + Taint Analysis");
		System.out.println("Expected: Hash calculation overflow, profitability division by zero");
		
		runCombinedAnalysis("inputs/cryptocurrency-mining.imp", NumericalSize.UINT32, "crypto-combined");
		
		System.out.println("Analysis completed. Check outputs/combined/crypto-combined/ for detailed results");
		System.out.println("=== COMBINED ANALYSIS CRYPTOCURRENCY FINISHED ===\n");
	}

	// ===== HELPER METHODS =====

	// NEW: Taint analysis with custom arrays
	private void runTaintThreeLevelsAnalysisWithArrays(String inputFile, String outputPath, String[] sources, String[] sanitizers, String[] sinks) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting taint analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		loadAnnotationsWithArrays(program, sources, sanitizers, sinks);
		System.out.println("Taint annotations loaded");
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/taint/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Abstract domain: TaintThreeLevels");

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			new ValueEnvironment<TaintThreeLevels>(new TaintThreeLevels()),
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.semanticChecks.add(new TaintThreeLevelsChecker());
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Taint analysis setup finished ---");
	}

	// NEW: Load annotations with custom arrays
	private void loadAnnotationsWithArrays(Program program, String[] sources, String[] sanitizers, String[] sinks) {
		System.out.println("--- Loading taint annotations ---");
		int sourcesFound = 0, sanitizersFound = 0, sinksFound = 0;
		
		for(Unit unit : program.getUnits()) {
			if(unit instanceof ClassUnit) {
				ClassUnit cunit = (ClassUnit) unit;
				for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
					String methodName = cm.getDescriptor().getName();
					
					// Check sources
					for(String source : sources) {
						if(methodName.equals(source)) {
							cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
							sourcesFound++;
							System.out.println("Annotated source: " + methodName);
							break;
						}
					}
					
					// Check sanitizers  
					for(String sanitizer : sanitizers) {
						if(methodName.equals(sanitizer)) {
							cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
							sanitizersFound++;
							System.out.println("Annotated sanitizer: " + methodName);
							break;
						}
					}
					
					// Check sinks
					for(String sink : sinks) {
						if(methodName.equals(sink)) {
							for(Parameter param : cm.getDescriptor().getFormals()) {
								param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
								sinksFound++;
								System.out.println("Annotated sink parameter: " + param.getName() + " in " + methodName);
							}
							break;
						}
					}
				}    
			}
		}
		
		System.out.println("Annotation summary: " + sourcesFound + " sources, " + sanitizersFound + " sanitizers, " + sinksFound + " sink parameters");
		System.out.println("--- Taint annotations loading completed ---");
	}

	// EXISTING: Original helper methods for ValueEnvironment-based domains
	private <V extends ValueDomain<V>> void runOverflowAnalysis(String inputFile, NumericalSize size, String outputPath, V valueEnv) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting overflow analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/overflow/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Target numerical type: " + size.getTypeName() + " (range: " + size.getMin() + " to " + size.getMax() + ")");
		System.out.println("Abstract domain: " + valueEnv.getClass().getSimpleName());

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			valueEnv,
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.semanticChecks.add(new OverflowChecker(size));
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Overflow analysis setup finished ---");
	}

	private <V extends ValueDomain<V>> void runDivisionByZeroAnalysis(String inputFile, NumericalSize size, String outputPath, V valueEnv) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting division by zero analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/divbyzero/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Target numerical type for precision: " + size.getTypeName());
		System.out.println("Abstract domain: " + valueEnv.getClass().getSimpleName());

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			valueEnv,
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.semanticChecks.add(new DivisionByZeroChecker(size));
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Division by zero analysis setup finished ---");
	}

	// EXISTING: Pentagons domain methods
	private void runOverflowAnalysisPentagons(String inputFile, NumericalSize size, String outputPath) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting overflow analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/overflow/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Target numerical type: " + size.getTypeName() + " (range: " + size.getMin() + " to " + size.getMax() + ")");
		System.out.println("Abstract domain: Pentagons (relational domain)");

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			new Pentagons(),
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		System.out.println("Note: Custom overflow checker omitted due to type incompatibility with Pentagons domain");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Overflow analysis setup finished ---");
	}

	private void runDivisionByZeroAnalysisPentagons(String inputFile, NumericalSize size, String outputPath) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting division by zero analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/divbyzero/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Target numerical type for precision: " + size.getTypeName());
		System.out.println("Abstract domain: Pentagons (relational domain)");

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			new Pentagons(),
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		System.out.println("Note: Custom division-by-zero checker omitted due to type incompatibility with Pentagons domain");
		System.out.println("Analysis will rely on built-in LiSA capabilities for Pentagons domain");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Division by zero analysis setup finished ---");
	}

	private void runTaintThreeLevelsAnalysis(String inputFile, String outputPath) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting taint three levels analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		loadAnnotations(program);
		System.out.println("Taint annotations loaded for sources, sanitizers, and sinks");
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/taint/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Abstract domain: TaintThreeLevels (TOP/TAINT/CLEAN/BOTTOM)");

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			new ValueEnvironment<TaintThreeLevels>(new TaintThreeLevels()),
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.semanticChecks.add(new TaintThreeLevelsChecker());
		
		System.out.println("Configuration complete. Starting LiSA analysis...");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("LiSA analysis completed successfully");
		System.out.println("--- Taint three levels analysis setup finished ---");
	}

	private void runCombinedAnalysis(String inputFile, NumericalSize size, String outputPath) 
			throws ParsingException, AnalysisException {
		
		System.out.println("--- Starting combined analysis setup ---");
		System.out.println("Reading program from: " + inputFile);
		
		Program program = IMPFrontend.processFile(inputFile);
		System.out.println("Program parsed successfully. CFGs found: " + program.getAllCFGs().size());
		
		loadAnnotations(program);
		System.out.println("Taint annotations loaded for combined analysis");
		
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "outputs/combined/" + outputPath;
		conf.analysisGraphs = GraphType.HTML;
		conf.jsonOutput = true;
		
		System.out.println("Output directory: " + conf.workdir);
		System.out.println("Target numerical type: " + size.getTypeName());
		System.out.println("Abstract domain: Intervals");
		System.out.println("Checkers: OverflowChecker + DivisionByZeroChecker + TaintChecker");

		conf.abstractState = DefaultConfiguration.simpleState(
			DefaultConfiguration.defaultHeapDomain(),
			new ValueEnvironment<Intervals>(new Intervals()),
			DefaultConfiguration.defaultTypeDomain());
		
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		
		// Add all checkers for comprehensive analysis
		conf.semanticChecks.add(new OverflowChecker(size));
		conf.semanticChecks.add(new DivisionByZeroChecker(size));
		conf.semanticChecks.add(new TaintChecker());
		
		System.out.println("All checkers configured. Starting LiSA analysis...");
		
		LiSA lisa = new LiSA(conf);
		lisa.run(program);
		
		System.out.println("Combined LiSA analysis completed successfully");
		System.out.println("--- Combined analysis setup finished ---");
	}

	private void loadAnnotations(Program program) {
		System.out.println("--- Loading taint annotations ---");
		int sourcesFound = 0, sanitizersFound = 0, sinksFound = 0;
		
		for(Unit unit : program.getUnits()) {
			if(unit instanceof ClassUnit) {
				ClassUnit cunit = (ClassUnit) unit;
				for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
					if(isSource(cm)) {
						cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
						sourcesFound++;
						System.out.println("Annotated source: " + cm.getDescriptor().getName());
					} else if(isSanitizer(cm)) {
						cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
						sanitizersFound++;
						System.out.println("Annotated sanitizer: " + cm.getDescriptor().getName());
					} else if(isSink(cm)) {
						for(Parameter param : cm.getDescriptor().getFormals()) {
							param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
							sinksFound++;
							System.out.println("Annotated sink parameter: " + param.getName() + " in function " + cm.getDescriptor().getName());
						}		
					}
				}	
			}
		}
		
		System.out.println("Annotation summary: " + sourcesFound + " sources, " + sanitizersFound + " sanitizers, " + sinksFound + " sink parameters");
		System.out.println("--- Taint annotations loading completed ---");
	}

	private boolean isSource(CodeMember cm) {
		for(String signatureName : sources) {
			if(cm.getDescriptor().getName().equals(signatureName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSanitizer(CodeMember cm) {
		for(String signatureName : sanitizers) {
			if(cm.getDescriptor().getName().equals(signatureName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSink(CodeMember cm) {
		for(String signatureName : sinks) {
			if(cm.getDescriptor().getName().equals(signatureName)) {
				return true;
			}
		}
		return false;
	}
}