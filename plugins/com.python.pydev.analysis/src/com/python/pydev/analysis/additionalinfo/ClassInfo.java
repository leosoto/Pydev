/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.ObjectsPool;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.NameTok;


public class ClassInfo extends AbstractInfo{

    private static final long serialVersionUID = 1L;

    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared, String path) {
        ClassInfo info = fromClassDef(def, moduleDeclared);
        info.path = path;
        return info;
        
    }
    
    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared) {
        ClassInfo info = new ClassInfo();
        info.name = ObjectsPool.intern(((NameTok)def.name).id);
        info.moduleDeclared = moduleDeclared;
        return info;
    }

    public int getType() {
        return CLASS_WITH_IMPORT_TYPE;
    }
}