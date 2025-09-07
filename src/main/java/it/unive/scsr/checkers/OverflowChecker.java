package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Set;

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
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.scsr.Intervals;
import it.unive.scsr.NumericalSize;

public class OverflowChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {
	private NumericalSize size;
	
	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Statement node) {
		
		if (node instanceof Assignment) {
			Assignment assignment = (Assignment) node;
			Expression leftExpression = assignment.getLeft();
			
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}
		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}
		
		return true;
	}
	
	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node) {
		
		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;
		
		// Check if we need numerical type verification
		boolean hasNumericalType = false;
		if (!staticType.isUntyped()) {
			hasNumericalType = isNumericalType(staticType);
		} else {
			// Check dynamic types
			for (Type dynType : dynamicTypes) {
				if (isNumericalType(dynType)) {
					hasNumericalType = true;
					break;
				}
			}
		}
		
		if (!hasNumericalType) {
			// Not a numerical type, skip overflow check
			return;
		}

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);	
			
			checkOverflowUnderflow(tool, varRef, intervalAbstractValue);
		}
	}
	
	private void checkOverflowUnderflow(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, Intervals intervals) {
		
		if (intervals.isBottom()) {
			// Bottom state - possibly an error already
			return;
		}
		
		if (intervals.isTop()) {
			// Top state - could potentially overflow/underflow
			tool.warnOn(varRef, "Variable '" + varRef.getName() + "' has unknown value - potential " + size.getTypeName() + " overflow/underflow");
			return;
		}
		
		MathNumber low = intervals.interval.getLow();
		MathNumber high = intervals.interval.getHigh();
		
		try {
		    if (!high.isPlusInfinity() && high.toDouble() > size.getMax()) {
		        double wrappedValue = wrapAroundValue(high.toDouble(), size);
		        if (size.isFloatingPoint()) {
		            tool.warnOn(varRef, "Potential " + size.getTypeName() + " overflow detected: value " + high + " exceeds maximum " + size.getMax() + " for variable '" + varRef.getName() + "'");
		        } else {
		            tool.warnOn(varRef, "Potential " + size.getTypeName() + " overflow detected: value " + high + " exceeds maximum " + size.getMax() + " (wraps to " + wrappedValue + ") for variable '" + varRef.getName() + "'");
		        }
		    }
		    
		    if (!low.isMinusInfinity() && low.toDouble() < size.getMin()) {
		        double wrappedValue = wrapAroundValue(low.toDouble(), size);
		        if (size.isFloatingPoint()) {
		            tool.warnOn(varRef, "Potential " + size.getTypeName() + " underflow detected: value " + low + " below minimum " + size.getMin() + " for variable '" + varRef.getName() + "'");
		        } else {
		            tool.warnOn(varRef, "Potential " + size.getTypeName() + " underflow detected: value " + low + " below minimum " + size.getMin() + " (wraps to " + wrappedValue + ") for variable '" + varRef.getName() + "'");
		        }
		    }
		    
		    if (!low.isMinusInfinity() && !high.isPlusInfinity() &&
		        low.toDouble() <= size.getMax() && high.toDouble() >= size.getMin() &&
		        (low.toDouble() < size.getMin() || high.toDouble() > size.getMax())) {
		        tool.warnOn(varRef, "Variable '" + varRef.getName() + "' has interval [" + low + ", " + high + "] that may exceed " + size.getTypeName() + " bounds [" + size.getMin() + ", " + size.getMax() + "]");
		    }
		} catch (MathNumberConversionException e) {
		    // Handle the exception as appropriate for your application
		    // For example, you can log and return, or throw a runtime exception
		    throw new RuntimeException("Failed to convert MathNumber to double", e);
		}
	}
	
	private double wrapAroundValue(double value, NumericalSize size) {
		if (size.isFloatingPoint()) {
			return value; // Floating point doesn't wrap around
		}
		
		double range = size.getMax() - size.getMin() + 1;
		
		if (value > size.getMax()) {
			// Overflow: wrap around from max to min
			double excess = value - size.getMax();
			return size.getMin() + ((excess - 1) % range);
		} else if (value < size.getMin()) {
			// Underflow: wrap around from min to max
			double deficit = size.getMin() - value;
			return size.getMax() - ((deficit - 1) % range);
		}
		
		return value; // No wrapping needed
	}
	
	private boolean isNumericalType(Type type) {
		String typeName = type.toString().toLowerCase();
		return typeName.contains("int") || typeName.contains("float") || typeName.contains("double") || typeName.contains("number") || typeName.contains("numeric");
	}

	private Set<Type> getPossibleDynamicTypes(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Statement node, Variable id, VariableRef varRef) {
		
		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
			try {
				Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
				if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
					possibleDynamicTypes.add(dynamicTypes);
				} else if(dynamicTypes != null && dynamicTypes.isUntyped()){
					Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
					if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
						for(Type t : runtimeTypes)
							possibleDynamicTypes.add(t);
				}
			} catch (SemanticException e) {
				System.err.println("Cannot check " + node);
				e.printStackTrace(System.err);
			}
		}	
		return possibleDynamicTypes;
	}
}