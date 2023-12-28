package com.etheller.interpreter.ast.execution.instruction;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.expression.ArithmeticJassExpression;
import com.etheller.interpreter.ast.expression.ArrayRefJassExpression;
import com.etheller.interpreter.ast.expression.FunctionCallJassExpression;
import com.etheller.interpreter.ast.expression.FunctionReferenceJassExpression;
import com.etheller.interpreter.ast.expression.JassExpression;
import com.etheller.interpreter.ast.expression.JassExpressionVisitor;
import com.etheller.interpreter.ast.expression.LiteralJassExpression;
import com.etheller.interpreter.ast.expression.NegateJassExpression;
import com.etheller.interpreter.ast.expression.NotJassExpression;
import com.etheller.interpreter.ast.expression.ReferenceJassExpression;
import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.statement.JassArrayedAssignmentStatement;
import com.etheller.interpreter.ast.statement.JassCallStatement;
import com.etheller.interpreter.ast.statement.JassDoNothingStatement;
import com.etheller.interpreter.ast.statement.JassExitWhenStatement;
import com.etheller.interpreter.ast.statement.JassIfElseIfStatement;
import com.etheller.interpreter.ast.statement.JassIfElseStatement;
import com.etheller.interpreter.ast.statement.JassIfStatement;
import com.etheller.interpreter.ast.statement.JassLocalDefinitionStatement;
import com.etheller.interpreter.ast.statement.JassLocalStatement;
import com.etheller.interpreter.ast.statement.JassLoopStatement;
import com.etheller.interpreter.ast.statement.JassReturnNothingStatement;
import com.etheller.interpreter.ast.statement.JassReturnStatement;
import com.etheller.interpreter.ast.statement.JassSetStatement;
import com.etheller.interpreter.ast.statement.JassStatement;
import com.etheller.interpreter.ast.statement.JassStatementVisitor;
import com.etheller.interpreter.ast.value.ArrayJassType;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.visitor.ArrayTypeVisitor;

