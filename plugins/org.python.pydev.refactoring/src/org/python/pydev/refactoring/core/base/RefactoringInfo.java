/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.base;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IFileEditorInput;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

public class RefactoringInfo {
    private IFile sourceFile;
    private IDocument doc;
    private ITextSelection userSelection;
    private ITextSelection extendedSelection;
    private ModuleAdapter moduleAdapter;
    private final IPythonNature nature;
    private final IGrammarVersionProvider versionProvider;
    private PythonModuleManager moduleManager;
    private AbstractScopeNode<?> scopeAdapter;
    private IProject project;
    private LinkedList<String> sysPath;
    private File realFile;

    public RefactoringInfo(PyEdit edit) throws MisconfigurationException {
        IFileEditorInput editorInput = (IFileEditorInput) edit.getEditorInput();
        this.sourceFile = editorInput.getFile();

        this.realFile = sourceFile != null ? sourceFile.getLocation().toFile() : null;

        this.doc = edit.getDocumentProvider().getDocument(editorInput);
        ITextSelection selection = (ITextSelection) edit.getSelectionProvider().getSelection();

        this.project = edit.getProject();
        this.nature = edit.getPythonNature();
        versionProvider = this.nature;
        initInfo(selection);

    }

    public RefactoringInfo(IDocument document, ITextSelection selection, IGrammarVersionProvider versionProvider) {
        this.sourceFile = null;
        this.nature = null;
        this.versionProvider = versionProvider;
        this.doc = document;

        initInfo(selection);
    }

    private void initInfo(ITextSelection selection) {
        this.sysPath = new LinkedList<String>();

        if(this.nature != null){
            this.moduleManager = new PythonModuleManager(nature);
        }

        try{
            this.moduleAdapter = VisitorFactory.createModuleAdapter(moduleManager, realFile, doc, nature);
        }catch(Throwable e){
            throw new RuntimeException(e);
        }

        this.extendedSelection = null;
        this.userSelection = moduleAdapter.normalizeSelection(selection);
    }

    public IProject getProject() {
        return project;
    }

    public ModuleAdapter getModuleAdapter() {
        return moduleAdapter;
    }

    public List<IClassDefAdapter> getClasses() {
        return moduleAdapter.getClasses();
    }

    public IFile getSourceFile() {
        return this.sourceFile;
    }

    public IDocument getDocument() {
        return this.doc;
    }

    public ITextSelection getExtendedSelection() {
        if(this.extendedSelection == null){
            this.extendedSelection = new TextSelection(this.doc, this.userSelection.getOffset(), this.userSelection.getLength());

            if(getScopeAdapter() != null){
                this.extendedSelection = moduleAdapter.normalizeSelection(VisitorFactory.createSelectionExtension(getScopeAdapter(), this.extendedSelection));
            }

        }
        return extendedSelection;
    }

    public ITextSelection getUserSelection() {
        return userSelection;
    }

