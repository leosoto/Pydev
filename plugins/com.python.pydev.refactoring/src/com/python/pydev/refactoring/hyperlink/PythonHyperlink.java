package com.python.pydev.refactoring.hyperlink;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.refactoring.actions.PyGoToDefinition;

/**
 * Hiperlink will try to open the current selected word (and will give a beep if not found).
 *
 * @author Fabio
 */
public class PythonHyperlink implements IHyperlink {

    private final IRegion fRegion;
    private PyEdit fEditor;
    
    public PythonHyperlink(IRegion region, PyEdit editor) {
        Assert.isNotNull(region);
        fRegion= region;
        fEditor = editor;
    }

    public IRegion getHyperlinkRegion() {
        return fRegion;
    }

    public String getHyperlinkText() {
        return null;
    }

    public String getTypeLabel() {
        return null;
    }

    /**
     * Try to find a definition and open it.
     */
    public void open() {
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        
        //saves the dirty editors so that hyperlink is correct.
        IWorkbench workbench = PlatformUI.getWorkbench();
        if(workbench != null){
            IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
            if(workbenchWindow != null){
                workbenchWindow.getActivePage().saveAllEditors(false);
            }
        }
        
        try{
            RefactoringRequest refactoringRequest = PyRefactorAction.createRefactoringRequest(null, this.fEditor, new PySelection(this.fEditor));
            ItemPointer[] pointers = pyRefactoring.findDefinition(refactoringRequest);
            
            if (pointers.length > 0){
                PyGoToDefinition.openDefinition(pointers, fEditor, fEditor.getSite().getShell());
            }else{
                workbench.getActiveWorkbenchWindow().getShell().getDisplay().beep();
            }
        }catch(Throwable t){
            PydevPlugin.log(t);
        }
    }

}