public class InstructionAppendingJassStatementVisitor
		implements JassStatementVisitor<Void>, JassExpressionVisitor<Void> {
	private final List<JassInstruction> instructions;
	private final GlobalScope globalScope;
	private final Map<String, Integer> nameToLocalId = new HashMap<>();
	private int nextLocalId;
	private final ArrayDeque<Integer> loopStartInstructionPtrs = new ArrayDeque<>();

	public InstructionAppendingJassStatementVisitor(final List<JassInstruction> instructions,
			final GlobalScope globalScope, final int startingLocalId) {
		this.instructions = instructions;
		this.globalScope = globalScope;
		this.nextLocalId = startingLocalId;
	}

	public int getLocalId(final String name) {
		final Integer localId = this.nameToLocalId.get(name);
		if (localId == null) {
			return -1;
		}
		return localId;
	}

	@Override
	public Void visit(final JassArrayedAssignmentStatement statement) {
		insertExpressionInstructions(statement.getIndexExpression());
		insertExpressionInstructions(statement.getExpression());
		final String identifier = statement.getIdentifier();
		final int localId = getLocalId(identifier);
		if (localId != -1) {
			this.instructions.add(new LocalArrayAssignmentInstruction(localId));
		}
		final int globalId = this.globalScope.getGlobalId(identifier);
		if (globalId != -1) {
			this.instructions.add(new GlobalArrayAssignmentInstruction(globalId));
		}
		return null;
	}

	@Override
	public Void visit(final JassCallStatement statement) {
		final String functionName = statement.getFunctionName();
		final List<JassExpression> arguments = statement.getArguments();
		insertFunctionCallInstructions(functionName, arguments);
		return null;
	}

	@Override
	public Void visit(final JassDoNothingStatement statement) {
		this.instructions.add(new DoNothingInstruction());
		return null;
	}

	@Override
	public Void visit(final JassExitWhenStatement statement) {
		insertExpressionInstructions(statement.getExpression());
		this.instructions.add(new ConditionalBranchInstruction(this.loopStartInstructionPtrs.peek()));
		return null;
	}

	@Override
	public Void visit(final JassIfElseIfStatement statement) {
		insertExpressionInstructions(statement.getCondition());
		final int branchInstructionPtr = this.instructions.size();
		this.instructions.add(null);
		for (final JassStatement thenStatement : statement.getThenStatements()) {
			thenStatement.accept(this);
		}
		final int branchEndInstructionPtr = this.instructions.size();
		this.instructions.add(null);
		final int elseStatementsStart = this.instructions.size();
		this.instructions.set(branchInstructionPtr, new InvertedConditionalBranchInstruction(elseStatementsStart));
		statement.getElseifTail().accept(this);
		this.instructions.set(branchEndInstructionPtr, new BranchInstruction(this.instructions.size()));
		return null;
	}

	@Override
	public Void visit(final JassIfElseStatement statement) {
		insertExpressionInstructions(statement.getCondition());
		final int branchInstructionPtr = this.instructions.size();
		this.instructions.add(null);
		for (final JassStatement thenStatement : statement.getThenStatements()) {
			thenStatement.accept(this);
		}
		final int branchEndInstructionPtr = this.instructions.size();
		this.instructions.add(null);
		final int elseStatementsStart = this.instructions.size();
		this.instructions.set(branchInstructionPtr, new InvertedConditionalBranchInstruction(elseStatementsStart));
		for (final JassStatement thenStatement : statement.getElseStatements()) {
			thenStatement.accept(this);
		}
		this.instructions.set(branchEndInstructionPtr, new BranchInstruction(this.instructions.size()));
		return null;
	}

	@Override
	public Void visit(final JassIfStatement statement) {
		insertExpressionInstructions(statement.getCondition());
		final int branchInstructionPtr = this.instructions.size();
		this.instructions.add(null);
		for (final JassStatement thenStatement : statement.getThenStatements()) {
			thenStatement.accept(this);
		}
		this.instructions.set(branchInstructionPtr, new InvertedConditionalBranchInstruction(this.instructions.size()));
		return null;
	}

	@Override
	public Void visit(final JassLocalDefinitionStatement statement) {
		final String identifier = statement.getIdentifier();
		this.nameToLocalId.put(identifier, this.nextLocalId++);
		insertExpressionInstructions(statement.getExpression());
		return null;
	}

	@Override
	public Void visit(final JassLocalStatement statement) {
		final String identifier = statement.getIdentifier();
		final JassType type = statement.getType();
		this.nameToLocalId.put(identifier, this.nextLocalId++);
		final ArrayJassType arrayType = type.visit(ArrayTypeVisitor.getInstance());
		if (arrayType != null) {
			this.instructions.add(new DeclareLocalArrayInstruction(arrayType));
		}
		else {
			this.instructions.add(new PushLiteralInstruction(type.getNullValue()));
		}
		return null;
	}

	@Override
	public Void visit(final JassLoopStatement statement) {
		this.loopStartInstructionPtrs.push(this.instructions.size());
		for (final JassStatement loopBodySubStatement : statement.getStatements()) {
			loopBodySubStatement.accept(this);
		}
		this.loopStartInstructionPtrs.pop();
		return null;
	}

	@Override
	public Void visit(final JassReturnNothingStatement statement) {
		this.instructions.add(new PushLiteralInstruction(JassReturnNothingStatement.RETURN_NOTHING_NOTICE));
		this.instructions.add(new ReturnInstruction());
		return null;
	}

	@Override
	public Void visit(final JassReturnStatement statement) {
		this.instructions.add(new ReturnInstruction());
		return null;
	}

	@Override
	public Void visit(final JassSetStatement statement) {
		final String identifier = statement.getIdentifier();
		insertExpressionInstructions(statement.getExpression());
		final int localId = getLocalId(identifier);
		if (localId != -1) {
			this.instructions.add(new LocalAssignmentInstruction(localId));
		}
		final int globalId = this.globalScope.getGlobalId(identifier);
		if (globalId != -1) {
			this.instructions.add(new GlobalAssignmentInstruction(globalId));
		}
		return null;
	}

	// Expressions

	private void insertExpressionInstructions(final JassExpression expression) {
		expression.accept(this);
	}

	private void insertReferenceExpressionInstructions(final String identifier) {
		final int localId = getLocalId(identifier);
		if (localId != -1) {
			this.instructions.add(new LocalReferenceInstruction(localId));
		}
		final int globalId = this.globalScope.getGlobalId(identifier);
		if (globalId != -1) {
			this.instructions.add(new GlobalReferenceInstruction(globalId));
		}
		throw new IllegalArgumentException("No such identifier: " + identifier);
	}

	@Override
	public Void visit(final ArithmeticJassExpression expression) {
		insertExpressionInstructions(expression.getLeftExpression());
		insertExpressionInstructions(expression.getRightExpression());
		this.instructions.add(new ArithmeticInstruction(expression.getArithmeticSign()));
		return null;
	}

	@Override
	public Void visit(final ArrayRefJassExpression expression) {
		insertReferenceExpressionInstructions(expression.getIdentifier());
		insertExpressionInstructions(expression.getIndexExpression());
		this.instructions.add(new ArrayReferenceInstruction());
		return null;
	}

	@Override
	public Void visit(final FunctionCallJassExpression expression) {
		final String functionName = expression.getFunctionName();
		final List<JassExpression> arguments = expression.getArguments();
		insertFunctionCallInstructions(functionName, arguments);
		return null;
	}

	public void insertFunctionCallInstructions(final String functionName, final List<JassExpression> arguments) {
		this.instructions.add(new NewStackFrameInstruction());
		for (int i = 0; i < arguments.size(); i++) {
			insertExpressionInstructions(arguments.get(i));
		}
		this.instructions.add(new SetReturnAddrInstruction(this.instructions.size()));
		final Integer userFunctionInstructionPtr = this.globalScope.getUserFunctionInstructionPtr(functionName);
		if (userFunctionInstructionPtr != null) {
			this.instructions.add(new BranchInstruction(userFunctionInstructionPtr.intValue()));
		}
		else {
			final Integer nativeId = this.globalScope.getNativeId(functionName);
			if (nativeId != null) {
				this.instructions.add(new NativeInstruction(nativeId, arguments.size()));
			}
			else {
				throw new JassException(this.globalScope, "Undefined function: " + functionName,
						new RuntimeException());
			}
		}
	}

	@Override
	public Void visit(final FunctionReferenceJassExpression expression) {
		final String identifier = expression.getIdentifier();
		final JassFunction functionByName = this.globalScope.getFunctionByName(identifier);
		if (functionByName == null) {
			throw new RuntimeException("Unable to find function: " + identifier);
		}
		this.instructions.add(new PushLiteralInstruction(new CodeJassValue(functionByName)));
		return null;
	}

	@Override
	public Void visit(final LiteralJassExpression expression) {
		this.instructions.add(new PushLiteralInstruction(expression.getValue()));
		return null;
	}

	@Override
	public Void visit(final NegateJassExpression expression) {
		this.instructions.add(new NegateInstruction());
		return null;
	}

	@Override
	public Void visit(final NotJassExpression expression) {
		this.instructions.add(new NotInstruction());
		return null;
	}

	@Override
	public Void visit(final ReferenceJassExpression expression) {
		final String identifier = expression.getIdentifier();
		insertReferenceExpressionInstructions(identifier);
		return null;
	}
}
