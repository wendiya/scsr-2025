package it.unive.scsr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.TaintThreeLevels;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.util.file.FileManager;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;

public class TaintThreeLevelsTaskEvaluation {
    
    // here I define which functions I consider sources, sanitizers, and sinks
	String[] sources = new String[] {"source1", "source2"};
	String[] sanitizers = new String[] {"sanitizer1", "sanitizer2"};
	String[] sinks = new String[] {"sink1", "sinks"};
	

	@Test
	public void testTaintThreeLevels() throws ParsingException, AnalysisException {
		// parse the input file to create the internal representation of the program
		Program program = IMPFrontend.processFile("inputs/taint-3lvs-eval.imp");

		// I mark the sources, sanitizers, and sinks in the program so LiSA knows about them
		loadAnnotations(program);
		
		// creating a configuration object to tell LiSA how to run
		LiSAConfiguration conf = new DefaultConfiguration();

		// set the folder where LiSA will store output files
		conf.workdir = "outputs/taint-3lvs-eval";

		// choose HTML for visual graphs of the analysis
		conf.analysisGraphs = GraphType.HTML;
		
		// enable JSON output to store warnings
		conf.jsonOutput= true;

		// setup the abstract state for the taint analysis
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new TaintThreeLevels()),
				DefaultConfiguration.defaultTypeDomain());
		 
		 // use interprocedural analysis so that function calls are handled correctly
		 conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		 
		 // add the TaintChecker to catch flows of tainted data into sinks
		 conf.semanticChecks.add(new TaintThreeLevelsChecker());
		 
		conf.serializeResults = true;
		conf.jsonOutput = true;
		
		// try to clean the working directory before running
		try {
			FileManager.forceDeleteFolder(conf.workdir);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Cannot delete working directory '" + conf.workdir + "': " + e.getMessage());
		}
		 
		// create a LiSA instance with the configuration we just set
		LiSA lisa = new LiSA(conf);
		
		// actually run the analysis on the program
		lisa.run(program);
		
		// prepare paths to compare expected and actual output
		Path expectedPath = Paths.get("expected", "taint-3lvs-eval");
		Path actualPath = Paths.get("outputs", "taint-3lvs-eval");

		File expFile = Paths.get(expectedPath.toString(), "report.json").toFile();
		File actFile = Paths.get(actualPath.toString(), "report.json").toFile();
		try {
			// read both JSON reports
			JsonReport expected = JsonReport.read(new FileReader(expFile));
			JsonReport actual = JsonReport.read(new FileReader(actFile));
			// check if the analysis results match what we expect
			assertTrue("Results are different",
					JsonReportComparer.compare(expected, actual, expectedPath.toFile(), actualPath.toFile()));
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			fail("Unable to find report file");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Unable to compare reports");
		}
	}


	private void loadAnnotations(Program program) {
		// go through all classes and their code members
		for(Unit unit : program.getUnits()) {
			if(unit instanceof ClassUnit) {
				ClassUnit cunit = (ClassUnit) unit;
				for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
					// if it's a source, add the tainted annotation
					if(isSource(cm))
						cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
					// if it's a sanitizer, mark it as clean
					else if(isSanitizer(cm)) 
						cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
					// if it's a sink, mark its parameters so LiSA knows data flows there
					else if(isSink(cm))
						for(Parameter param : cm.getDescriptor().getFormals()) {
							param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
						}		
				}	
			}
		}
	}


	private boolean isSource(CodeMember cm) {
		// check if the member's name is in the sources list
		for(String signatureName : sources) {
			if(cm.getDescriptor().getName().equals(signatureName))
				return true;
		}
		return false;
	}
	

	private boolean isSanitizer(CodeMember cm) {
		// check if the member's name is in the sanitizers list
		for(String signatureName : sanitizers) {
			if(cm.getDescriptor().getName().equals(signatureName))
				return true;
		}
		return false;
	}
	

	private boolean isSink(CodeMember cm) {
		// check if the member's name is in the sinks list
		for(String signatureName : sinks) {
			if(cm.getDescriptor().getName().equals(signatureName))
				return true;
		}
		return false;
	}
	
}
