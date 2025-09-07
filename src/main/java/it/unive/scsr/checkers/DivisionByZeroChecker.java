package it.unive.scsr;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.scsr.Intervals;
import it.unive.scsr.NumericalSize; 

public class DivisionByZeroChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {
	
	private NumericalSize size;
	
	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Statement node) {
		
		if(node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		return true;
	}

	private void checkDivision(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Division div) {

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			
			AnalysisState<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());
			
			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
			if(comExprIterator.hasNext()) {
				SymbolicExpression divisor = comExprIterator.next();
				try {
					reachableIds.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);
					
					for (SymbolicExpression s : reachableIds) {
						Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());
					
						// Check if it's a numerical type
						boolean isNumerical = false;
						for (Type type : types) {
							if (isNumericalType(type)) {
								isNumerical = true;
								break;
							}
						}
						
						if (!isNumerical && !types.isEmpty()) {
							// Not a numerical type, skip division by zero check
							continue;
						}
		
						ValueEnvironment<Intervals> valueState = state.getState().getValueState();
						Intervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());
						
						checkDivisionByZero(tool, div, intervalAbstractValue, s);
					}
				} catch (SemanticException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void checkDivisionByZero(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, Division div, Intervals intervals, SymbolicExpression divisorExpr) {
		
		if (intervals.isBottom()) {
			// Bottom state - possibly an error already
			tool.warnOn(div, "Division by unreachable value (bottom state) detected in expression: " + div);
			return;
		}
		
		if (intervals.isTop()) {
			// Top state - unknown value, could be zero
			tool.warnOn(div, "Division by unknown value - potential division by zero in expression: " + div);
			return;
		}
		
		// Check if the interval contains zero
		if (intervals.containsZero()) {
			if (intervals.isZero()) {
				// Definitely zero
				tool.warnOn(div, "Definite division by zero detected in expression: " + div + " (divisor is definitely zero)");
			} else {
				// Possibly zero
				tool.warnOn(div, "Possible division by zero detected in expression: " + div + " (divisor interval " + intervals.representation() + " contains zero)");
			}
		}
		
		// Additional check for very small values that might cause precision issues in floating point
		if (size.isFloatingPoint()) {
			MathNumber low = intervals.interval.getLow();
			MathNumber high = intervals.interval.getHigh();
			
			double epsilon = getEpsilonForFloatType(size);
			
			try {
				double absLow = Math.abs(low.toDouble());
				double absHigh = Math.abs(high.toDouble());
				double minAbs = Math.min(absLow, absHigh);
				
				if (minAbs > 0 && minAbs < epsilon) {
					tool.warnOn(div, "Division by very small " + size.getTypeName() + " value detected in expression: " + div + " (divisor interval " + intervals.representation() + " contains values close to zero, may cause precision issues)");
				}
			} catch (MathNumberConversionException e) {
				throw new RuntimeException("Failed to convert MathNumber to double", e);
			}
		}
	}
	
	private double getEpsilonForFloatType(NumericalSize size) {
		switch (size) {
			case FLOAT8:
				return 1e-2; // Very limited precision for 8-bit float
			case FLOAT16:
				return 1e-4; // Limited precision for 16-bit float
			case FLOAT32:
				return 1e-6; // Standard float precision threshold
			default:
				return 1e-6; // Default threshold
		}
	}
	
	private boolean isNumericalType(Type type) {
		if (type == null) return false;
		String typeName = type.toString().toLowerCase();
		return typeName.contains("int") || typeName.contains("float") || typeName.contains("double") || typeName.contains("number") || typeName.contains("numeric");
	}

	// Compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div, SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state) throws SemanticException {
		
		Set<Type> possibleDynamicTypes = new HashSet<>();
		Type dynamicTypes = state.getDynamicTypeOf(s, div, state);
		if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
			possibleDynamicTypes.add(dynamicTypes);
		} else if(dynamicTypes != null && dynamicTypes.isUntyped()){
			Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
			if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
				for(Type t : runtimeTypes)
					possibleDynamicTypes.add(t);
		}
		
		return possibleDynamicTypes;
	}
}