package it.unive.scsr;

import it.unive.lisa.LiSA;
import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Program;
import it.unive.scsr.EnhancedDivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import it.unive.scsr.NumericalSize;
import it.unive.lisa.checks.semantic.SemanticCheck;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class ComprehensiveStudentProgramsTestSuite {

    private static final String STUDENT_PROGRAMS_DIR = "student-programs";
    private static final String[] OVERFLOW_DIRS = { "overflow" };
    private static final String[] DIVZERO_DIRS = { "divbyzero" };
    private static final String[] TAINT_DIRS = { "taint" };

    private static final NumericalSize OVERFLOW_TYPE = NumericalSize.UINT8;
    private static final NumericalSize DIVZERO_TYPE = NumericalSize.INT32;

    @Test
    public void testStudentProgramsOverflow() throws Exception {
        runCheckerOnStudentPrograms("Overflow", new OverflowChecker(OVERFLOW_TYPE), OVERFLOW_DIRS);
    }

    @Test
    public void testStudentProgramsDivByZero() throws Exception {
        runCheckerOnStudentPrograms("DivisionByZero", new EnhancedDivisionByZeroChecker(DIVZERO_TYPE), DIVZERO_DIRS);
    }

    @Test
    public void testStudentProgramsTaintThreeLevels() throws Exception {
        runCheckerOnStudentPrograms("TaintThreeLevels", new TaintThreeLevelsChecker(), TAINT_DIRS);
    }

    private void runCheckerOnStudentPrograms(String checkerName, SemanticCheck<?> checker, String[] subfolders) throws ParsingException, AnalysisException {
        for (String sub : subfolders) {
            File dir = new File(STUDENT_PROGRAMS_DIR, sub);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".imp"));
            if (files == null || files.length == 0) {
                System.out.println("No IMP files found in " + dir.getPath());
                continue;
            }

            Arrays.sort(files);
            for (File file : files) {
                System.out.println("=== Running " + checkerName + " on: " + file.getName() + " ===");
                Program program = IMPFrontend.processFile(file.getAbsolutePath());

                LiSAConfiguration conf = new DefaultConfiguration();
                conf.workdir = "outputs/student-programs/" + checkerName.toLowerCase() + "/" + file.getName().replace(".imp", "");
                conf.analysisGraphs = GraphType.NONE;
                conf.jsonOutput = false;
                conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
                conf.semanticChecks.add(checker);

                LiSA lisa = new LiSA(conf);
                lisa.run(program);

                System.out.println("Analysis completed for: " + file.getName());
            }
        }
    }
}