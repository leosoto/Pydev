/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IModule;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * This tests the 'whole' code completion, passing through all modules.
 * 
 * @author Fabio Zadrozny
 */
public class PythonCompletionWithoutBuiltinsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        
      try {
          //DEBUG_TESTS_BASE = true;
          PythonCompletionWithoutBuiltinsTest test = new PythonCompletionWithoutBuiltinsTest();
	      test.setUp();
          test.testProj2Global();
	      test.tearDown();
          System.out.println("Finished");

          junit.textui.TestRunner.run(PythonCompletionWithoutBuiltinsTest.class);
	  } catch (Exception e) {
	      e.printStackTrace();
	  } catch(Error e){
	      e.printStackTrace();
	  }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }


    
	public void testCompleteImportCompletion() throws CoreException, BadLocationException{
	    requestCompl("import testl"                        , "testlib");
	    requestCompl("from testl"                          , "testlib");
	    requestCompl("from testlib import "                , new String[]{"__init__", "unittest"});
	    requestCompl("from testlib import unittest, __in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest,__in"   , new String[]{"__init__"});
	    requestCompl("from testlib import unittest ,__in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , __in" , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , "     , new String[]{"__init__", "unittest"});
	    
	    requestCompl("from testlib.unittest import  ", getTestLibUnittestTokens());

	    requestCompl("from testlib.unittest.testcase.TestCase import  assertImagesNotE", new String[]{"assertImagesNotEqual"});
	    requestCompl("from testlib.unittest.testcase.TestCase import  assertBM", new String[]{"assertBMPsNotEqual","assertBMPsEqual"});
    }

    /**
     * @return
     */
    private String[] getTestLibUnittestTokens() {
        return new String[]{
          "__init__"
        , "anothertest"
        , "AnotherTest"
        , "GUITest"
        , "guitestcase"
        , "main"
        , "relative"
        , "t"
        , "TestCase"
        , "testcase"
        , "TestCaseAlias"
        };
    }

	
	public void testSelfReference() throws CoreException, BadLocationException{
        String s;
        s = "class C:            \n" +
			"    def met1(self): \n" +
			"        pass        \n" +
			"                    \n" +
			"class B:            \n" +
			"    def met2(self): \n" +
			"        self.c = C()\n" +
			"                    \n" +
			"    def met3(self): \n" +
			"        self.c.";
        requestCompl(s, s.length(), -1, new String[] { "met1()"});
	}
	
	public void testProj2() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"import proj2root\n" +
		"print proj2root.";
		requestCompl(s, s.length(), -1, new String[] { "Proj2Root"}, nature2);
	}
	
	public void testProj2Global() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"import ";
		requestCompl(s, s.length(), -1, new String[] { "proj2root", "testlib"}, nature2);
	}
	
	public void testInnerImport() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
        "def m1():\n" +
        "    from testlib import unittest\n" +
        "    unittest.";
	    requestCompl(s, s.length(), -1, new String[]{
            "AnotherTest"
            , "GUITest"
            , "main"
            , "TestCase"
            , "testcase"
            , "TestCaseAlias"
            
            //gotten because unittest is actually an __init__, so, gather others that are in the same level
            , "anothertest"
            , "guitestcase"
            , "testcase"
            });
	}
	
	
	public void testSelfReferenceWithTabs() throws CoreException, BadLocationException{
	    String s;
	    s = "class C:\n" +
	    "    def met1(self):\n" +
	    "        pass\n" +
	    "        \n" +
	    "class B:\n" +
	    "    def met2(self):\n" +
	    "        self.c = C()\n" +
	    "        \n" +
	    "    def met3(self):\n" +
	    "        self.c.";
        s = s.replaceAll("\\ \\ \\ \\ ", "\t");
	    requestCompl(s, s.length(), -1, new String[] { "met1()"});
	}

	
	public void testClassCompl() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "class Test:\n" +
        "    classVar = 1\n"+
	    "    def findIt(self):\n"+
	    "        self.";
	    requestCompl(s, s.length(), -1, new String[] { "classVar"});
	}
	
	public void testInnerCtxt() throws CoreException, BadLocationException{
		String s;
		s = "" +
		"class Test:\n"+
		"    def findIt(self):\n"+
		"        pass\n"+
		"    \n"+
		"def m1():\n"+
		"    s = Test()\n"+
		"    s.";
		requestCompl(s, s.length(), -1, new String[] { "findIt()"});
	}
	
	
	public void testDeepNested() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
    	    "from extendable.nested2 import hub\n"+
    	    "hub.c1.a.";
	    requestCompl(s, s.length(), -1, new String[] { "fun()"});
	}
	
	public void testDeepNested2() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.b.";
	    requestCompl(s, s.length(), -1, new String[] { "another()"});
	}
	
	public void testDeepNested3() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.c.";
	    requestCompl(s, s.length(), -1, new String[] { "another()"});
	}
	
	public void testDeepNested4() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.d.";
	    requestCompl(s, s.length(), -1, new String[] { "AnotherTest"});
	}
	
	public void testDeepNested5() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.e.";
	    requestCompl(s, s.length(), -1, new String[] { "assertBMPsNotEqual"});
	}
	
	public void testDeepNested6() throws CoreException, BadLocationException{
		String s;
		s = "" +
		"from extendable.nested2 import mod2\n"+
		"mod2.c1.a.";
		requestCompl(s, s.length(), -1, new String[] { "fun()"});
	}
	
	
	public void testSelfReferenceWithTabs2() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
        "class C:\n" +
        "    def met3(self):\n" +
        "        self.COMPLETE_HERE\n" +
        "                    \n" +
	    "    def met1(self): \n" +
	    "        pass        \n" +
        "";
	    s = s.replaceAll("\\ \\ \\ \\ ", "\t");
        int iComp = s.indexOf("COMPLETE_HERE");
        s = s.replaceAll("COMPLETE_HERE", "");
	    requestCompl(s, iComp, -1, new String[] { "met1()"});
	}
	
	public void testRelativeImport() throws FileNotFoundException, CoreException, BadLocationException{
        String file = TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/testrelative.py";
        String strDoc = "from toimport import ";
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[]{"Test1", "Test2"});   
    }

	public void testWildImportRecursive() throws BadLocationException, IOException, Exception{
        String s;
        s = "from testrecwild import *\n" +
            "";
        requestCompl(s, -1, -1, new String[] { "Class1"});
	}
	
	public void testWildImportRecursive2() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "from testrecwild2 import *\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "Class2"});
	}
	
	public void testWildImportRecursive3() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "from testrec2 import *\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "Leaf"});
	}
	
	public void testProperties() throws BadLocationException, IOException, Exception{
		String s;
		s = 
		"class C:\n" +
		"    \n" +
		"    properties.create(test = 0)\n" +
		"    \n" +
		"c = C.";
		requestCompl(s, -1, -1, new String[] { "test"});
	}
	
	public void testImportMultipleFromImport() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "import testlib.unittest.relative\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "testlib","testlib.unittest","testlib.unittest.relative"});
    }
	
	public void testImportMultipleFromImport2() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "import testlib.unittest.relative\n" +
	    "testlib.";
	    requestCompl(s, -1, 0, new String[] { });
	}
	
	
	public void testNestedImports() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import nested\n"+ 
		"print nested.NestedClass.";   
		requestCompl(s, -1, 1, new String[] { "nestedMethod()" });
	}
	
	
	public void testSameName() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable.namecheck import samename\n"+ 
		"print samename.";   
		requestCompl(s, -1, 1, new String[] { "method1()" });
	}
	
	
	public void testSameName2() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import namecheck\n"+ 
		"print namecheck.samename.";   
		requestCompl(s, -1, 1, new String[] { "method1()" });
	}
	
	public void testCompositeImport() throws BadLocationException, IOException, Exception{
		String s;
		s = "import xml.sax\n"+ 
		"print xml.sax.";   
		requestCompl(s, -1, -1, new String[] { "default_parser_list" });
	}
	
	public void testIsInGlobalTokens() throws BadLocationException, IOException, Exception{
		IModule module = nature.getAstManager().getModule("testAssist.__init__", nature, true);
		assertTrue(module.isInGlobalTokens("assist.ExistingClass.existingMethod", nature));
	}
	
	
	
    public void testGetActTok(){
        String strs[];
        
        strs = PySelection.getActivationTokenAndQual(new Document(""), 0);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14);
        assertEquals("self." , strs[0]);
        assertEquals("c", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("[a,b].ap"), 8);
        assertEquals("list." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12);
        assertEquals("dict." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("''.ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("\"\".ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20);
        assertEquals("ClassA.someMethod." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2);
        assertEquals("" , strs[0]);
        assertEquals("fo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, false);
        assertEquals("" , strs[0]);
        assertEquals("fo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, true);
        assertEquals("" , strs[0]);
        assertEquals("foo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 2, true);
        assertEquals("" , strs[0]);
        assertEquals("foo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 5, true);
        assertEquals("foo.", strs[0]);
        assertEquals("bar", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 100, true); //out of the league
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        String importsTipperStr = PyCodeCompletion.getImportsTipperStr(new Document("from coilib.decorators import "), 30);
        assertEquals("coilib.decorators" , importsTipperStr);
        
        
    }

    /**
     * @throws BadLocationException
     * @throws CoreException
     * 
     */
    public void testFor() throws CoreException, BadLocationException {
        String s;
        s = "" +
    		"for event in a:   \n" +
    		"    print event   \n" +
    		"                  \n" +
			"event.";
        try {
            requestCompl(s, s.length(), -1, new String[] {});
        } catch (StackOverflowError e) {
            throw new RuntimeException(e);
        }
    }

}