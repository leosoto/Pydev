/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.overridemethods.request;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class OverrideMethodsRequest implements IRefactoringRequest {

    public final FunctionDefAdapter method;
    public final int offsetStrategy;
    public final boolean generateMethodComments;

    private IClassDefAdapter classAdapter;
    private String baseClassName;
    private AdapterPrefs adapterPrefs;

    public OverrideMethodsRequest(
            IClassDefAdapter classAdapter, int offsetStrategy, FunctionDefAdapter method, 
            boolean generateMethodComments, String baseClassName, AdapterPrefs adapterPrefs) {
        this.baseClassName = baseClassName;
        this.classAdapter = classAdapter;
        this.offsetStrategy = offsetStrategy;
        this.method = method;
        this.generateMethodComments = generateMethodComments;
        this.adapterPrefs = adapterPrefs;
    }

    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return classAdapter;
    }

    public String getBaseClassName() {
        return getOffsetNode().getModule().getBaseContextName(this.classAdapter, baseClassName);
    }

    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }
}
