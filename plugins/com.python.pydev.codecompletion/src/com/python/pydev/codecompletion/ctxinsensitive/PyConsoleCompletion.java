package com.python.pydev.codecompletion.ctxinsensitive;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;

/**
 * Extends the basic completion proposal to add a line with an import in the console.
 *
 * @author Fabio
 */
public class PyConsoleCompletion  extends CtxInsensitiveImportComplProposal{

    /**
     * 
     * Offset containing the start of the editable line in the document
     */
    private int commandLineOffset;
    
    /**
     * This attribute is only filled during the apply method with the number of chars from the
     * end of the document to the offset where the completion was requested
     */
    private int deltaInLine;
    
    private int diff;
    
    public PyConsoleCompletion(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, String realImportRep, IScriptConsoleViewer viewer) {
        
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, realImportRep);
        commandLineOffset = viewer.getCommandLineOffset();
    }
    
    /**
     * Applies the completion to the document and also updates the caret offset.
     */
    @Override
    public void apply(IDocument document, char trigger, int stateMask, int offset) {
        try {
            this.diff = offset - (fReplacementOffset+fReplacementLength);

            deltaInLine = document.getLength()-(fReplacementOffset+fReplacementLength);
            
            String currentLineContents = document.get(commandLineOffset, document.getLength()-commandLineOffset);
            
            StringBuffer buf = new StringBuffer(currentLineContents);
            int startReplace = currentLineContents.length()-deltaInLine-fReplacementLength;
            int endReplace = currentLineContents.length()-deltaInLine+diff;
            
            String newCurrentLineString = buf.replace(startReplace, endReplace, fReplacementString).toString();
            
            //clear the current line contents
            document.replace(commandLineOffset, document.getLength()-commandLineOffset, "");
            
            boolean addImport = realImportRep.length() > 0;
            String delimiter = PyAction.getDelimiter(document);
            
            //now, add the import if that should be done...
            
            if(addImport){
                //add the import and the contents of the current line
                document.replace(commandLineOffset, 0, realImportRep+delimiter+newCurrentLineString);
            }else{
                //just add the completion contents without doing the import
                document.replace(document.getLength(), 0, newCurrentLineString);
            }
        } catch (BadLocationException x) {
            PydevPlugin.log(x);
        }
    } 
    
    
    @Override
    public Point getSelection(IDocument document) {
        return new Point(document.getLength()-deltaInLine+diff, 0);
    }

}
