/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitorTest  extends CodeCompletionTestsBase{

    public static void main(String[] args) {
        try{
            FindDefinitionModelVisitorTest test = new FindDefinitionModelVisitorTest();
            test.setUp();
            test.testFind6();
            test.tearDown();
            junit.textui.TestRunner.run(FindDefinitionModelVisitorTest.class);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        restorePythonPath(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws Exception
     * 
     */
    public void testFind() throws Exception {
        String d = ""+
        "from testAssist import assist\n" +
        "ex = assist.ExistingClass()\n" +
        "ex.newMethod(c,d)";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("ex", nature, new CompletionCache()), 3, 3, nature);
        
        assertEquals(1, defs.length);
        assertEquals("ex", ((AssignDefinition)defs[0]).target);
        assertEquals("assist.ExistingClass", defs[0].value);
        assertSame(module, defs[0].module);
        
        defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("assist.ExistingClass", nature, new CompletionCache()), 2, 6, nature);
        assertEquals(1, defs.length);
        assertEquals("ExistingClass", defs[0].value);
        assertNotSame(module, defs[0].module);
        assertEquals("testAssist.assist", defs[0].module.getName());
        
    }
    
    /**
     * @throws Exception
     * 
     */
    public void testFind4() throws Exception {
        String d = ""+
        "mydict = {}\n" +
        "mydict['key'] = 'value'";        
        
        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, -1);
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("mydict", nature, new CompletionCache()), 2, 2, nature);
        
        assertEquals(1, defs.length);
        assertEquals("mydict", ((AssignDefinition)defs[0]).target);
        assertEquals("dict", defs[0].value);
        assertSame(module, defs[0].module);
    }
    
    /**
     * @throws Exception
     * 
     */
    public void testFind3() throws Exception {
        String d = ""+
        "class Foo:\n" +
        "    def m1(self, bar):\n" +
        "        pass\n" +
        "    def m2(self):\n" +
        "        bar = 10\n" +
        "        self.m1(bar = bar)\n" + //the definition for first bar is in m1(self, bar) 
        "";
        
        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
        ICompletionState emptyCompletionState = CompletionStateFactory.getEmptyCompletionState("bar", nature, new CompletionCache());
        Definition[] defs = (Definition[]) module.findDefinition(emptyCompletionState, 6, 17, nature);
        
        assertEquals(1, defs.length);
        assertEquals(6, defs[0].line);
        assertEquals(17, defs[0].col);
        assertSame(module, defs[0].module);
    }
    
    
    /**
     * @throws Exception
     * 
     */
    public void testFind5() throws Exception {
        String d = ""+
        "class Foo:\n" +
        "    def m1(self, bar):\n" +
        "        pass\n" +
        "        xxx = \\\n" +
        "           yyy = 10\n" +
        "        print xxx, yyy\n" +
        "";
        
        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 1);
        ICompletionState emptyCompletionState = CompletionStateFactory.getEmptyCompletionState("xxx", nature, new CompletionCache());
        
        //look for xxx
        Definition[] defs = (Definition[]) module.findDefinition(emptyCompletionState, 6, 16, nature);
        
        assertEquals(1, defs.length);
        assertEquals(4, defs[0].line);
        assertEquals(9, defs[0].col);
        assertSame(module, defs[0].module);
        
        //look for yyy
        emptyCompletionState = CompletionStateFactory.getEmptyCompletionState("yyy", nature, new CompletionCache());
        defs = (Definition[]) module.findDefinition(emptyCompletionState, 6, 22, nature);
        
        assertEquals(1, defs.length);
        assertEquals(5, defs[0].line);
        assertEquals(12, defs[0].col);
        assertSame(module, defs[0].module);
    }
    
    

    /**
     * @throws Exception
     * 
     */
    public void testFind2() throws Exception {
        String d;
        d = "class C:            \n" +
            "    def met1(self): \n" +
            "        pass        \n" +
            "                    \n" +
            "class B:            \n" +
            "    def met2(self): \n" +
            "        self.c = C()\n" +
            "                    \n" +
            "    def met3(self): \n" +
            "        self.c.";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 9);
        //self.c is found as an assign
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("self.c",nature, new CompletionCache()), 10, 9, nature);
        
        assertEquals(1, defs.length);
        assertEquals("self.c", ((AssignDefinition)defs[0]).target);
        assertEquals("C", defs[0].value);
        assertSame(module, defs[0].module);
        
        defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("C", nature, new CompletionCache()), 7, 18, nature);
        assertEquals(1, defs.length);
        assertEquals("C", defs[0].value);
        assertSame(module, defs[0].module);
    }
    
    /**
     * @throws Exception
     * 
     */
    public void testFind6() throws Exception {
        String d;
        d = "class C:            \n" +
        "    def met1(self): \n" +
        "        pass        \n" +
        "                    \n" +
        "def TestIt(foo):\n" +
        "    pass\n";
        
        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, -1);
        //self.c is found as an assign
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("TestIt",nature, new CompletionCache()), -1, -1, nature);
        
        assertEquals(1, defs.length);
        assertEquals("TestIt", defs[0].value);
        assertEquals(5, defs[0].line);
        assertEquals(5, defs[0].col);
        
    }
}
