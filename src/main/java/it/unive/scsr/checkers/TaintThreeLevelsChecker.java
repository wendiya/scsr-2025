package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.StringUtilities;
import it.unive.scsr.TaintThreeLevels;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.annotations.Annotation;

public class TaintThreeLevelsChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> {
	
	// define the annotation we use to mark a sink in the program
	public static final Annotation SINK_ANNOTATION = new Annotation("lisa.taint.Sink");

	// create a matcher that helps us find all elements annotated as sinks
	public static final AnnotationMatcher SINK_MATCHER = new BasicAnnotationMatcher(SINK_ANNOTATION);

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		// only interested in unresolved function calls, skip other statements
		if (!(node instanceof UnresolvedCall))
			return true; 
		
		// cast the node to a call so we can work with it
		UnresolvedCall call = (UnresolvedCall) node;
		try {
			// get the analysis results for this function’s CFG
			for (AnalyzedCFG<
					SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(call.getCFG())) {
				
				// try to get the resolved version of the call using the analysis results
				Call resolved = tool.getResolvedVersion(call, result);
				if (resolved == null)
					System.err.println("Error"); // couldn't resolve the call, something went wrong

				// if the resolved call is a CFG call, check its targets
				if (resolved instanceof CFGCall) {
					CFGCall cfg = (CFGCall) resolved;
					for (CodeMember n : cfg.getTargets()) {
						Parameter[] parameters = n.getDescriptor().getFormals();
						// check each parameter of the target function
						for (int i = 0; i < parameters.length; i++)
							// if the parameter is a sink, check if tainted data flows into it
							if (parameters[i].getAnnotations().contains(SINK_MATCHER)) {
								AnalysisState<
										SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>,
												TypeEnvironment<InferredTypes>>> state = result
														.getAnalysisStateAfter(call.getParameters()[i]);
								
								// collect all expressions that are reachable from the parameter
								Set<SymbolicExpression> reachableIds = new HashSet<>();
								for (SymbolicExpression e : state.getComputedExpressions())
									reachableIds
											.addAll(state.getState().reachableFrom(e, node, state.getState()).elements);

								// now check each expression to see if it is tainted
								for (SymbolicExpression s : reachableIds) {
									ValueEnvironment<TaintThreeLevels> valueState = state.getState().getValueState();

									// if the value is definitely tainted, warn as definite
									if(valueState.eval((ValueExpression) s, node, state.getState()).isAlwaysTainted())
										tool.warnOn(call, "[DEFINITE] The value passed for the " + StringUtilities.ordinal(i + 1)
										+ " parameter of this call is always tainted, and it reaches the sink at parameter '"
										+ parameters[i].getName() + "' of " + resolved.getFullTargetName());
									// if the value might be tainted, warn as possible
									else if (valueState.eval((ValueExpression) s, node, state.getState())
											.isPossiblyTainted())
										tool.warnOn(call, "[POSSIBLE] The value passed for the " + StringUtilities.ordinal(i + 1)
												+ " parameter of this call may be tainted, and it reaches the sink at parameter '"
												+ parameters[i].getName() + "' of " + resolved.getFullTargetName());
								}
							}

					}
				} 
			}
		} catch (SemanticException e) {
			// if something goes wrong during the checking, print it out
			System.err.println("Cannot check " + node);
			e.printStackTrace(System.err);
		}

		// always return true because we don’t want to stop the analysis
		return true;
	}

}
