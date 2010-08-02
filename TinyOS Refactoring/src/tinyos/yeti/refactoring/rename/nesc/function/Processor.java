package tinyos.yeti.refactoring.rename.nesc.function;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;

import tinyos.yeti.ep.parser.IASTModelPath;
import tinyos.yeti.ep.parser.IDeclaration;
import tinyos.yeti.nesc12.parser.ast.nodes.general.Identifier;
import tinyos.yeti.refactoring.rename.RenameInfo;
import tinyos.yeti.refactoring.rename.RenameProcessor;

public class Processor extends RenameProcessor {

	boolean selectionisInterfaceAliasInNesCComponentWiring=false;
	private tinyos.yeti.refactoring.rename.alias.Processor aliasProcessor;
	
	private IDeclaration interfaceDeclaration;
	
	private RenameInfo info;

	public Processor(RenameInfo info) {
		super(info);
		this.info = info;
	}
	
	/**
	 * Returns a list which doesnt contain aliases which have a different name then the interface to be refactored and therefore dont have to be touched.
	 * This is needed since aliases in event/command definitions also reference the original interface.
	 * @param identifiers
	 */
	private List<Identifier> getAliasFreeList(List<Identifier> identifiers,String interfaceNameToChange) {
		List<Identifier> result=new LinkedList<Identifier>();
		for(Identifier identifier:identifiers){
			if(interfaceNameToChange.equals(identifier.getName())){
				result.add(identifier);
			}
		}
		return result;
	}

	@Override
	public Change createChange(IProgressMonitor pm) 
	throws CoreException,OperationCanceledException {
		//If the selection was identified as interface alias, the alias processor is used.
		//This alias is handled by the interface processor, since we have no process monitor when we are deciding the processor type.
		//Without process monitor we are unable to get an ast and without ast or at least a node we cant create a AstAnalyzer => we cannot decide if it is an interface or actually an alias.
		if(selectionisInterfaceAliasInNesCComponentWiring){	
			return aliasProcessor.createChange(pm);
		}
		CompositeChange ret = new CompositeChange("Rename Interface "+ info.getOldName() + " to " + info.getNewName());
		try {
			//Add Change for interface definition
			IFile declaringFile=getIFile4ParseFile(interfaceDeclaration.getParseFile());
			Identifier declaringIdentifier=getIdentifierForPath(interfaceDeclaration.getPath(), pm);
			List<Identifier> identifiers=new LinkedList<Identifier>();
			identifiers.add(declaringIdentifier);
			addMultiTextEdit(identifiers, getAst(declaringFile, pm), declaringFile, createTextChangeName("interface", declaringFile), ret);
			
			//Add Changes for referencing elements
			Collection<IASTModelPath> paths=new LinkedList<IASTModelPath>();
			paths.add(interfaceDeclaration.getPath());
			for(IFile file:getAllFiles()){
				identifiers=getReferencingIdentifiersInFileForTargetPaths(file, paths, pm);
				identifiers=getAliasFreeList(identifiers,declaringIdentifier.getName());
				addMultiTextEdit(identifiers, getAst(file, pm), file, createTextChangeName("interface", file), ret);
			}
			
			//Adds the change for renaming the file which contains the definition.
			RenameResourceChange resourceChange=new RenameResourceChange(declaringFile.getFullPath(), info.getNewName()+".nc");
			ret.add(resourceChange);
			
		} catch (Exception e){
			e.printStackTrace();
		}
		return ret;
	}

}
