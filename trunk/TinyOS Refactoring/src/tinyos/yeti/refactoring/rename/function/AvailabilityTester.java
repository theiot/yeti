package tinyos.yeti.refactoring.rename.function;

import org.eclipse.jface.text.ITextSelection;

import tinyos.yeti.editors.NesCEditor;
import tinyos.yeti.nesc12.ep.NesC12AST;
import tinyos.yeti.nesc12.parser.ast.nodes.general.Identifier;
import tinyos.yeti.refactoring.utilities.ASTUtil;
import tinyos.yeti.refactoring.utilities.ASTUtil4Functions;
import tinyos.yeti.refactoring.utilities.ActionHandlerUtil;

public class AvailabilityTester implements tinyos.yeti.refactoring.AvailabilityTester.IRefactoringAvailabilityTester {

	@Override
	public boolean test(ITextSelection receiver) {
		NesCEditor editor = ActionHandlerUtil.getActiveEditor().getNesCEditor();
		NesC12AST ast = (NesC12AST) editor.getAST();
		ASTUtil util = new ASTUtil(ast);
		ITextSelection selection= ActionHandlerUtil.getSelection(editor);
		Identifier identifier = util.getASTLeafAtPos(selection.getOffset(),selection.getLength(), Identifier.class);
		return 	ASTUtil4Functions.isFunction(identifier);
	}
	


}