    public ModuleAdapter getParsedExtendedSelection() {
        String source = normalizeSourceSelection(getExtendedSelection());

        if(source.length() > 0){
            try{
                return VisitorFactory.createModuleAdapter(moduleManager, null, new Document(source), nature);
            }catch(TokenMgrError e){
                return null;
            }catch(ParseException e){
                /* Parse Exception means the current selection is invalid, discard and return null */
                return null;
            }catch(Throwable e){
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public ModuleAdapter getParsedUserSelection() {
        ModuleAdapter parsedAdapter = null;
        String source = normalizeSourceSelection(this.userSelection);

        if(this.userSelection != null && source.length() > 0){
            try{
                parsedAdapter = VisitorFactory.createModuleAdapter(moduleManager, null, new Document(source), nature);
            }catch(TokenMgrError e){
                return null;
            }catch(ParseException e){
                /* Parse Exception means the current selection is invalid, discard and return null */
                return null;
            }catch(Throwable e){
                throw new RuntimeException(e);
            }

        }
        return parsedAdapter;
    }

    public String normalizeSourceSelection(ITextSelection selection) {
        String selectedText = "";

        if(selection.getText() != null){
            selectedText = selection.getText().trim();
        }
        if(selectedText.length() == 0){
            return "";
        }

        try{
            return normalizeBlockIndentation(selection, selectedText);
        }catch(Throwable e){
            /* TODO: uncommented empty exception catch all */
        }
        return selectedText;

    }

    private String normalizeBlockIndentation(ITextSelection selection, String selectedText) throws Throwable {
        String[] lines = selectedText.split("\\n");
        if(lines.length < 2){
            return selectedText;
        }

        String firstLine = doc.get(doc.getLineOffset(selection.getStartLine()), doc.getLineLength(selection.getStartLine()));
        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(doc);

        String indentation = "";
        int bodyIndent = 0;
        while(firstLine.startsWith(" ")){
            indentation += " ";
            firstLine = firstLine.substring(1);
            bodyIndent += 1;
        }

        if(bodyIndent > 0){
            StringBuffer selectedCode = new StringBuffer();
            for(String line:lines){
                if(line.startsWith(indentation)){
                    selectedCode.append(line.substring(bodyIndent) + lineDelimiter);
                }else{
                    selectedCode.append(line + lineDelimiter);
                }

            }
            selectedText = selectedCode.toString();
        }
        return selectedText;
    }

    public IClassDefAdapter getScopeClass() {
        return moduleAdapter.getScopeClass(getUserSelection());
    }

    public IPythonNature getNature() {
        return nature;
    }

    public List<IClassDefAdapter> getScopeClassAndBases() throws MisconfigurationException {
        return moduleAdapter.getClassHierarchy(getScopeClass());
    }

    public AbstractScopeNode<?> getScopeAdapter() {
        if(scopeAdapter == null){
            scopeAdapter = moduleAdapter.getScopeAdapter(userSelection);
        }
        return scopeAdapter;
    }

    public boolean isSelectionExtensionRequired() {
        return !(this.getUserSelection().getOffset() == this.getExtendedSelection().getOffset() && this.getUserSelection().getLength() == this.getExtendedSelection().getLength());
    }

    public String getNewLineDelim() {
        return TextUtilities.getDefaultLineDelimiter(this.doc);
    }

    public AdapterPrefs getAdapterPrefs() {
        return new AdapterPrefs(getNewLineDelim(), versionProvider);
    }

//    public Workspace getWorkspace() {
//        /* create or get the workspace */
//
//        if(workspace == null){
//            workspace = createWorkspace();
//        }
//
//        return workspace;
//    }
//
//    private Workspace createWorkspace() {
//        LinkedList<String> srcPath = new LinkedList<String>();
//
//        Set<String> paths;
//        try{
//            paths = nature.getPythonPathNature().getProjectSourcePathSet(true);
//        }catch(CoreException e){
//            throw new RuntimeException(e);
//        }
//        for(String path:paths){
//            IFolder folder = project.getParent().getFolder(new Path(path));
//            /* get the source folder's path relative to the project */
//            String relativePath = folder.getProjectRelativePath().toString();
//            srcPath.add(relativePath);
//        }
//
//        return new Workspace(project.getLocation().makeAbsolute().toFile(), srcPath, sysPath);
//    }
//
//    public Module getModule() {
//        File file = this.realFile;
//        Workspace workspace = this.getWorkspace();
//
//        Module module = workspace.getModule(file);
//        return module;
//    }
//
//    /**
//     * Returns the NameUse of the currently selected variable
//     * 
//     * @return the currently selected nameUse
//     */
//    public Use findSelectedUse() {
//        List<Use> uses = getModule().getContainedUses();
//
//        int selectionOffset = userSelection.getOffset();
//
//        for(Use use:uses){
//            NameAdapter name = use.getName();
//            int nodeLength = name.getId().length();
//            int nodeOffsetBegin = NodeUtils.getOffset(this.doc, name.getNode());
//
//            int nodeOffsetEnd = nodeOffsetBegin + nodeLength;
//
//            if(selectionOffset >= nodeOffsetBegin && selectionOffset <= nodeOffsetEnd){
//                return use;
//            }
//        }
//
//        return null;
//    }

//    public PythonTypeInferencer getTypeInferencer() {
//        if(inferencer == null){
//            inferencer = new PythonTypeInferencer();
//        }
//        return inferencer;
//    }
//
//    public IFile getFileForModule(IModule module) {
//        if(project != null){
//            String relativePath = module.getRelativePath();
//            IPath path = new Path(relativePath);
//            IFile file = project.getFile(path);
//            return file;
//        }else{
//            return null;
//        }
//    }
}
