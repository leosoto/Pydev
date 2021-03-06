/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;
import com.python.pydev.codecompletion.ui.CodeCompletionPreferencesPage;

/**
 * Provides the completions in a context-insensitive way for classes and methods (both for the editor or the console).
 *
 * @author Fabio
 */
public class CtxParticipant implements IPyDevCompletionParticipant, IPyDevCompletionParticipant2{
    
    
    // Console completions ---------------------------------------------------------------------------------------------
    
    /**
     * IPyDevCompletionParticipant2
     */
    public Collection<ICompletionProposal> computeConsoleCompletions(ActivationTokenAndQual tokenAndQual,
            List<IPythonNature> naturesUsed, IScriptConsoleViewer viewer, int requestOffset) {
        List<ICompletionProposal> completions = new ArrayList<ICompletionProposal>();
        if(tokenAndQual.activationToken != null && tokenAndQual.activationToken.length() > 0){
            //we only want 
            return completions;
        }
        
        String qual = tokenAndQual.qualifier;
        if(qual.length() >= CodeCompletionPreferencesPage.getCharsForContextInsensitiveGlobalTokensCompletion() && naturesUsed.size() > 0){ //at least n characters required...
            boolean addAutoImport = AutoImportsPreferencesPage.doAutoImport();
            int qlen = qual.length();
            String lowerQual = qual.toLowerCase();
            
        
            for(IPythonNature nature:naturesUsed){
                fillNatureCompletionsForConsole(viewer, requestOffset, completions, qual, addAutoImport, qlen, lowerQual,
                    nature, false);
            }
    
            //and at last, get from the system
            fillNatureCompletionsForConsole(viewer, requestOffset, completions, qual, addAutoImport, qlen, lowerQual,
                    naturesUsed.get(0), true);
        }        
        return completions;

    }

    private void fillNatureCompletionsForConsole(IScriptConsoleViewer viewer, int requestOffset,
            List<ICompletionProposal> completions, String qual, boolean addAutoImport, int qlen, String lowerQual,
            IPythonNature nature, boolean getSystem) {
        AbstractAdditionalInterpreterInfo additionalInfoForProject;
        
        if(getSystem){
            try {
                additionalInfoForProject = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                        PydevPlugin.getInterpreterManager(nature), nature.getProjectInterpreter().getExecutableOrJar());
            }catch(PythonNatureWithoutProjectException e){
                throw new RuntimeException(e);
            } catch (MisconfigurationException e) {
                throw new RuntimeException(e);
            }
        }else{
            additionalInfoForProject = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
        }
        
