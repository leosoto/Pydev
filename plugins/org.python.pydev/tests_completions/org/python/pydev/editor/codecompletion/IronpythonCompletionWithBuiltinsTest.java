package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.IronpythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.IronpythonShell;
import org.python.pydev.editor.codecompletion.shell.PythonShellTest;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class IronpythonCompletionWithBuiltinsTest extends CodeCompletionTestsBase{
    
    
    protected boolean isInTestFindDefinition = false;
    
    public static void main(String[] args) {
        try {
            IronpythonCompletionWithBuiltinsTest builtins = new IronpythonCompletionWithBuiltinsTest();
            builtins.setUp();
            builtins.testSortParamsCorrect();
            builtins.tearDown();
            
            junit.textui.TestRunner.run(IronpythonCompletionWithBuiltinsTest.class);

            System.out.println("Finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getIronpythonInterpreterManager();
    }
    
    @Override
    protected void setInterpreterManager() {
        PydevPlugin.setIronpythonInterpreterManager(new IronpythonInterpreterManagerStub(this.getPreferences()));
    }
    
    
    @Override
    protected PythonNature createNature() {
        return new PythonNature(){
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
            }
            @Override
            public int getGrammarVersion() {
                return IPythonNature.LATEST_GRAMMAR_VERSION;
            }
            
            @Override
            public String resolveModule(File file) throws MisconfigurationException {
                if(isInTestFindDefinition){
                    return null;
                }
                return super.resolveModule(file);
            }
        };
    }

    private static IronpythonShell shell;
    
    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.IRONPYTHON_LIB, false);
        
        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if(shell == null){
            shell = PythonShellTest.startIronpythonShell(nature);
        }
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, shell);
    
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, null);
    }
    
    public void testRecursion() throws FileNotFoundException, Exception, CompletionRecursionException{
        String file = TestDependent.TEST_PYSRC_LOC+"testrec3/rec.py";
        String strDoc = "RuntimeError.";
        File f = new File(file);
        try{
            nature.getAstManager().getCompletionsForToken(f, new Document(REF.getFileContents(f)), 
                    CompletionStateFactory.getEmptyCompletionState("RuntimeError", nature, new CompletionCache()));
        }catch(CompletionRecursionException e){
            //that's ok... we're asking for it here...
        }
        requestCompl(f, strDoc, strDoc.length(), -1, new String[]{"__doc__", "__getitem__()", "__init__()", "__str__()"});   
    }
    

    
    public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception{
        
        String s;
        
        s = "from datetime import datetime\n" +
        "datetime.";
        
        //for some reason, this is failing only when the module is specified...
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/pysrc/simpledatetimeimport.py");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        requestCompl(file, s, s.length(), -1, new String[]{"today()", "now()", "utcnow()"});

        
        
        s = "from datetime import datetime, date, MINYEAR,";
        requestCompl(s, s.length(), -1, new String[] { "date", "datetime", "MINYEAR", "MAXYEAR", "timedelta" });
        
        s = "from datetime.datetime import ";
        requestCompl(s, s.length(), -1, new String[] { "today", "now", "utcnow" });

    
    
        // Problem here is that we do not evaluate correctly if
        // met( ddd,
        //      fff,
        //      ccc )
        //so, for now the test just checks that we do not get in any sort of
        //look... 
        s = "" +
        
        "class bla(object):pass\n" +
        "\n"+
        "def newFunc(): \n"+
        "    callSomething( bla.__get#complete here... stack error \n"+
        "                  keepGoing) \n";

        //If we improve the parser to get the error above, uncomment line below to check it...
        requestCompl(s, s.indexOf('#'), 1, new String[]{"__getattribute__(object self, str name)"});


        //check for builtins..1
        s = "" +
        "\n" +
        "";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

        //check for builtins..2
        s = "" +
        "from testlib import *\n" +
        "\n" +
        "";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

        //check for builtins..3 (builtins should not be available because it is an import request for completions)
        requestCompl("from testlib.unittest import  ", new String[]{"__file__", "__name__", "__init__", "__path__", "anothertest"
                , "AnotherTest", "GUITest", "guitestcase", "main", "relative", "t", "TestCase", "testcase", "TestCaseAlias", 
                });

    }

    
    
    public void testBuiltinsInNamespace() throws BadLocationException, IOException, Exception{
        String s = "__builtins__.";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError", "list"});
        
        s = "__builtins__.list.";
        requestCompl(s, s.length(), -1, new String[]{"sort(object cmp, object key, bool reverse)"});
    }
    
    public void testBuiltinsInNamespace2() throws BadLocationException, IOException, Exception{
        String s = "__builtins__.RuntimeError.";
        requestCompl(s, s.length(), -1, new String[]{"__doc__", "__getitem__()", "__init__()", "__str__()"});
    }
    
    public void testDeepNested6() throws Exception{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.c1.f.";
        requestCompl(s, s.length(), -1, new String[] { "curdir"});
    }
    
    public void testDeepNested10() throws Exception{
        String s;
        s = "" +
        "from extendable.nested3 import hub2\n"+
        "hub2.c.a.";
        requestCompl(s, s.length(), -1, new String[] { "fun()"});
    }
    
    public void testRelativeOnSameProj() throws Exception{
        String s;
        s = "" +
        "import prefersrc\n" +
        "prefersrc.";
        AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "foo";
        try {
            requestCompl(s, s.length(), -1, new String[] { "OkGotHere" }, nature2);
        } finally {
            AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";
        }
    }
    
    public void testDeepNested7() throws Exception{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.c1.f.curdir.";
        requestCompl(s, s.length(), -1, new String[] { "upper()"});
    }
    
    public void testDeepNested8() throws Exception{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.C1.f.sep."; //changed: was altsep (may be None in linux).
        requestCompl(s, s.length(), -1, new String[] { "upper()"});
    }
    
    public void testDeepNested9() throws Exception{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.C1.f.inexistant.";
        requestCompl(s, s.length(), -1, new String[] { });
    }
    
    public void testDictAssign() throws Exception{
        String s;
        s = "" +
        "a = {}\n"+
        "a.";
        requestCompl(s, s.length(), -1, new String[] { "keys()" });
    }
    

    public void testPreferSrc() throws BadLocationException, IOException, Exception{
        String s = ""+
        "import prefersrc\n"+
        "prefersrc.";
        requestCompl(s, s.length(), -1, new String[]{"PreferSrc"});
    }
    
    public void testCompleteImportBuiltinReference() throws BadLocationException, IOException, Exception{
        
        String s;

        s = "" +
        "import os\n"+
        "                \n"+   
        "os.";         
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/pysrc/simpleosimport.py");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        requestCompl(file, s, s.length(), -1, new String[]{"path"});
        
        s = "" +
        "import os\n"+
        "                \n"+   
        "os.";         
        requestCompl(s, s.length(), -1, new String[]{"path"});

        //check for builtins with reference..3
        s = "" +
        "from testlib.unittest import anothertest\n"+
        "anothertest.";         
        requestCompl(s, s.length(), 4, new String[]{"__file__", "__name__", "AnotherTest","t"});

    }
    

    public void testInstanceCompletion() throws Exception {
        String s = 
            "class A:\n" +
            "    def __init__(self):\n" +
            "        self.list1 = []\n" +
            "if __name__ == '__main__':\n" +
            "    a = A()\n" +
            "    a.list1.";
        
        requestCompl(s, -1, new String[] {"pop(int index)", "remove(object value)"});
    }
    
    
    public void test__all__() throws Exception {
        String s = 
            "from extendable.all_check import *\n" +
            "";
        
        //should keep the variables from the __builtins__ in this module
        requestCompl(s, -1, new String[] {"ThisGoes", "RuntimeError"});
    }
    
    
    public void testSortParamsCorrect() throws Exception {
        String s = 
            "[].sort" +
            "";
        
        //should keep the variables from the __builtins__ in this module
        ICompletionProposal[] requestCompl = requestCompl(s, -1, new String[] {"sort(object cmp, object key, bool reverse)"});
        assertEquals(1, requestCompl.length);
        IDocument doc = new Document(s);
        requestCompl[0].apply(doc);
        assertEquals("[].sort(cmp, key, reverse)", doc.get());
    }

    
    public void testFindDefinition() throws Exception {
        isInTestFindDefinition = true;
        try {
            CompiledModule mod = new CompiledModule("os", nature.getAstManager());
            Definition[] findDefinition = mod.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState("walk", nature, new CompletionCache()), -1, -1, nature);
            assertEquals(1, findDefinition.length);
            assertEquals("os", findDefinition[0].module.getName());
        } finally {
            isInTestFindDefinition = false;
        }
    }
    
    
    
}
