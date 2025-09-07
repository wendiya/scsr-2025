package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class TaintThreeLevels extends BaseTaint<TaintThreeLevels> {

	/*
	 * Lattice of Taint Domain with three levels
	 * 
	 * 	  TOP
	 * 	/  	   \
	 * TAINT	CLEAN
	 *  \      /
	 *   BOTTOM
	 *   
	 * Element meanings:
	 * - TOP: might be tainted or clean (unknown)
	 * - TAINT: definitely tainted
	 * - CLEAN: definitely clean
	 * - BOTTOM: error state
	 */
	
	private static final TaintThreeLevels TOP = new TaintThreeLevels(State.TOP);
	private static final TaintThreeLevels TAINT = new TaintThreeLevels(State.TAINT);
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels(State.CLEAN);
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels(State.BOTTOM);
	
	private enum State {
		TOP, TAINT, CLEAN, BOTTOM
	}
	
	private final State state;
	
	public TaintThreeLevels() {
		this(State.TOP);
	}
	
	public TaintThreeLevels(State state) {
		this.state = state;
	}
	
	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		if (this.state == State.BOTTOM) return other;
		if (other.state == State.BOTTOM) return this;
		
		if (this.state == State.TOP || other.state == State.TOP) return TOP;
		
		if (this.state == other.state) return this;
		
		// TAINT ⊔ CLEAN = TOP
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (this.state == State.BOTTOM) return true;
		if (other.state == State.TOP) return true;
		if (this.state == other.state) return true;
		
		return false;
	}

	@Override
	public TaintThreeLevels top() {
		return TOP;
	}

	@Override
	public TaintThreeLevels bottom() {
		return BOTTOM;
	}

	@Override
	protected TaintThreeLevels tainted() {
		return TAINT;
	}

	@Override
	protected TaintThreeLevels clean() {
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
		return this.state == State.TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this.state == State.TAINT || this.state == State.TOP;
	}
	
	@Override
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		
		// If any operand is bottom, result is bottom
		if (left.state == State.BOTTOM || right.state == State.BOTTOM) {
			return BOTTOM;
		}
		
		// If any operand is tainted, result is tainted
		if (left.state == State.TAINT || right.state == State.TAINT) {
			return TAINT;
		}
		
		// If any operand is top (unknown), result is top
		if (left.state == State.TOP || right.state == State.TOP) {
			return TOP;
		}
		
		// Both operands are clean, result is clean
		if (left.state == State.CLEAN && right.state == State.CLEAN) {
			return CLEAN;
		}
		
		// Default case (should not reach here)
		return TOP;
	}
	
	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		// For taint analysis, widening is typically the same as lub
		return lubAux(other);
	}

	@Override
	public StructuredRepresentation representation() {
		switch (state) {
			case BOTTOM:
				return Lattice.bottomRepresentation();
			case TOP:
				return Lattice.topRepresentation();
			case CLEAN:
				return new StringRepresentation("_");
			case TAINT:
				return new StringRepresentation("#");
			default:
				return new StringRepresentation("?");
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		TaintThreeLevels that = (TaintThreeLevels) obj;
		return state == that.state;
	}
	
	@Override
	public int hashCode() {
		return state.hashCode();
	}
	
	@Override
	public String toString() {
		return representation().toString();
	}
	
	@Override
	public boolean isTop() {
		return state == State.TOP;
	}
	
	@Override
	public boolean isBottom() {
		return state == State.BOTTOM;
	}
}