        List<IInfo> tokensStartingWith = additionalInfoForProject.getTokensStartingWith(
                qual, AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        
        FastStringBuffer realImportRep = new FastStringBuffer();
        FastStringBuffer displayString = new FastStringBuffer();
        FastStringBuffer tempBuf = new FastStringBuffer();
        for (IInfo info : tokensStartingWith) {
            //there always must be a declaringModuleName
            String declaringModuleName = info.getDeclaringModuleName();
            boolean hasInit = false;
            if(declaringModuleName.endsWith(".__init__")){
                declaringModuleName = declaringModuleName.substring(0, declaringModuleName.length()-9);//remove the .__init__
                hasInit = true;
            }
            
            String rep = info.getName();
            String lowerRep = rep.toLowerCase();
            if(!lowerRep.startsWith(lowerQual)){
                continue;
            }
            
            if(addAutoImport){
                realImportRep.clear();
                realImportRep.append("from ");
                realImportRep.append(AutoImportsPreferencesPage.removeImportsStartingWithUnderIfNeeded(declaringModuleName, tempBuf));
                realImportRep.append(" import ");
                realImportRep.append(rep);
            }
            
            displayString.clear();
            displayString.append(rep );
            displayString.append(" - ");
            displayString.append(declaringModuleName);
            if(hasInit){
                displayString.append(".__init__");
            }

            PyConsoleCompletion  proposal = new PyConsoleCompletion(
                    rep,
                    requestOffset - qlen, 
                    qlen, 
                    realImportRep.length(), 
                    AnalysisPlugin.getImageForAutoImportTypeInfo(info), 
                    displayString.toString(), 
                    (IContextInformation)null, 
                    "", 
                    lowerRep.equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_1 : IPyCompletionProposal.PRIORITY_GLOBALS,
                    realImportRep.toString(), 
                    viewer);
            
            completions.add(proposal);
        }
    }
    
    
    
    
    // Editor completions ----------------------------------------------------------------------------------------------

    
    private Collection<CtxInsensitiveImportComplProposal> getThem(CompletionRequest request, ICompletionState state, 
            boolean addAutoImport) throws MisconfigurationException {
        
        ArrayList<CtxInsensitiveImportComplProposal> completions = new ArrayList<CtxInsensitiveImportComplProposal>();
        if(request.isInCalltip){
            return completions;
        }
        
        HashSet<String> importedNames = getImportedNames(state);
        
        String qual = request.qualifier;
        if(qual.length() >= CodeCompletionPreferencesPage.getCharsForContextInsensitiveGlobalTokensCompletion()){ //at least n characters required...
            String lowerQual = qual.toLowerCase();
            
            String initialModule = request.resolveModule();
        
            List<IInfo> tokensStartingWith = AdditionalProjectInterpreterInfo.getTokensStartingWith(qual, request.nature, 
                    AbstractAdditionalInterpreterInfo.TOP_LEVEL);
            
            FastStringBuffer realImportRep = new FastStringBuffer();
            FastStringBuffer displayString = new FastStringBuffer();
            FastStringBuffer tempBuf = new FastStringBuffer();
            
            for (IInfo info : tokensStartingWith) {
                //there always must be a declaringModuleName
                String declaringModuleName = info.getDeclaringModuleName();
                if(initialModule != null && declaringModuleName != null){
                    if(initialModule.equals(declaringModuleName)){
                        continue;
                    }
                }
                boolean hasInit = false;
                if(declaringModuleName.endsWith(".__init__")){
                    declaringModuleName = declaringModuleName.substring(0, declaringModuleName.length()-9);//remove the .__init__
                    hasInit = true;
                }
                
                String rep = info.getName();
                String lowerRep = rep.toLowerCase();
                if(!lowerRep.startsWith(lowerQual) || importedNames.contains(rep)){
                    continue;
                }
                
                if(addAutoImport){
                    realImportRep.clear();
                    realImportRep.append("from ");
                    realImportRep.append(AutoImportsPreferencesPage.removeImportsStartingWithUnderIfNeeded(declaringModuleName, tempBuf));
                    realImportRep.append(" import ");
                    realImportRep.append(rep);
                }
                
                displayString.clear();
                displayString.append(rep );
                displayString.append(" - ");
                displayString.append(declaringModuleName);
                if(hasInit){
                    displayString.append(".__init__");
                }

                CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                        rep,
                        request.documentOffset - request.qlen, 
                        request.qlen, 
                        realImportRep.length(), 
                        AnalysisPlugin.getImageForAutoImportTypeInfo(info), 
                        displayString.toString(), 
                        (IContextInformation)null, 
                        "", 
                        lowerRep.equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_1 : IPyCompletionProposal.PRIORITY_GLOBALS,
                        realImportRep.toString());
                
                completions.add(proposal);
            }
    
        }        
        return completions;
    }

    /**
     * @return the names that are already imported in the current document
     */
    private HashSet<String> getImportedNames(ICompletionState state) {
        List<IToken> tokenImportedModules = state.getTokenImportedModules();
        HashSet<String> importedNames = new HashSet<String>();
        if(tokenImportedModules != null){
            for (IToken token : tokenImportedModules) {
                importedNames.add(token.getRepresentation());
            }
        }
        return importedNames;
    }
    
    @SuppressWarnings("unchecked")
    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state) throws MisconfigurationException {
        return getThem(request, state, AutoImportsPreferencesPage.doAutoImport());
    }

    /**
     * IPyDevCompletionParticipant
     */
    @SuppressWarnings("unchecked")
    public Collection getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope, Collection<IToken> interfaceForLocal) {
        ArrayList<IToken> ret = new ArrayList<IToken>();
        String qual = state.getQualifier();
        if(qual.length() >= CodeCompletionPreferencesPage.getCharsForContextInsensitiveGlobalTokensCompletion()){ //at least n characters
            
            List<IInfo> tokensStartingWith = AdditionalProjectInterpreterInfo.getTokensStartingWith(qual, state.getNature(), AbstractAdditionalInterpreterInfo.INNER);
            for (IInfo info : tokensStartingWith) {
                ret.add(new SourceToken(null, info.getName(), null, null, info.getDeclaringModuleName(), info.getType()));
            }
            
        }
        return ret;
    }

    /**
     * IPyDevCompletionParticipant
     * @throws MisconfigurationException 
     */
    @SuppressWarnings("unchecked")
    public Collection getStringGlobalCompletions(CompletionRequest request, ICompletionState state) throws MisconfigurationException {
        return getThem(request, state, false);
    }

    public Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal){
        throw new RuntimeException("Deprecated");
    }

    @SuppressWarnings("unchecked")
    public Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal){
        return getCompletionsForMethodParameter(state, localScope, interfaceForLocal);
    }


}
