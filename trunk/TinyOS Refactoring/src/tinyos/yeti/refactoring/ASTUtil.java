package tinyos.yeti.refactoring;

import java.util.Collection;
import java.util.LinkedList;

import tinyos.yeti.nesc12.ep.NesC12AST;
import tinyos.yeti.nesc12.parser.ast.nodes.ASTNode;
import tinyos.yeti.nesc12.parser.ast.nodes.general.Identifier;
import tinyos.yeti.preprocessor.PreprocessorReader;

/**
 * Conglomeration of Methods that are useful while working whit the AST in a Refactoring Plugin
 */
public class ASTUtil {
	
	private NesC12AST ast;
	private PreprocessorReader reader;
	public ASTUtil(NesC12AST ast){
		this.ast=ast;
		reader=ast.getReader();
	}
	/**
	 * Method returns the AST-Leaf that relates to the Position specified in pos in the not preprocessed input file. 
	 * @param pos Position in the original Input File
	 * @return The AST Leaf that covers this Position, or null if the Position is not covered by a leaf.
	 */
	public  ASTNode getASTLeafAtPos(int pos){
		ASTNode root = ast.getRoot();
		
		boolean foundChild=true;
		while(root.getChildrenCount() > 0 && foundChild){
			foundChild=false;
			for(int i=0; i < root.getChildrenCount()&& !foundChild; i++){
				ASTNode child = root.getChild(i);
				
				// It happend to us that we got null values
				if(child!=null){
					if(end(child) >= pos){
						foundChild=true;
						root=root.getChild(i);
					}
				}
			}	
		}
		
		if(foundChild){
			//System.err.println("Pos: "+pos+" root.range:"+ast.getOffsetAtBegin(root).getInputfileOffset()+"-"+ast.getOffsetAtEnd(root).getInputfileOffset()+" root:"+root);
			return root;
		} else {
			// Happens for example if the Cursor is at a blank position
			return null;
		}
	}
	
	/**
	 * Returns the next higher AST node of type
	 * @param child The AST node to start from
	 * @param type The type of the ASTNode we are looking for
	 * @return
	 */
	public static ASTNode getParentForName(ASTNode child,Class<? extends ASTNode> type){
		ASTNode parent=child.getParent();
		if(parent==null){
			return null;
		}
		if(parent.getClass().equals(type)){
			return parent;
		}
		return getParentForName(parent, type);
	}
	
	/**
	 * 
	 * @param root ASTNode which child's are checked for being Identifier with name indentifierName 
	 * @param identifierName Name of the Identifier you are looking for
	 * @return A list with all occurrences of Identifiers below the root parameter in the AST
	 */
	public static Collection<Identifier> getIncludedIdentifiers(ASTNode root, String identifierName){
		LinkedList<Identifier> ret = new LinkedList<Identifier>();
		getIncludedIdentifiers_sub(root, identifierName, ret);
		return ret;
	}
	
	private static void getIncludedIdentifiers_sub(ASTNode root,String identifierName,Collection<Identifier> result){
		ASTNode child=null;
		Identifier identifier=null;
		for(int i=0;i<root.getChildrenCount();++i){
			child=root.getChild(i);
			if(child!=null){
				if(child instanceof Identifier){
					identifier=(Identifier)child;
					if(identifier.getName().equals(identifierName)){
						result.add((Identifier)child);
					}
				} else {
					getIncludedIdentifiers_sub(child, identifierName, result);
				}
			}
		}
	}
	
	/**
	 * Returns the Begin-Offset of node in the not preprocessed input file
	 * @param node The node you wan't to know the offset of.
	 * @return	Well, the offset.
	 */
	public int start(ASTNode node){
		return reader.inputLocation(ast.getOffsetAtBegin(node).getPreprocessedOffset(), true);
	}

	/**
	 * As start, just the end.
	 */
	public int end(ASTNode node){
		return reader.inputLocation(ast.getOffsetAtEnd(node).getPreprocessedOffset(), true);
	}

}